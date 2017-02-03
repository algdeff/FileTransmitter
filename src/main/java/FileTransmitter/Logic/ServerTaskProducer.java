package FileTransmitter.Logic;

import FileTransmitter.Facade;
import FileTransmitter.Publisher.Interfaces.IListener;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;
import FileTransmitter.Publisher.Publisher;
import FileTransmitter.Publisher.PublisherEvent;
import FileTransmitter.ServerStarter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class ServerTaskProducer implements IListener {

    private static final String NAME = "TASK_PRODUCER_DE7F";

    private Path _logFilePath;
    private static BlockingQueue<List<String>> _completeTaskQueue;
    private static BlockingQueue<List<String>> _preparedTaskQueue;

    private static boolean _inited = false;


    public ServerTaskProducer() {
    }

    public void init() {
        if (_inited) return;

        _completeTaskQueue = new LinkedBlockingQueue<>();
        _preparedTaskQueue = new LinkedBlockingQueue<>();

        _inited = true;
        registerOnPublisher();
    }

    private void delayedStart() {
        messageLog("[ServerTaskProducer] START");

//        Publisher.getInstance().sendTransitionEvent(Facade.EVENT_GROUP_TASK_EXECUTOR, NAME);

        //PublisherEvent transitionEvent = new PublisherEvent(Facade.CMD_EXECUTOR_PUT_TASK, "From ClientTaskExecutor");
        //Publisher.getInstance().sendTransitionEvent(transitionEvent);


//        Callable task = (Callable) publisherEvent.getBody();
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



    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewListener(this, Facade.EVENT_GROUP_TASK_PRODUCER, NAME);
    }

    @Override
    public String[] listenerInterests() {
        return new String[] {
                Facade.CMD_TASK_PRODUCER_START,
                Facade.CMD_TASK_PRODUCER_GET_NEW_TASK,
                Facade.CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
//        System.out.println("Logger received event " + publisherEvent.getName()
//                + " / " + publisherEvent.getType() + ":\n" + publisherEvent.getBody().toString());
        if (publisherEvent.getType().equals(Facade.EVENT_TYPE_GROUP)) {
            messageLog("TASK_FACTORY - received group event ("
                    + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
        }

        switch (publisherEvent.getInterestName()) {
            case Facade.CMD_TASK_PRODUCER_START: {
                delayedStart();
                break;

            }
            case Facade.CMD_TASK_PRODUCER_GET_NEW_TASK: {
                messageLog("CMD_TASK_PRODUCER_GET_NEW_TASK" + publisherEvent.getBody().toString());
                break;

            }
            case Facade.CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK: {
                messageLog("CMD_TASK_PRODUCER_COLLECT_COMPLETE_TASK" + publisherEvent.getBody().toString());
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