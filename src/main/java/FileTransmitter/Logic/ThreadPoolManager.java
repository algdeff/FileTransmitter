package FileTransmitter.Logic;

import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

public final class ThreadPoolManager {

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
        _threadsNumber = threadsNumber;
        _queue = new ConcurrentLinkedQueue<>();
        _threads = new PoolWorker[threadsNumber];
//        for (int i=0; i<threadsNumber; i++) {
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

    public void execute(Runnable task) {
        synchronized(_queue) {
            _queue.add(task);
            _queue.notify();
        }
    }

    public void executeFutureTask (Callable callable) {
        _completionService.submit(callable);
    }

    public Future getCompletionFutureTask() {
        Future future = null;
        try {
            future = _completionService.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
}