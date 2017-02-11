package Transmitter.Logic;

import static Transmitter.Facade.*;

import Transmitter.Facade;
import Transmitter.Logic.Network.FileClient;
import Transmitter.Logic.Network.NetworkServer;
import Transmitter.Publisher.Publisher;
import Transmitter.ServerStarter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;

public class ModeSelector {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    public ModeSelector() {
        _receivedPath = ConfigManager.getReceivedPath();
        _outcomingPath = ConfigManager.getOutcomingPath();
        _sentPath = ConfigManager.getSentPath();
    }

    public void start() {


//        if (true) return;

        boolean isSelected = false;

        while (!isSelected) {
            messageLog(    "   You choice: \n" +
                           "1. Start CLIENT role\n" +
                           "2. Start SERVER role\n" +
                           "3. Terminate the program\n");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String choice = "";
            try {
                choice = stdin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            switch (choice) {
                case "1": {
                    isSelected = true;
                    startClientRole();
                    break;
                }
                case "2": {
                    isSelected = true;
                    startServerRole();
                    break;
                }
                case "3": {
                }

                ServerStarter.stopAndExit(0);
            }

        }

//        Thread directoryWatcherThread = new Thread(new DirectoryWatcherThread());
//        //directoryWatcherThread.setDaemon(true);
//        directoryWatcherThread.start();
//
//        Thread directoryWalkingThread = new Thread(new DirecroryWalkingThread());
//        directoryWalkingThread.start();

    }

    private void startClientRole() {
        messageLog("StartClientRole");

        Facade.setServerRole(false);
        prepareWorkFolders();

        FileClient fileClient = new FileClient(
                ConfigManager.getRemoteServerURL(),
                ConfigManager.getRemoteServerPort());
        fileClient.start();

    }

    private void startServerRole() {
        messageLog("StartServerRole");

        Facade.setServerRole(true);
        prepareWorkFolders();

        NetworkServer networkServer = new NetworkServer(
                ConfigManager.getRemoteServerPort());
        networkServer.start();

    }

    private void prepareWorkFolders() {

        try {
//            if (!_isServerRole) Files.createDirectories(_outcomingPath);
            Files.createDirectories(_outcomingPath);
            Files.createDirectories(_receivedPath);
            Files.createDirectories(_sentPath);
        } catch (FileAlreadyExistsException faee) {
            System.err.println("Please rename this files: "
                    + _receivedPath + ", " + _outcomingPath
                    + " or " + _sentPath);
            ServerStarter.stopAndExit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }



//    private class DirecroryWalkingThread implements Runnable {
//
//        @Override
//        public void run() {
//            try {
//                directoryWalking();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
//
//        private void directoryWalking() throws Exception {
//
//            try {
//                //Path filename = Files.walkFileTree(pathName, new FindFileVisitor(SEARCH_GLOB));
//                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(_outcomingPath, ConfigManager
//                        .getOutcomingTypesGlob());
//                for (Path file : directoryStream) {
//                    //if (Files.isDirectory(file)) continue;
//                    if (!isCorrectFile(file)) continue;
//                    ThreadPoolManager.getInstance().executeFutureTask(new FileProcessingThread(file));
//                }
//
//            } catch (IOException ioe) {
//                System.err.println("directoryWalking: ioe");
//            }
//
//        }
//
//    }
//
//
//    private class DirectoryWatcherThread implements Runnable {
//
//        @Override
//        public void run() {
//            try {
//                startWatcher();
//            } catch (Exception e) {
//                e.printStackTrace();
//                ServerStarter.stopAndExit(1);
//            }
//        }
//
//        private void startWatcher() throws Exception {
//            Path watchDirectory = _outcomingPath;
//            WatchService watchService = null;
//
//            try {
//                watchService = watchDirectory.getFileSystem().newWatchService();
//                watchDirectory.register(watchService,
//                        StandardWatchEventKinds.ENTRY_CREATE);
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//
//            while (true) {
//                WatchKey key = null;
//                try {
//                    key = watchService.take();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                for (WatchEvent event : key.pollEvents()) {
//                    if (event.context() == null) {
//                        System.err.println("Some files in progress..");
//                        continue;
//                    }
//                    fileProcessing(Paths.get(watchDirectory.toString() , event.context().toString()));
//                }
//                key.reset();
//            }
//
//        }
//
//        private void fileProcessing(Path filePath) {
//            if (!isCorrectFile(filePath)) return;
//            ThreadPoolManager.getInstance().executeFutureTask(new FileProcessingThread(filePath));
//        }
//
//    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_LOG, message);
    }

}

