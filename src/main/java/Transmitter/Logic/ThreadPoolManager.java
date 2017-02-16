package Transmitter.Logic;

import static Transmitter.Facade.*;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;
import Transmitter.Publisher.Publisher;
import Transmitter.Publisher.PublisherEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

public final class ThreadPoolManager implements ISubscriber {

    private static final int SCHEDULER_THREADS = 10;

    private static ExecutorService _executorService;
    private static CompletionService _completionService;
    private static ScheduledExecutorService _scheduler;

    private static boolean _inited = false;

    private static class SingletonInstance {
        private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();
    }

    private ThreadPoolManager() {
    }

    public static ThreadPoolManager getInstance() {
        return SingletonInstance.INSTANCE;
    }

    public void init(int threadsNumber) {
        if (_inited) {
            return;
        }

        registerOnPublisher();

        _executorService = Executors.newWorkStealingPool(threadsNumber); //ForkJoinPool.commonPool(); //Executors.newFixedThreadPool(50);
        _scheduler = Executors.newScheduledThreadPool(SCHEDULER_THREADS);
        //Executor executor = Executors.newFixedThreadPool(threadsNumber);
        _completionService = new ExecutorCompletionService<>(_executorService);

        _inited = true;
    }

    public void shutdown() {
    }

    public void executeRunnable(Runnable runnable) {
        _executorService.execute(runnable);
    }

    public void executeFutureTask (Callable callable) {
        _completionService.submit(callable);
    }
    public Future getCompletionFutureTask() {
        Future future = null;
        try {
            future = _completionService.take();
        } catch (InterruptedException e) {
            toLog(e.getMessage());
        }
        return future;
    }

    public ScheduledFuture<?> scheduledTask(Runnable task, int intervalSec) {
        return  _scheduler.scheduleAtFixedRate(task, intervalSec, intervalSec, SECONDS);
    }

    public ScheduledFuture<Object> scheduledCallable(Callable<Object> task, long intervalSec) {
        return  _scheduler.schedule(task, intervalSec, SECONDS);
    }

    public ScheduledFuture<Object> taskSchedulerService(LocalDateTime targetTime, Callable<Object> callableTask) {
        ZonedDateTime zonedTargetTime = targetTime.atZone(ZoneId.systemDefault());

        //time remaining, sec (for all time zones)
        long timeRemainingSec = zonedTargetTime.toEpochSecond() - ZonedDateTime.now().toEpochSecond();
        System.out.println(timeRemainingSec);

        //run expired tasks instantly
        return scheduledCallable(callableTask, timeRemainingSec > 0 ? timeRemainingSec : 0);
    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_RECORD, message);
    }

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewSubscriber(this, EVENT_GROUP_EXECUTOR);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                CMD_EXECUTOR_PUT_TASK,
                CMD_EXECUTOR_TAKE_TASK,
                CMD_EXECUTOR_DEMO
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
        if (publisherEvent.getType().equals(EVENT_TYPE_GROUP)) {
            messageLog("Executor received group event ("
                    + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
        }

        switch (publisherEvent.getInterestName()) {
            case CMD_EXECUTOR_PUT_TASK: {

                PublisherEvent transitionEvent = new PublisherEvent(CMD_LOGGER_ADD_LOG, "THIS MESSAGE from REMOTE EXECUTOR");
                Publisher.getInstance().sendTransitionEvent(transitionEvent);
                break;
            }

            case CMD_EXECUTOR_TAKE_TASK: {
                messageLog("EXECUTOR_TAKE_TASK" + publisherEvent.getBody());
                break;
            }

            case CMD_EXECUTOR_DEMO: {
                messageLog("[EXECUTOR] command received on CMD_EXECUTOR_DEMO, body: "
                        + publisherEvent.getBody() + "\nEXECUTOR: sendTransitionEvent to "
                        + "remote LOGGER_ADD_LOG, contains THIS MESSAGE");
                //work......

                PublisherEvent transitionEvent = new PublisherEvent(CMD_LOGGER_ADD_LOG, "THIS MESSAGE from REMOTE EXECUTOR");
                Publisher.getInstance().sendTransitionEvent(transitionEvent, null, TRANSITION_EVENT_GROUP_ALL_USERS);
                break;
            }

        }

    }

}