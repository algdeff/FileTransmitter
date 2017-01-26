package FileTransmitter.Logic;

import FileTransmitter.ServerStarter;
import FileTransmitter.Logic.Workers.FileProcessingThread;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class FileSystemMonitor {

    private Path _monitoringPath;
    private Path _sentPath;

    public FileSystemMonitor() {
    }

    public void start() {

        _monitoringPath = ConfigManager.getOutcomingPath();
        _sentPath = ConfigManager.getSentPath();

        prepareWorkFolders();

        Thread directoryWatcherThread = new Thread(new DirectoryWatcherThread());
        //directoryWatcherThread.setDaemon(true);
        directoryWatcherThread.start();

        Thread directoryWalkingThread = new Thread(new DirecroryWalkingThread());
        directoryWalkingThread.start();

    }

    private void prepareWorkFolders() {

        try {
            Files.createDirectories(_monitoringPath);
            Files.createDirectories(_sentPath);
        } catch (FileAlreadyExistsException faee) {
            System.err.println("Please rename this files: "
                    + _monitoringPath + " or " + _sentPath);
            ServerStarter.stopAndExit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private boolean isCorrectFile(Path pathname) {
        if (Files.isSymbolicLink(pathname)
                || !Files.isWritable(pathname)
                || Files.isDirectory(pathname)) return false;

        PathMatcher pathMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + ConfigManager
                        .getOutcomingTypesGlob());

        return pathMatcher.matches(pathname.getFileName());
    }


    private class DirecroryWalkingThread implements Runnable {

        @Override
        public void run() {
            try {
                directoryWalking();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void directoryWalking() throws Exception {

            try {
                //Path filename = Files.walkFileTree(pathName, new FindFileVisitor(SEARCH_GLOB));
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(_monitoringPath, ConfigManager
                        .getOutcomingTypesGlob());
                for (Path file : directoryStream) {
                    //if (Files.isDirectory(file)) continue;
                    if (!isCorrectFile(file)) continue;
                    ThreadPoolManager.getInstance().executeFutureTask(new FileProcessingThread(file));
                }

            } catch (IOException ioe) {
                System.err.println("directoryWalking: ioe");
            }

        }

    }


    private class DirectoryWatcherThread implements Runnable {

        @Override
        public void run() {
            try {
                startWatcher();
            } catch (Exception e) {
                e.printStackTrace();
                ServerStarter.stopAndExit(1);
            }
        }

        private void startWatcher() throws Exception {
            Path watchDirectory = _monitoringPath;
            WatchService watchService = null;

            try {
                watchService = watchDirectory.getFileSystem().newWatchService();
                watchDirectory.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            while (true) {
                WatchKey key = null;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (WatchEvent event : key.pollEvents()) {
                    if (event.context() == null) {
                        System.err.println("Some files in progress..");
                        continue;
                    }
                    fileProcessing(Paths.get(watchDirectory.toString() , event.context().toString()));
                }
                key.reset();
            }

        }

        private void fileProcessing(Path filePath) {
            if (!isCorrectFile(filePath)) return;
            ThreadPoolManager.getInstance().executeFutureTask(new FileProcessingThread(filePath));
        }

    }

}

