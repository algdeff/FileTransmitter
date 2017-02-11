package Transmitter.Logic.DistributedComputing;

import static Transmitter.Facade.*;
import Transmitter.Logic.ThreadPoolManager;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;
import Transmitter.Publisher.Publisher;
import Transmitter.Publisher.PublisherEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class ServerTaskProducer implements ISubscriber {

    private static final String NAME = "TASK_PRODUCER_DE7F";

    private static BlockingQueue<RemoteTaskEntity> _completeTaskQueue;
    private static BlockingQueue<RemoteTaskEntity> _preparedTaskQueue;

    private static ConcurrentMap<String, List<IRemoteTaskEntity>> _registeredClients;

    private static boolean _inited = false;


    public ServerTaskProducer() {
    }

    public void init() {
        if (_inited) return;

        _completeTaskQueue = new LinkedBlockingQueue<>(50);
        _preparedTaskQueue = new LinkedBlockingQueue<>(10);

        _registeredClients = new ConcurrentHashMap<>();

        _inited = true;
        registerOnPublisher();
    }

    private void delayedStart() {
        messageLog("[ServerTaskProducer] Preparing trasks....");

        startTasksGenerator();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Тotal prepared tasks: " + _preparedTaskQueue.size());

        messageLog("[ServerTaskProducer] Run tasks allocator");

        startTasksAllocator();


    }

    private void startTasksAllocator() {
        ThreadPoolManager.getInstance().executeRunnable(new TasksAllocator());
    }

    private void startTasksGenerator() {
        ThreadPoolManager.getInstance().executeRunnable(new TaskGenerator());
//        new Thread(new TaskGenerator()).start();
    }

    private void registerNewExecutor(String ClientID) {
        if (_registeredClients.containsKey(ClientID)) {
            messageLog("[ServerTaskProducer] client " + ClientID + " already registered");
//            rerun
            return;
        }
        messageLog("[ServerTaskProducer] Register new client: " + ClientID);
        _registeredClients.put(ClientID,  new CopyOnWriteArrayList<>());

    }

    private void collectCompletedTasks(RemoteTaskEntity completedTask) {
        messageLog("Collect completed task (" + completedTask.getTaskName()
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

        messageLog("Result: " + completedTask.getCompletedTaskResult().toString());

        try {
            _completeTaskQueue.put(completedTask);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        messageLog("Completed tasks number: " + _completeTaskQueue.size());
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
                CMD_SERVER_INTERNAL_CLIENT_SHUTDOWN
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
        if (publisherEvent.getType().equals(EVENT_TYPE_GROUP)) {
            messageLog("TASK_PRODUCER - received group event ("
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
                messageLog("CMD_TASK_PRODUCER_GET_NEW_TASK" + publisherEvent.getBody().toString());
                break;

            }
            case CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK: {
                collectCompletedTasks((RemoteTaskEntity) publisherEvent.getBody());
                break;
            }
            case CMD_SERVER_INTERNAL_CLIENT_SHUTDOWN: {
                restoreUncompletedTasks((String) publisherEvent.getBody());
                break;
            }

        }
    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_RECORD, message);
    }

    private void sendTasktoClient(String ClientID, IRemoteTaskEntity task) {
        Publisher.getInstance().sendTransitionEvent(new PublisherEvent(
                CMD_TASK_EXECUTOR_ADD_NEW_TASK, task), ClientID);
    }

    private void restoreUncompletedTasks(String Client_ID) {
        if (!_registeredClients.keySet().contains(Client_ID)) {
            messageLog("[ServerTaskProducer] Client (" + Client_ID + ") is not registered!");
            return;
        }

        List<IRemoteTaskEntity> clientTasks = new ArrayList<>(_registeredClients.get(Client_ID));
        _registeredClients.remove(Client_ID);

        for (IRemoteTaskEntity clientTask : clientTasks) {
            try {
                _preparedTaskQueue.put((RemoteTaskEntity) clientTask);
            } catch (InterruptedException e) {
                toLog("[ServerTaskProducer] restoreUncompletedTasks: InterruptedException");
            }
        }

        messageLog("[ServerTaskProducer] " + clientTasks.size()
                + " uncompleted tasks for client (" + Client_ID
                + ") restored. Client unregistered.");
    }

    private class TasksAllocator implements Runnable {

        @Override
        public void run() {

            RemoteTaskEntity newTask = null;

            while (true) {

                for (String client : _registeredClients.keySet()) {
                    List<IRemoteTaskEntity> clientTasks = _registeredClients.get(client);
                    if (clientTasks.size() == 0) {
                        try {
                            newTask = _preparedTaskQueue.take();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        newTask.setAssignedClientName(client);
                        clientTasks.add(newTask);
                        sendTasktoClient(client, newTask);
                        messageLog("Assign and send new task (" + newTask.getTaskName() + ") to client: " + client);
                    }
                }
//                System.out.println("TASKS QUEUE SIZE: " + _preparedTaskQueue.size());

                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }

        }

    }

    private class TaskGenerator implements Runnable {

        @Override
        public void run() {

            for (int i = 0; i < 100; i++) {

                List<Integer> parameters = new ArrayList<>();
                for (int k = i; k < i * 2; k++) {
                    parameters.add(k);
                }

                String taskName = "task" + i;

                ProducerTaskUnit taskUnit = new ProducerTaskUnit(parameters);
                RemoteTaskEntity newTask = new RemoteTaskEntity(taskUnit, taskName);

                try {
                    _preparedTaskQueue.put(newTask);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

//    private class CallableTaskUnit implements Callable<Object>, Serializable {
//
//        private List<Integer> _inputValues = new ArrayList<>();
//
//        public CallableTaskUnit(List<Integer> inputValues) {
//            _inputValues.addAll(inputValues);
//        }
//
//        @Override
//        public Object call() throws Exception {
//            return calculate();
//        }
//
//        private Object calculate() {
//            Integer result = 0;
//
//            for (Integer value : _inputValues) {
//                result += value;
//            }
//
//            return result;
//        }
//
//    }

}