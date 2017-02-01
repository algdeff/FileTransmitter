package FileTransmitter.Logic.Network;

import FileTransmitter.Facade;
import FileTransmitter.Logic.ConfigManager;
import FileTransmitter.Publisher.Interfaces.IListener;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;
import FileTransmitter.Publisher.Publisher;
import FileTransmitter.Publisher.PublisherEvent;
import FileTransmitter.ServerStarter;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FileClient {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    private String _remoteServerUrl;
    private int _remoteServerPort;

    private BlockingQueue<PublisherEvent> _outcomeClientEventsQueue;

    private AsynchronousSocketChannel _clientSocketChannel;
    private ObjectOutputStream _objectOutputStream;
    private ObjectInputStream _objectInputStream;

    private List<String> _serverFileListCache;
    private List<String> _clientFileListCache;

    public FileClient(String remoteServerUrl, int remoteServerPort) {
        _remoteServerUrl = remoteServerUrl;
        _remoteServerPort = remoteServerPort;
        _receivedPath = ConfigManager.getReceivedPath();
        _outcomingPath = ConfigManager.getOutcomingPath();
        _sentPath = ConfigManager.getSentPath();
        _outcomeClientEventsQueue = new LinkedBlockingQueue<>(50);
        _serverFileListCache = new ArrayList<>();
        _clientFileListCache = new ArrayList<>();
        _clientSocketChannel = null;
    }

    public void start() {
        openConnectionToServer();

    }

    private void openConnectionToServer() {
        int threadsNumber = 10;
        ExecutorService executorService = Executors.newWorkStealingPool(threadsNumber); //ForkJoinPool.commonPool(); //Executors.newFixedThreadPool(50);

        try {
//            InetSocketAddress  hostAddress = new InetSocketAddress(InetAddress.getByName(_remoteServerUrl),_remoteServerPort);
            SocketAddress hostAddress = new InetSocketAddress(InetAddress.getByName(_remoteServerUrl),_remoteServerPort);
//            AsynchronousSocketChannel clientSocketChannel = AsynchronousSocketChannel.open(); //AsynchronousChannelGroup.withThreadPool(executorService));

            boolean isConnected = false;
            while (!isConnected) {
                _clientSocketChannel = AsynchronousSocketChannel.open();
                try {
                    _clientSocketChannel.connect(hostAddress).get();
                    isConnected = true;
                } catch (ExecutionException | InterruptedException ee) {
                    _clientSocketChannel.close();
                    messageLog("Connecting to server: " + hostAddress + "......");
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        toLog(e.getMessage());
                    }
                }
            }
            messageLog("Connected to server " + hostAddress);

            outcomeClientEventsQueueInit();
            InputStream inputStream = Channels.newInputStream(_clientSocketChannel);
            _objectInputStream = new ObjectInputStream(inputStream);

            new Thread(new ServerEventMonitor()).start();
            new Thread(new ClientInterface()).start();

//            clientSocketChannel.shutdownInput();
//            clientSocketChannel.shutdownOutput();
//            objectInputStream.close();
//            objectOutputStream.close();
//            clientSocketChannel.close();
        } catch (IOException e) {
            messageLog("Server breakdown!");
        }

    }

    private void printMenu() {
        messageLog(    "   You choice: \n" +
                      "1. Send file to server\n" +
                      "2. Receive file from server\n" +
                      "3. List server files\n" +
                      "4. List client files to send\n" +
                      "5. Send task to executor\n" +
                      "6. Transition event demo\n" +
                      "7. Terminate the program\n");
    }

    private class ClientInterface implements  Runnable {

        @Override
        public void run() {
            clientCommandListener();
        }

        private void clientCommandListener() {
            while (true) {
                printMenu();

                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                String choice = "";
                try {
                    choice = stdin.readLine();
                } catch (IOException e) {
                    toLog(e.getMessage());
                }

                switch (choice) {
                    case "1": {
                        sendFileToServer();
                        break;
                    }
                    case "2": {
                        receiveFileFromServer();
                        break;
                    }
                    case "3": {
                        listServerFiles();
                        break;
                    }
                    case "4": {
                        printListClientFilesToSend();
                        break;
                    }
                    case "5": {
                        sendTaskToExecutor();
                        break;
                    }
                    case "6": {
                        transitionEventDemo();
                        break;
                    }
                    case "7": {
                    }
                    ServerStarter.stopAndExit(0);
                }

            }

        }

        private void sendFileToServer() {
            messageLog("[SendFileToServer]");
            for (String file : getClientOutcommingPathContent()) {
                int index = _clientFileListCache.indexOf(file);
                messageLog(String.format("%1$s: %2$s", index, file));
            }
            messageLog("Select file index to send it to the server: ");

            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String choice = "";
            try {
                choice = stdin.readLine();
            } catch (IOException e) {
                toLog(e.getMessage());
            }
            int selectedIndex = Integer.parseInt(choice);

            Path fileToSend = Paths.get(_clientFileListCache.get(selectedIndex));

            byte[] fileContent = new byte[0];
            try {
                fileContent = Files.readAllBytes(fileToSend);
            } catch (IOException e) {
                toLog(e.getMessage());
            }
            long fileSize = fileContent.length;

//            System.out.println(fileToSend.toString() + " " + fileSize);

            PublisherEvent eventToServer = new PublisherEvent(Facade.CMD_SERVER_ADD_FILES, fileContent).toServerCommand();
            eventToServer.setArgs(fileToSend.getFileName().toString(), fileSize);
//            System.out.println(eventToServer.getName());
            sendEventToServer(eventToServer);

        }

        private void receiveFileFromServer() {
            messageLog("[ReceiveFileFromServer]");
            if (_serverFileListCache.size() == 0) return;

            for (String file : _serverFileListCache) {
                int index = _serverFileListCache.indexOf(file);
                messageLog(String.format("%1$s: %2$s", index, file));
            }

            messageLog("Select index of server file to receive it: ");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String choice = "";
            try {
                choice = stdin.readLine();
            } catch (IOException e) {
                toLog(e.getMessage());
            }
            int selectedIndex = Integer.parseInt(choice);

            PublisherEvent eventToServer = new PublisherEvent(Facade.CMD_SERVER_GET_FILES,
                    _serverFileListCache.get(selectedIndex)).toServerCommand();
            System.out.println(eventToServer.getName());
            sendEventToServer(eventToServer);

        }

        private void listServerFiles() {
            messageLog("[ListServerFiles]");
            PublisherEvent eventToServer = new PublisherEvent(Facade.CMD_SERVER_GET_FILES_LIST).toServerCommand();
            sendEventToServer(eventToServer);

        }

        private void printListClientFilesToSend() {
            messageLog("[ListClientFilesToSend]");
            List<String> fileList = getClientOutcommingPathContent();
            for (String file : fileList) {
                int index = fileList.indexOf(file);
                messageLog(String.format("%1$s: %2$s", index, file));
            }
        }

        private void sendTaskToExecutor() {

            System.out.println("TASK SENDED To EXECUTOR");

        }

        private void transitionEventDemo() {
            messageLog("[Transition event demo...]");
            PublisherEvent publisherEvent = new PublisherEvent(Facade.CMD_EXECUTOR_PUT_TASK,
                    "CLIENT: THIS OUTCOME MESSAGE send to EXECUTOR_PUT_TASK")
                    .addServerCommand(Facade.CMD_SERVER_TRANSITION_EVENT);
//            PublisherEvent publisherEvent = new PublisherEvent(Facade.EVENT_GROUP_EXECUTOR,
//                    "HELOO FROM CLIENT").addServerCommand(Facade.CMD_SERVER_ADD_FUTURE_TASK);
//            publisherEvent.setType(Facade.EVENT_TYPE_GROUP);
            sendEventToServer(publisherEvent);
        }

        private List<String> getClientOutcommingPathContent() {
            List<String> fileList = new ArrayList<>();

            try {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
                        _outcomingPath, ConfigManager.getOutcomingTypesGlob());
                for (Path file : directoryStream) {
//                if (!isCorrectFile(file)) continue;
                    fileList.add(file.toString());
                }

            } catch (IOException e) {
                toLog(e.getMessage());
            }

            _clientFileListCache.clear();
            _clientFileListCache.addAll(fileList);

            return fileList;
        }

        private void sendEventToServer(PublisherEvent publisherEvent) {  //synchronyzed if no queue
            try {
                _outcomeClientEventsQueue.put(publisherEvent);
            } catch (InterruptedException e) {
                toLog(e.getMessage());
            }

        }

    }


    //=================================================================================

    private class ServerEventMonitor implements Runnable {

        @Override
        public void run() {
            serverCommandListener();
        }

        private void serverCommandListener() {
            while (true) {
                try {
                    Object receivedObject = _objectInputStream.readObject();

                    if (!receivedObject.getClass().getName().equals(PublisherEvent.class.getName())) {
                        messageLog("Incorrect event object type");
                        continue;
                    }
                    PublisherEvent eventFromServer = (PublisherEvent) receivedObject;

                    if (eventFromServer.getServerCommand() == null) {
                        messageLog("No server command found in event: " + eventFromServer.getName());
                        continue;
                    }

                    if (eventFromServer.getServerCommand().equals(Facade.CMD_SERVER_TERMINATE)) {
                        messageLog("CMD_SERVER_TERMINATE");
                        //clientSocket.close();
                        break;
                    }
                    parseCommandFromServer(eventFromServer);

                } catch (IOException e) {
                    toLog(e.getMessage());
                } catch (ClassNotFoundException e) {
                    toLog(e.getMessage());
                }

            }
//            System.err.println("echo thread close");
        }



        private void parseCommandFromServer(PublisherEvent eventFromServer) {

            switch (eventFromServer.getServerCommand()) {
                case Facade.CMD_SERVER_ADD_FILES: {

//                    System.err.println("Command: " + command);
                    return;
                }
                case Facade.CMD_SERVER_GET_FILES: {
//                    System.err.println("Command: " + command);
                    saveServerFileToReceivedFolder(eventFromServer);
                    return;
                }
                case Facade.CMD_SERVER_GET_FILES_LIST: {
//                    System.err.println("Command: " + command);
                    _serverFileListCache.clear();
                    _serverFileListCache.addAll((List<String>) eventFromServer.getBody());
                    printListFilesFromServer();
                    return;
                }
                case Facade.CMD_SERVER_TRANSITION_EVENT: {
                    publishTransitionEvent(eventFromServer);
                    return;
                }

            }
            messageLog("Incorrect server command: " + eventFromServer.getServerCommand());

        }

        private void publishTransitionEvent(PublisherEvent eventFromServer) {
            Publisher.getInstance().sendPublisherEvent(eventFromServer.toGenericEvent());
        }

        private void saveServerFileToReceivedFolder(PublisherEvent eventFromServer) {
            Path filename = Paths.get(_receivedPath.toString(),
                    Paths.get((String) eventFromServer.getArgs()[0]).getFileName().toString());
            System.out.println(filename.toString() + " @@@ " + (long) eventFromServer.getArgs()[1]);

            byte[] fileContent = (byte[]) eventFromServer.getBody();
            System.out.println("+++" + fileContent.length);

            try {
                Files.write(filename, fileContent, StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
                ServerStarter.stopAndExit(1);
            }

        }

        private void printListFilesFromServer() {

            for (String file : _serverFileListCache) {
                int index = _serverFileListCache.indexOf(file);
                messageLog(String.format("%1$s: %2$s", index, file));
            }
            printMenu();
        }

    }

    private void outcomeClientEventsQueueInit() {

        Thread outcomeQueueThread = new Thread(() -> {
            OutputStream outputStream = Channels.newOutputStream(_clientSocketChannel);
            try {
                _objectOutputStream = new ObjectOutputStream(outputStream);

                while (true) {
                    PublisherEvent publisherEvent = _outcomeClientEventsQueue.take();
                    _objectOutputStream.writeObject(publisherEvent);
                }

            } catch (InterruptedException | IOException e) {
                messageLog("Output stream break!");
            }

        });
        outcomeQueueThread.setName("outcomeServerEventsQueue");
        outcomeQueueThread.start();

    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_RECORD, message);
    }

}