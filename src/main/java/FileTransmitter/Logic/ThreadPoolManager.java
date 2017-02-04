package FileTransmitter.Logic;

import FileTransmitter.Facade;
import FileTransmitter.Publisher.Interfaces.ISubscriber;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;
import FileTransmitter.Publisher.Publisher;
import FileTransmitter.Publisher.PublisherEvent;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

public final class ThreadPoolManager implements ISubscriber {

    private static int _threadsNumber;
    private static PoolWorker[] _threads;
    private static ConcurrentLinkedQueue<Runnable> _queue;

    private static ConcurrentLinkedQueue<Future> _futureTasksQueue;

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

        int workersThreadsNumber = 4;
        _threadsNumber = threadsNumber;
        _queue = new ConcurrentLinkedQueue<>();
//        _threads = new PoolWorker[workersThreadsNumber];
//        for (int i=0; i<workersThreadsNumber; i++) {
//            _threads[i] = new PoolWorker();
//            _threads[i].start();
//        }

        _futureTasksQueue = new ConcurrentLinkedQueue<>();

        _executorService = Executors.newWorkStealingPool(threadsNumber); //ForkJoinPool.commonPool(); //Executors.newFixedThreadPool(50);
        _scheduler = Executors.newScheduledThreadPool(1);
        //Executor executor = Executors.newFixedThreadPool(threadsNumber);
        _completionService = new ExecutorCompletionService<>(_executorService);

        _inited = true;
    }

    private void sendCompleteTask() {

//                Callable task = (Callable) publisherEvent.getBody();
//                executeFutureTask(task);


//        new Thread(() -> {
//
//            Callable task = (Callable) publisherEvent.getBody();
//            executeFutureTask(task);
//
//            while (true) {
//
//            Future future = getCompletionFutureTask();
//
//            PublisherEvent publisherEvent = new PublisherEvent(Facade.CMD_LOGGER_CLEAR_LOG, future).addServerCommand(Facade.CMD_SERVER_ADD_FUTURE_TASK);
//            sendEventToClient(publisherEvent);
//
////            try {
////                result = future.get();
////            } catch (InterruptedException ie) {
////                ie.printStackTrace();
////            } catch (ExecutionException ee) {
////                ee.printStackTrace();
////            }
//
////            Future<ArrayList> future = ThreadPoolManager.getInstance().getCompletionFutureTask();
////            List<String> result = new ArrayList<>();
////            try {
////                result = future.get();
////            } catch (InterruptedException ie) {
////                ie.printStackTrace();
////            } catch (ExecutionException ee) {
////                ee.printStackTrace();
////            }
////
////            addRecords(result);
//            }
//
//        });

    }


//    public void addRunnableTask(Runnable task) {
//        _executorService.execute(task);
//    }
//    public void shutdownRunnableTasks() {
//        _executorService.shutdownNow();
//    }

    public void execute(Runnable task) {
        synchronized(_queue) {
            _queue.add(task);
            _queue.notify();
        }
    }

    public void executeRunnable(Runnable runnable) {
        _executorService.execute(runnable);
    }

    public void executeFutureTask (Callable callable) {
        System.err.println("ADD TASK");
        _completionService.submit(callable);
    }

    public Future getCompletionFutureTask() {
        Future future = null;
        try {
            future = _completionService.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.err.println("COMPLETE TASK");
        return future;
    }

    public void executeCallable (Callable<String> callable) {
        Future future = _executorService.submit(callable);
        _futureTasksQueue.add(future);
//        ExecutorService ss = Executors.newCachedThreadPool();
//            ThreadFactory tf = Executors.defaultThreadFactory();
//            ThreadPoolExecutor ss =
//            ss.execute(r);
    }

    public Future getCallableFutureFromQueue() {
        System.err.println(getFutureTasksQueueSize());
        return _futureTasksQueue.poll();
    }

    public void sheduledTask(Runnable task, int interval) {

        //_scheduler.scheduleAtFixedRate(task, 0, interval, SECONDS);
        _scheduler.schedule(task, interval, SECONDS);

//        final Runnable beeper = new Runnable() {
//            public void run() { System.out.println("beep"); }
//        };
//        final ScheduledFuture<?> beeperHandle = _scheduler.scheduleAtFixedRate(beeper, 2, 2, SECONDS);
//        _scheduler.schedule(new Runnable() {
//            public void run() { beeperHandle.cancel(true); }
//        }, 10, SECONDS);
    }

    public int getFutureTasksQueueSize() {
        return _futureTasksQueue.size();
    }

    public void shutdown() {

    }



    private class PoolWorker extends Thread {
        //@Override
        public void run() {
            Runnable task;

            while (true) {
                synchronized(_queue) {
                    while (_queue.isEmpty()) {
                        try {
                            _queue.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    System.out.println(_queue.size());
                    task = (Runnable) _queue.poll();
                }

                try {
                    task.run();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_RECORD, message);
    }

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewSubscriber(this, Facade.EVENT_GROUP_EXECUTOR);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                Facade.CMD_EXECUTOR_PUT_TASK,
                Facade.CMD_EXECUTOR_TAKE_TASK,
                Facade.CMD_EXECUTOR_DEMO
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
        if (publisherEvent.getType().equals(Facade.EVENT_TYPE_GROUP)) {
            messageLog("Executor received group event ("
                    + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
        }

        switch (publisherEvent.getInterestName()) {
            case Facade.CMD_EXECUTOR_PUT_TASK: {

                PublisherEvent transitionEvent = new PublisherEvent(Facade.CMD_LOGGER_ADD_LOG, "THIS MESSAGE from REMOTE EXECUTOR");
                Publisher.getInstance().sendTransitionEvent(transitionEvent);
                break;
            }

            case Facade.CMD_EXECUTOR_TAKE_TASK: {
                messageLog("EXECUTOR_TAKE_TASK" + publisherEvent.getBody());
                break;
            }

            case Facade.CMD_EXECUTOR_DEMO: {
                messageLog("[EXECUTOR] command received on CMD_EXECUTOR_DEMO, body: "
                        + publisherEvent.getBody() + "\nEXECUTOR: sendTransitionEvent to "
                        + "remote LOGGER_ADD_LOG, contains THIS MESSAGE");
                //work......

                PublisherEvent transitionEvent = new PublisherEvent(Facade.CMD_LOGGER_ADD_LOG, "THIS MESSAGE from REMOTE EXECUTOR");
                Publisher.getInstance().sendTransitionEvent(transitionEvent, null, Facade.TRANSITION_EVENT_GROUP_ALL_USERS);
                break;
            }

        }

    }

}