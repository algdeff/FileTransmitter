package Transmitter.Logic.DistributedComputing;

import static Transmitter.Facade.*;
import Transmitter.Logic.ThreadPoolManager;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;
import Transmitter.Publisher.Publisher;
import Transmitter.Publisher.PublisherEvent;
import Transmitter.ServerStarter;

import java.time.LocalDateTime;

import java.util.List;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * ClientTaskExecutor
 *
 * При соединении с сервером отправляется запрос о регистрации, после чего сервер присылает список задач
 * List<IRemoteTaskEntity>, которые могут быть двух видов:
 * TASK_TYPE_INSTANT - задачи выполняемые немедленно;
 * TASK_TYPE_SCHEDULED - отложенные задачи, в которых через setTargetTime() задается время запуска.
 *
 * Каждая выполненная задача отсылается на сервер, где сверяется со списком задач, назначенных данному
 * клиенту. Если выполнятся все задачи, то сервер пришлет новую порцию задач, если не выполнятся, в случае
 * обрыва соединения или закрытия клиента, сервер снимет не выполненную часть назначенных задач и вернет их
 * в пул задач, ожидающих назначения.
 *
 *
 * @author  Anton Butenko
 *
 */

public class ClientTaskExecutor implements ISubscriber {

    private static BlockingQueue<IRemoteTaskEntity> _inProcessingTasks;
    private static BlockingQueue<ScheduledTaskEntity> _scheduledTasks;

    private volatile boolean _sessionActive = true;

    private static boolean _inited = false;

    private String _clientID = null;


    public ClientTaskExecutor() {
    }

    public void init() {
        if (_inited) return;

        _inProcessingTasks = new LinkedBlockingQueue<>();
        _scheduledTasks = new DelayQueue<>();

        _inited = true;
        registerOnPublisher();
    }

    private void shutdown() {
        _sessionActive = false;
        _scheduledTasks.clear();

    }

    private void delayedStart(String client_ID) {
        messageLog("[ClientTaskExecutor START]");

        //выключаем меню выбора
        Publisher.getInstance().sendPublisherEvent(CMD_NET_CLIENT_UI_BREAK);

        //ожидают поступление новых задач в очередь и выполняют их
        startTaskProcessingMonitor();
        startScheduledTasksQueueMonitor();

        //регистрируем данного клиента нв сервере (TaskProducer)
        Publisher.getInstance().sendTransitionEvent(new PublisherEvent(
                CMD_TASK_PRODUCER_REGISTER_EXECUTOR, client_ID));

    }

    private void addIncomingTasks(List<IRemoteTaskEntity> incomingTasks) {

        //сообщение о поступивших задачах и времени выполнения
        messageLog(incomingTasks.size() + " tasks accepted from server:");
        for (IRemoteTaskEntity incomingTask : incomingTasks) {
            if (incomingTask.getTaskType().equals(IRemoteTaskEntity.TASK_TYPE_SCHEDULED)) {
                LocalDateTime targetTime = ((ScheduledTaskEntity) incomingTask).getTargetTime();
                long timeRemainSec = ((ScheduledTaskEntity) incomingTask).getTimeRemainSec();

                messageLog("Scheduled task (" + incomingTask.getTaskName() + ") | "
                        + targetTime + ", time remain, sec: " + timeRemainSec);
            } else {
                messageLog("Instant task (" + incomingTask.getTaskName() + ")");
            }
        }

        //парсим задачи по типу и добавляем в очередь на выполнение
        for (IRemoteTaskEntity incomingTask : incomingTasks) {

            switch (incomingTask.getTaskType()) {
                //очередь на немедленное выполнение
                case IRemoteTaskEntity.TASK_TYPE_INSTANT: {
                    _inProcessingTasks.add(incomingTask);
                    break;
                }
                //очередь задач отложенного выполнения
                case IRemoteTaskEntity.TASK_TYPE_SCHEDULED: {
                    _scheduledTasks.offer((ScheduledTaskEntity) incomingTask);
                    break;
                }
            }
        }

    }

    private void startScheduledTasksQueueMonitor() {

        Thread scheduledTasksMonitor = new Thread(() -> {
            try {
                while (_sessionActive) {
                    ScheduledTaskEntity scheduledTask = _scheduledTasks.take();
                    _inProcessingTasks.put(scheduledTask);
                }
            } catch (InterruptedException e) {
                messageLog("ScheduledTasksQueueMonitor interrupted");
                toLog(e.getMessage());
                ServerStarter.stopAndExit(1);
            }

        }, "ScheduledTasksQueueMonitor");
        scheduledTasksMonitor.start();

    }

    private void startTaskProcessingMonitor() {

        Thread taskProcessingMonitor = new Thread(() -> {
            try {
                while (_sessionActive) {
                    IRemoteTaskEntity processingTask = _inProcessingTasks.take();
                    taskProcessing(processingTask);
                }
            } catch (InterruptedException e) {
                messageLog("TaskProcessingMonitor interrupted");
                toLog(e.getMessage());
                ServerStarter.stopAndExit(1);
            }

        }, "TaskProcessingMonitor");
        taskProcessingMonitor.start();

    }

    private void taskProcessing(IRemoteTaskEntity taskEntity) {
        messageLog("Processing task from queue (" + taskEntity.getTaskName() + ") ...");

        //вычисление задачи
        Callable<Object> taskUnit = taskEntity.getTaskUnit();

        FutureTask<Object> futureTask = new FutureTask<>(taskUnit);
        ThreadPoolManager.getInstance().executeRunnable(futureTask);

        //ожидание результата
        Object completeTaskResult = null;
        try {
            completeTaskResult = futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            messageLog("taskProcessing interrupted");
            toLog(e.getMessage());
        }

        //отправление результата вычислений на сервер
        taskEntity.setCompletedTaskResult(completeTaskResult);

        messageLog("COMPLETE - send result to server");
        Publisher.getInstance().sendTransitionEvent(new PublisherEvent(
                CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK, taskEntity));

    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_RECORD, message);
    }

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewSubscriber(this, EVENT_GROUP_TASK_EXECUTOR);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                CMD_TASK_EXECUTOR_START,
                CMD_TASK_EXECUTOR_ADD_NEW_TASKS
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
        if (publisherEvent.getType().equals(EVENT_TYPE_GROUP)) {
            messageLog("TASK_EXECUTOR - received group event ("
                    + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
            return;
        }

        switch (publisherEvent.getInterestName()) {
            case CMD_TASK_EXECUTOR_START: {
                delayedStart((String) publisherEvent.getBody());
                break;

            }
            case CMD_TASK_EXECUTOR_ADD_NEW_TASKS: {
                addIncomingTasks((List<IRemoteTaskEntity>) publisherEvent.getBody());
                break;

            }
            case GLOBAL_SHUTDOWN: {
                shutdown();
                break;

            }

        }
    }

}