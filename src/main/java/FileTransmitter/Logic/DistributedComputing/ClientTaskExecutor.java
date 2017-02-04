package FileTransmitter.Logic.DistributedComputing;

import FileTransmitter.Facade;
import FileTransmitter.Logic.DistributedComputing.RemoteTaskEntity;
import FileTransmitter.Logic.ThreadPoolManager;
import FileTransmitter.Publisher.Interfaces.ISubscriber;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;
import FileTransmitter.Publisher.Publisher;
import FileTransmitter.Publisher.PublisherEvent;

import java.util.concurrent.*;


public class ClientTaskExecutor implements ISubscriber {

    private static boolean _inited = false;

    private String _clientID = null;


    public ClientTaskExecutor() {
    }

    public void init() {
        if (_inited) return;

        _inited = true;
        registerOnPublisher();
    }

    private void delayedStart(String client_ID) {
        messageLog("[ClientTaskExecutor START]");
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_NET_CLIENT_UI_BREAK);

        //register this client (CID) on TaskProducer
        Publisher.getInstance().sendTransitionEvent(new PublisherEvent(
                Facade.CMD_TASK_PRODUCER_REGISTER_EXECUTOR, client_ID));

    }

    private void processedNewTask(RemoteTaskEntity remoteTaskEntity) {
        messageLog("Processed new task (" + remoteTaskEntity.getTaskName() + ") ...");

        Callable<Object> taskUnit = remoteTaskEntity.getTaskUnit();

        FutureTask<Object> futureTask = new FutureTask<>(taskUnit);
        ThreadPoolManager.getInstance().executeRunnable(futureTask);

        Object completeTaskResult = null;
        try {
            completeTaskResult = futureTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        remoteTaskEntity.setCompletedTaskResult(completeTaskResult);

        messageLog("COMPLETE - send result to server");
        Publisher.getInstance().sendTransitionEvent(new PublisherEvent(
                Facade.CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK, remoteTaskEntity));

    }


    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewSubscriber(this, Facade.EVENT_GROUP_TASK_EXECUTOR);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                Facade.CMD_TASK_EXECUTOR_START,
                Facade.CMD_TASK_EXECUTOR_ADD_NEW_TASK
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
        if (publisherEvent.getType().equals(Facade.EVENT_TYPE_GROUP)) {
            messageLog("TASK_EXECUTOR - received group event ("
                    + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
            return;
        }

        switch (publisherEvent.getInterestName()) {
            case Facade.CMD_TASK_EXECUTOR_START: {
                delayedStart((String) publisherEvent.getBody());
                break;

            }
            case Facade.CMD_TASK_EXECUTOR_ADD_NEW_TASK: {
                processedNewTask((RemoteTaskEntity) publisherEvent.getBody());
                break;

            }

        }
    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_RECORD, message);
    }

}