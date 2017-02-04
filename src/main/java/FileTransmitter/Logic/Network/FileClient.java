package FileTransmitter.Logic.Network;

import FileTransmitter.Facade;
import FileTransmitter.Logic.ConfigManager;
import FileTransmitter.Publisher.Interfaces.ISubscriber;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;
import FileTransmitter.Publisher.Publisher;
import FileTransmitter.Publisher.PublisherEvent;
import FileTransmitter.ServerStarter;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FileClient implements ISubscriber {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    private String _clientID = null;

    private boolean uiActive = true;

    private ThreadGroup _clientWorkersThreads;

    private String _remoteServerUrl;
    private int _remoteServerPort;

    private BlockingQueue<PublisherEvent> _outcomeEventsToServerQueue;

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
        _outcomeEventsToServerQueue = new LinkedBlockingQueue<>(50);
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

            initOutcomeEventsToServerQueue();
//            initTransitionEventSender();
            registerOnPublisher();

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
                      "5. Run task factory\n" +
                      "6. Transition event demo\n" +
                      "7. Terminate the program\n");
    }

    private class ClientInterface implements  Runnable {

        @Override
        public void run() {
            clientCommandListener();
        }

        private void clientCommandListener() {
            while (uiActive) {
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
                        runTaskFactory();
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
            messageLog("/Client UI terminate/");

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

            PublisherEvent eventToServer = new PublisherEvent(Facade.SERVER_ADD_FILES, fileContent).toServerCommand();
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

            PublisherEvent eventToServer = new PublisherEvent(Facade.SERVER_GET_FILES,
                    _serverFileListCache.get(selectedIndex)).toServerCommand();
            sendEventToServer(eventToServer);

        }

        private void listServerFiles() {
            messageLog("[ListServerFiles]");
            PublisherEvent eventToServer = new PublisherEvent(Facade.SERVER_GET_FILES_LIST).toServerCommand();
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

        private void runTaskFactory() {
            Publisher.getInstance().sendPublisherEvent(Facade.CMD_TASK_EXECUTOR_START, getClientID());

        }

        private void transitionEventDemo() {
            messageLog("[Transition event demo...]");
            PublisherEvent publisherEvent = new PublisherEvent(Facade.CMD_EXECUTOR_DEMO,
                    "CLIENT: THIS OUTCOME MESSAGE send to CMD_EXECUTOR_DEMO")
                    .addServerCommand(Facade.SERVER_TRANSITION_EVENT);

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

    }

    private void sendEventToServer(PublisherEvent publisherEvent) {  //synchronyzed if no queue
        try {
            _outcomeEventsToServerQueue.put(publisherEvent);
        } catch (InterruptedException e) {
            toLog(e.getMessage());
        }

    }


    //=================================================================================

    private class ServerEventMonitor implements Runnable {

        @Override
        public void run() {
            serverCommandListener();
        }

        private void serverCommandListener() {
            try {
                while (true) {
                    Object receivedObject = _objectInputStream.readObject();

                    if (!receivedObject.getClass().getName().equals(PublisherEvent.class.getName())) {
                        messageLog("Incorrect event object type");
                        continue;
                    }
                    PublisherEvent eventFromServer = (PublisherEvent) receivedObject;

                    if (eventFromServer.getServerCommand() == null) {
                        messageLog("No server command found in event: " + eventFromServer.getInterestName());
                        continue;
                    }

                    if (eventFromServer.getServerCommand().equals(Facade.SERVER_TERMINATE)) {
                        messageLog("CMD_SERVER_TERMINATE");
                        //clientSocket.close();
                        break;
                    }
                    parseCommandFromServer(eventFromServer);
                }

            } catch (IOException e) {
//                    toLog(e.getMessage());
                messageLog(e.getMessage());
            } catch (ClassNotFoundException e) {
                messageLog(e.getMessage());
//                    toLog(e.getMessage());
            }

        }


        private void parseCommandFromServer(PublisherEvent eventFromServer) {

            switch (eventFromServer.getServerCommand()) {
                case Facade.SERVER_SET_CLIENT_ID: {
                    setClientID((String) eventFromServer.getBody());
                    toLog(getClientID());
                    return;
                }

                case Facade.SERVER_ADD_FILES: {

//                    System.err.println("Command: " + command);
                    return;
                }
                case Facade.SERVER_GET_FILES: {
//                    System.err.println("Command: " + command);
                    saveServerFileToReceivedFolder(eventFromServer);
                    return;
                }
                case Facade.SERVER_GET_FILES_LIST: {
//                    System.err.println("Command: " + command);
                    _serverFileListCache.clear();
                    _serverFileListCache.addAll((List<String>) eventFromServer.getBody());
                    printListFilesFromServer();
                    return;
                }
                case Facade.SERVER_TRANSITION_EVENT: {
                    publishTransitionEventFromServer(eventFromServer);
                    return;
                }

            }
            messageLog("Incorrect server command: " + eventFromServer.getServerCommand());

        }

        private void publishTransitionEventFromServer(PublisherEvent eventFromServer) {
            Publisher.getInstance().sendPublisherEvent(eventFromServer);
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

    private void initOutcomeEventsToServerQueue() {

        Thread outcomeQueueThread = new Thread(_clientWorkersThreads, () -> {
            OutputStream outputStream = Channels.newOutputStream(_clientSocketChannel);
            try {
                _objectOutputStream = new ObjectOutputStream(outputStream);

                while (true) {
                    PublisherEvent publisherEvent = _outcomeEventsToServerQueue.take();
                    _objectOutputStream.writeObject(publisherEvent);
                }

            } catch (InterruptedException | IOException e) {
                messageLog("Output stream break!");
            }

        }, "outcomeEventsToServerQueue");
        outcomeQueueThread.start();

    }

//    private void initTransitionEventSender() {
//        Thread transitionEventSenderThread = new Thread(_clientWorkersThreads, () -> {
//            while (true) {
//                PublisherEvent outcomeTransitionEvent = Publisher.getInstance().getTransitionEvent();
//                outcomeTransitionEvent.setServerCommand(Facade.SERVER_TRANSITION_EVENT);
//                sendEventToServer(outcomeTransitionEvent);
//                System.out.println("outcomeTransitionEvent send");
//
//            }
//        }, "transitionEventSenderThread");
//        transitionEventSenderThread.start();
//
//    }

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewSubscriber(this, Facade.TRANSITION_EVENT_GROUP_CLIENT);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                Facade.CMD_NET_CLIENT_UI_BREAK,
                Facade.CMD_NET_CLIENT_SHUTDOWN
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
        if (publisherEvent.getServerCommand().equals(Facade.SERVER_TRANSITION_EVENT)) {
            messageLog("Send transition event to Server, type is: " + publisherEvent.getGroupName());
            sendEventToServer((PublisherEvent) publisherEvent);
            return;
        }
        if (publisherEvent.getType().equals(Facade.EVENT_TYPE_GROUP)) {
            messageLog("NetClient received group event ("
                    + publisherEvent.getGroupName() + "): \n" + publisherEvent.getBody().toString());
        }

        switch (publisherEvent.getInterestName()) {
            case Facade.CMD_NET_CLIENT_UI_BREAK: {
                messageLog("CMD_NET_CLIENT_UI_BREAK " + publisherEvent.getBody());
                uiActive = false;
                break;
            }

            case Facade.CMD_NET_CLIENT_SHUTDOWN: {
                messageLog("CMD_NET_CLIENT_SHUTDOWN " + publisherEvent.getBody());
                break;
            }

        }

    }

    private void setClientID(String clientID) {
        if (_clientID != null) return;
        _clientID = clientID;
    }
    private String getClientID() {
        return _clientID;
    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_RECORD, message);
    }

}