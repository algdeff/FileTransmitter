package Transmitter.Logic.DistributedComputing;

import static Transmitter.Facade.*;
import Transmitter.Logic.ThreadPoolManager;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;
import Transmitter.Publisher.Publisher;
import Transmitter.Publisher.PublisherEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * ServerTaskProducer
 *
 * TaskGenerator, при старте генерирует список задач для выполнения группой удаленных клиентов и
 * помещает их в очередь _preparedTaskQueue.
 *
 * TasksAllocator, берет задачи из очереди _preparedTaskQueue и назначает их зарегистрированным
 * клиентам (@key в _registeredClients) порциями, по ASSIGNED_TASKS_LIST_SIZE задач на каждого клиента.
 * Периодически проверяется сколько осталось невыполненных задач у каждого клиента, если все задачи
 * выполнены и размер List<IRemoteTaskEntity> в _registeredClients равен 0, то назначаются новые
 * задачи.
 *
 *
 * @author  Anton Butenko
 *
 */

public class ServerTaskProducer implements ISubscriber {

    private static final String NAME = "TASK_PRODUCER_DE7F";

    //число задачь отправляемое клиенту за раз
    private static final int ASSIGNED_TASKS_LIST_SIZE = 5;

    //интервал проверки выполненных задач для TasksAllocator, сек.
    private static final int TASK_ALLOCATOR_CHECK_INTERVAL = 5;

    private static BlockingQueue<IRemoteTaskEntity> _completeTaskQueue;
    private static BlockingQueue<IRemoteTaskEntity> _preparedTaskQueue;

    /**
     *  @key    - имя зарегистрированного клиента (clientID);
     *  @value  - список задач назначенных данному клиенту.
     */
    private static ConcurrentMap<String, List<IRemoteTaskEntity>> _registeredClients;

    private volatile boolean _sessionActive = true;

    private static boolean _inited = false;


    public ServerTaskProducer() {
    }

    public void init() {
        if (_inited) return;

        _completeTaskQueue = new LinkedBlockingQueue<>();
        _preparedTaskQueue = new LinkedBlockingQueue<>(); //DelayQueue<>() for server schedule;

        _registeredClients = new ConcurrentHashMap<>();

        _inited = true;
        registerOnPublisher();
    }

    private void delayedStart() {
        messageAndLog("[ServerTaskProducer] Preparing trasks....");

        startTasksGenerator();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            toLog(e.getMessage());
        }
        System.out.println("Тotal prepared tasks: " + _preparedTaskQueue.size());

        messageAndLog("[ServerTaskProducer] Run tasks allocator");

        startTasksAllocator();

    }

    private void shutdown() {
        _sessionActive = false;
    }

    private void startTasksAllocator() {
        ThreadPoolManager.getInstance().executeRunnable(new TasksAllocator());
    }

    private void startTasksGenerator() {
        ThreadPoolManager.getInstance().executeRunnable(new TaskGenerator());
    }

    /**
     *  Регистрируем нового клиента в _registeredClients, с пустым списком задач для назначения TasksAllocator'ом
     */
    private void registerNewExecutor(String clientID) {
        if (_registeredClients.containsKey(clientID)) {
            messageAndLog("[ServerTaskProducer] client " + clientID + " already registered");
            return;
        }
        messageAndLog("[ServerTaskProducer] Register new client: " + clientID);
        _registeredClients.put(clientID,  new CopyOnWriteArrayList<>());

    }

    /**
     *  Получаем от каждого клиента выполненную задачу, удаляем ее из списка назначенных клиенту задач,
     *  и помещаем ее в очередь выполненных задач _completeTaskQueue.
     */
    private void collectCompletedTask(IRemoteTaskEntity completedTask) {
        messageAndLog("Collect completed task (" + completedTask.getTaskName()
                + ") from client " + completedTask.getAssignedClientName());

        String client = completedTask.getAssignedClientName();
        String completedTaskName = completedTask.getTaskName();

        List<IRemoteTaskEntity> clientTasks = _registeredClients.get(client);
        for (IRemoteTaskEntity clientTask : clientTasks) {
            if (clientTask.getTaskName().equals(completedTaskName)
                    && clientTask.getAssignedClientName().equals(client)) {
                clientTasks.remove(clientTask);
            }
        }

        messageAndLog("Remain " + clientTasks.size() +  " tasks, result:" + completedTask.getCompletedTaskResult().toString());

        try {
            _completeTaskQueue.put(completedTask);
        } catch (InterruptedException e) {
            messageAndLog("collectCompletedTask interrupted");
            toLog(e.getMessage());
        }
        messageAndLog("Completed tasks number: " + _completeTaskQueue.size());
    }

    /**
     *  Снятие отключенного клиента с регистрации и возвращение невыполненных задач в пул _preparedTaskQueue
     */
    private void restoreUncompletedTasks(String client_ID) {
        if (!_registeredClients.keySet().contains(client_ID)) {
            messageAndLog("[ServerTaskProducer] Client (" + client_ID + ") is not registered!");
            return;
        }

        List<IRemoteTaskEntity> clientTasks = new ArrayList<>(_registeredClients.get(client_ID));
        _registeredClients.remove(client_ID);

        _preparedTaskQueue.addAll(clientTasks);

        messageAndLog("[ServerTaskProducer] " + clientTasks.size()
                + " uncompleted client (" + client_ID
                + ") tasks restored. Client unregistered.");
    }

    private void sendTasksToClient(List<IRemoteTaskEntity> tasks, String clientID) {
        Publisher.getInstance().sendTransitionEvent(new PublisherEvent(
                CMD_TASK_EXECUTOR_ADD_NEW_TASKS, new ArrayList<>(tasks)), clientID);
        // !обязательно создаем новый List, иначе при сериализации получим старые ссылки!

    }

    private void messageAndLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_RECORD, message);
    }

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewSubscriber(this, EVENT_GROUP_TASK_PRODUCER, NAME);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                CMD_TASK_PRODUCER_START,
                CMD_TASK_PRODUCER_REGISTER_EXECUTOR,
                CMD_TASK_PRODUCER_GET_NEW_TASK,
                CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK,
                CMD_INTERNAL_CLIENT_BREAKDOWN,
                GLOBAL_SHUTDOWN
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
        if (publisherEvent.getType().equals(EVENT_TYPE_GROUP)) {
            messageAndLog("TASK_PRODUCER - received group event ("
                    + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
        }

        switch (publisherEvent.getInterestName()) {
            case CMD_TASK_PRODUCER_START: {
                delayedStart();
                break;

            }
            case CMD_TASK_PRODUCER_REGISTER_EXECUTOR: {
                registerNewExecutor((String) publisherEvent.getBody());
                break;

            }
            case CMD_TASK_PRODUCER_GET_NEW_TASK: {
                messageAndLog("CMD_TASK_PRODUCER_GET_NEW_TASK" + publisherEvent.getBody().toString());
                break;

            }
            case CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK: {
                collectCompletedTask((IRemoteTaskEntity) publisherEvent.getBody());
                break;
            }
            case CMD_INTERNAL_CLIENT_BREAKDOWN: {
                restoreUncompletedTasks((String) publisherEvent.getBody());
                break;
            }
            case GLOBAL_SHUTDOWN: {
                shutdown();
                break;
            }

        }
    }

    private class TasksAllocator implements Runnable {

        @Override
        public void run() {

            try {
                while (_sessionActive) {

                    for (String client : _registeredClients.keySet()) {
                        List<IRemoteTaskEntity> clientTasks = _registeredClients.get(client);
                        if (clientTasks.size() == 0) {

                            for (int i = 0; i < ASSIGNED_TASKS_LIST_SIZE; i++) {
                                IRemoteTaskEntity newTask = _preparedTaskQueue.take();
                                newTask.setAssignedClientName(client);
                                clientTasks.add(newTask);
                                messageAndLog("New task (" + newTask.getTaskName() + ") assigned to client: " + client);
                            }

                            messageAndLog("Send " + clientTasks.size() + " tasks to client: " + client);
                            sendTasksToClient(clientTasks, client);
                        }
                    }

                    //интервал повторной проверки выполнения и назначения новых задач
                    TimeUnit.SECONDS.sleep(TASK_ALLOCATOR_CHECK_INTERVAL);

                }

            } catch (InterruptedException e) {
                messageAndLog("TasksAllocator interrupted");
                toLog(e.getMessage());
            }

        }

    }

    private class TaskGenerator implements Runnable {

        @Override
        public void run() {

            try {
                //создание списка задач (пример)
                for (int i = 0; i < 100; i++) {

                    List<Integer> parameters = new ArrayList<>();
                    for (int k = i; k < i * 2; k++) {
                        parameters.add(k);
                    }

                    String taskName = "task" + i;

                    ProducerTaskUnit taskUnit = new ProducerTaskUnit(parameters);

                    int addTimeSec = i + new Random().nextInt(200);
                    addTimeSec ^= 2;

                    System.out.println(taskName + " / " + addTimeSec);

                    //создание обычных задач
//                    RemoteTaskEntity newTask = new RemoteTaskEntity(taskUnit, taskName);

                    //создание отложенных задач, на определенный DateTime (независимый от часовых поясов)
                    ScheduledTaskEntity newScheduledTask = new ScheduledTaskEntity(taskUnit, taskName);
                    //dateTime to start scheduled task on remote client
                    LocalDateTime targetTime = LocalDateTime.of(LocalDate.now(),
                            LocalTime.now().plusSeconds(addTimeSec));
                    newScheduledTask.setTargetTime(targetTime);

                    //добавление в очередь
                    _preparedTaskQueue.put(newScheduledTask);
                }
            } catch (InterruptedException e) {
                messageAndLog("TaskGenerator interrupted");
                toLog(e.getMessage());
            }

        }

    }


}