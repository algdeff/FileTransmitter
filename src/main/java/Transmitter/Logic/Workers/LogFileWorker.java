package Transmitter.Logic.Workers;

import Transmitter.Facade;
import Transmitter.Logic.ConfigManager;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;
import Transmitter.Publisher.Publisher;
import Transmitter.ServerStarter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class LogFileWorker implements ISubscriber {

    private Path _logFilePath;
    private static BlockingQueue<List<String>> _queue;

    private static boolean _inited = false;

    private static class SingletonInstance {
        private static final LogFileWorker INSTANCE = new LogFileWorker();
    }

    private LogFileWorker() {
    }

    public static LogFileWorker getInstance() {
        return SingletonInstance.INSTANCE;
    }

    public void init() {
        if (_inited) return;

        _queue = new LinkedBlockingQueue<>();

        _logFilePath = ConfigManager.getLogFilePath();
        _inited = true;
        registerOnPublisher();
        startQueueMonitor();
    }

    private void addLog(String message) {
        addRecord(getDateTimeNow() + " - " + message);
    }

    private void addRecord(String record) {
        List<String> records = new ArrayList<>();
        records.add(record);
        addRecord(records);
    }

    private void addRecord(List<String> records) {
        try {
            _queue.put(records);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startQueueMonitor() {

        Thread fileWorkerThread = new Thread(() -> {
            while (true) {

                List<String> result = new ArrayList<>();
                try {
//                    System.out.println("QUEUE1: take" + result.toString());
                    result = _queue.take();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                saveRecord(result);
//                System.out.println("QUEUE2: take" + result.toString());
            }
        });
        fileWorkerThread.start();

    }

    private void saveRecord(List<String> records) {

        try {
            Files.write(_logFilePath, records, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            ServerStarter.stopAndExit(1);
        }

    }

    private String getDateTimeNow() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewSubscriber(this, Facade.EVENT_GROUP_LOGGER);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                Facade.CMD_LOGGER_ADD_LOG,
                Facade.CMD_LOGGER_ADD_RECORD,
                Facade.CMD_LOGGER_CLEAR_LOG
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
//        System.out.println("Logger received event " + publisherEvent.getName()
//                + " / " + publisherEvent.getType() + ":\n" + publisherEvent.getBody().toString());
        if (publisherEvent.getType().equals(Facade.EVENT_TYPE_GROUP)) {
            addLog(publisherEvent.getBody().toString());
            System.err.println(getDateTimeNow() + " - Logger received group event ("
                    + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
        }

        switch (publisherEvent.getInterestName()) {
            case Facade.CMD_LOGGER_ADD_LOG: {
                addLog(publisherEvent.getBody().toString());
                System.err.println(publisherEvent.getBody().toString());
                break;

            }
            case Facade.CMD_LOGGER_ADD_RECORD: {
                addRecord(publisherEvent.getBody().toString());
//                System.out.println("Logger event ("
//                        + publisherEvent.getName() + "): \n" + publisherEvent.getBody().toString());
                break;

            }
            case Facade.CMD_LOGGER_CLEAR_LOG: {
                System.out.println("Logger event ("
                        + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
                break;
            }
        }
    }

}