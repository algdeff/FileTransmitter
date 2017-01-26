package FileTransmitter.Logic.Workers;

import FileTransmitter.Logic.ConfigManager;
import FileTransmitter.Logic.ThreadPoolManager;
import FileTransmitter.ServerStarter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LogFileWorker {

    private Path _logFilePath;

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
        startLoggingRecords();
    }

    private void startLoggingRecords() {
        //ArrayList<String> result = new ArrayList<>();

        while (true) {

            Future<ArrayList> future = ThreadPoolManager.getInstance().getCompletionFutureTask();
            List<String> result = new ArrayList<>();
            try {
                result = future.get();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (ExecutionException ee) {
                ee.printStackTrace();
            }

            addRecords(result);
        }

    }

    private void addRecord(String record) {
        List<String> records = new ArrayList<>();
        records.add(record);
        addRecords(records);
    }

    private void addRecords(List<String> records) {

        try {
            Files.write(_logFilePath, records, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            ServerStarter.stopAndExit(1);
        }

    }

}

