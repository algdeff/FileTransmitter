package Transmitter.Logic.Workers;

import static Transmitter.Facade.*;

import Transmitter.Logic.ConfigManager;
import Transmitter.Logic.ThreadPoolManager;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;
import Transmitter.Publisher.Publisher;
import Transmitter.ServerStarter;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

public class LogFileWorker implements ISubscriber {

    private static final int MIN_SCHEDULER_INTERVAL = 1;

    private Path _logFilePath;
    private static BlockingQueue<List<String>> _recordsQueue;
    private static Map<String, FileStatisticsContext> _filesStatistics;

    private ScheduledFuture _scheduledTaskFuture;

    private boolean _isActive = true;

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

        _logFilePath = ConfigManager.getLogFilePath();
        _inited = true;

        registerOnPublisher();

//        if (!Facade.isServerRole()) return;

        _recordsQueue = new LinkedBlockingQueue<>();
        _filesStatistics = new ConcurrentHashMap<>();
        int statsWriteInterval = ConfigManager.getStatsWriteInterval();

        startQueueMonitor();

        startStatisticsScheduler(statsWriteInterval > MIN_SCHEDULER_INTERVAL
                ? statsWriteInterval : MIN_SCHEDULER_INTERVAL);

    }

    private void shutdown() {
        _isActive = false;
        if (_scheduledTaskFuture != null) _scheduledTaskFuture.cancel(true);
    }

    private void addFileToStatistics(String filename) {
        if (!_filesStatistics.containsKey(filename)) {
            _filesStatistics.put(filename, new FileStatisticsContext(filename));
        }
        FileStatisticsContext currentFileContext = _filesStatistics.get(filename);
        currentFileContext.incrementNumberOfDownloads();
//        System.out.println(currentFileContext.getNumberOfDownloads());
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
            _recordsQueue.put(records);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startStatisticsScheduler(int statWriteIntervalSec) {
        _scheduledTaskFuture = ThreadPoolManager.getInstance().scheduledTask(() -> {
            for (FileStatisticsContext fileContext : _filesStatistics.values()) {
                if (fileContext.isProcessed()) continue;
                addLog(fileContext.getFileName() + " downloads "
                        + fileContext.getNumberOfDownloads() + " times");
                fileContext.processedChanges();
            }

        }, statWriteIntervalSec);

    }

    private void startQueueMonitor() {

        Thread fileWorkerThread = new Thread(() -> {
            try {
                while (_isActive) {
                    List<String> result = _recordsQueue.take();
                    saveRecord(result);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, "LogQueueMonitor");
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
        Publisher.getInstance().registerNewSubscriber(this, EVENT_GROUP_LOGGER);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                CMD_LOGGER_ADD_LOG,
                CMD_LOGGER_ADD_RECORD,
                CMD_LOGGER_CONSOLE_MESSAGE,
                CMD_LOGGER_ADD_FILE_TO_STATISTICS,
                CMD_LOGGER_CLEAR_LOG,
                GLOBAL_SHUTDOWN
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
//        System.out.println("Logger received event " + publisherEvent.getName()
//                + " / " + publisherEvent.getType() + ":\n" + publisherEvent.getBody().toString());
        if (publisherEvent.getType().equals(EVENT_TYPE_GROUP)) {
            addLog(publisherEvent.getBody().toString());
            System.err.println(getDateTimeNow() + " - Logger received group event ("
                    + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
        }

        switch (publisherEvent.getInterestName()) {
            case CMD_LOGGER_ADD_LOG: {
                addLog(publisherEvent.getBody().toString());
                System.err.println(publisherEvent.getBody().toString());
                break;

            }
            case CMD_LOGGER_ADD_RECORD: {
                addLog(publisherEvent.getBody().toString());
                break;

            }
            case CMD_LOGGER_CONSOLE_MESSAGE: {
                System.err.println(publisherEvent.getBody().toString());
                break;

            }
            case CMD_LOGGER_ADD_FILE_TO_STATISTICS: {
                addFileToStatistics(publisherEvent.getBody().toString());
                break;
            }
            case CMD_LOGGER_CLEAR_LOG: {
                System.out.println("Logger event ("
                        + publisherEvent.getInterestName() + "): \n" + publisherEvent.getBody().toString());
                break;
            }
            case GLOBAL_SHUTDOWN: {
                shutdown();
                break;
            }

        }
    }

}