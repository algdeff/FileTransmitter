package Transmitter.Logic.Network;

import static Transmitter.Facade.*;
import Transmitter.Logic.ConfigManager;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;
import Transmitter.Publisher.Publisher;
import Transmitter.Publisher.PublisherEvent;
import Transmitter.ServerStarter;

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

public class RemoteClient implements ISubscriber {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    private String _clientID = null;

    private volatile boolean _sessionActive = true;
    private boolean _uiActive = true;

    private boolean _skipClientMenu = false;

    private ThreadGroup _clientControllerThreads;

    private String _remoteServerUrl;
    private int _remoteServerPort;

    private BlockingQueue<PublisherEvent> _outcomeEventsToServerQueue;

    private AsynchronousSocketChannel _clientSocketChannel;
    private ObjectOutputStream _objectOutputStream;
    private ObjectInputStream _objectInputStream;

    private List<FileContext> _serverFilesListCache;


    public RemoteClient(String remoteServerUrl, int remoteServerPort) {
        _remoteServerUrl = remoteServerUrl;
        _remoteServerPort = remoteServerPort;
        _receivedPath = ConfigManager.getReceivedPath();
        _outcomingPath = ConfigManager.getOutcomingPath();
        _sentPath = ConfigManager.getSentPath();
        _clientControllerThreads = new ThreadGroup("clientControllerThreads");
        _outcomeEventsToServerQueue = new LinkedBlockingQueue<>(50);
        _serverFilesListCache = new ArrayList<>();
        _clientSocketChannel = null;
    }

    public void start() {
        openConnectionToServer();

    }

    private void openConnectionToServer() {
//        int threadsNumber = 10;
//        ExecutorService executorService = Executors.newWorkStealingPool(threadsNumber); //ForkJoinPool.commonPool(); //Executors.newFixedThreadPool(50);

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
                    message("Connecting to server: " + hostAddress + "......");
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        toLog(e.getMessage());
                    }
                }
            }
            message("Connected to server " + hostAddress);

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
            message("Server breakdown!");
            clientShutdown();
        }

    }

    private void initOutcomeEventsToServerQueue() {

        Thread outcomeQueueThread = new Thread(_clientControllerThreads, () -> {
            OutputStream outputStream = Channels.newOutputStream(_clientSocketChannel);
            try {
                _objectOutputStream = new ObjectOutputStream(outputStream);

                while (_sessionActive) {
                    PublisherEvent publisherEvent = _outcomeEventsToServerQueue.take();
                    _objectOutputStream.writeObject(publisherEvent);
                }

            } catch (InterruptedException | IOException e) {
                message("Output stream break!");
            } finally {
                clientShutdown();
            }

        }, "outcomeEventsToServerQueue");
        outcomeQueueThread.start();

    }

    private void clientShutdown() {
        if (!_sessionActive) {
            return;
        }
        _sessionActive = false;

        try {
            _objectOutputStream.flush();
            _objectOutputStream.close();
        } catch (IOException e) {
            toLog("Client shutdown...IOException");
        } finally {
            message("CLIENT (" + _clientID + ") SHUTDOWN...");
        }

        ServerStarter.stopAndExit(1);

    }


    //=============================================================================================

    private class ServerEventMonitor implements Runnable {

        @Override
        public void run() {
            serverCommandListener();
        }

        private void serverCommandListener() {
            try {
                while (_sessionActive) {
                    Object receivedObject = _objectInputStream.readObject();

                    if (!receivedObject.getClass().getName().equals(PublisherEvent.class.getName())) {
                        message("Incorrect event object type");
                        continue;
                    }
                    PublisherEvent eventFromServer = (PublisherEvent) receivedObject;

                    if (eventFromServer.getServerCommand() == null) {
                        message("No server command found in event: " + eventFromServer.getInterestName());
                        continue;
                    }

                    if (eventFromServer.getServerCommand().equals(CMD_SERVER_TERMINATE)) {
                        message("CMD_SERVER_TERMINATE");
                        clientShutdown();
                        break;
                    }
                    parseCommandFromServer(eventFromServer);
                }

            } catch (IOException | ClassNotFoundException e) {
                message("serverCommandListener interrupted: " + e.getMessage());
                clientShutdown();
            }

        }

        private void parseCommandFromServer(PublisherEvent eventFromServer) {

            switch (eventFromServer.getServerCommand()) {
                case CMD_SERVER_SET_CLIENT_ID: {
                    setClientID((String) eventFromServer.getBody());
                    toLog(getClientID());
                    return;
                }

                case CMD_SERVER_ADD_FILES: {

//                    System.err.println("Command: " + command);
                    return;
                }
                case CMD_SERVER_GET_FILES: {
//                    System.err.println("Command: " + command);
                    saveServerFileToReceivedFolder((FileContext) eventFromServer.getBody());
                    return;
                }
                case CMD_SERVER_GET_FILES_LIST: {
//                    System.err.println("Command: " + command);
                    _serverFilesListCache.clear();
                    _serverFilesListCache.addAll((List<FileContext>) eventFromServer.getBody());
                    showServerFilesList();
                    showClientMenu();
                    return;
                }
                case CMD_SERVER_REMOTE_PROCEDURE_CALL: {
                    return;
                }
                case CMD_SERVER_TRANSITION_EVENT: {
                    publishTransitionEventFromServer(eventFromServer);
                    return;
                }

            }
            message("Incorrect server command: " + eventFromServer.getServerCommand());

        }

    }

    private void sendEventToServer(PublisherEvent publisherEvent) {  //synchronyzed if no queue
        try {
            _outcomeEventsToServerQueue.put(publisherEvent);
        } catch (InterruptedException e) {
            messageAndLog("sendEventToServer interrupted");
            toLog(e.getMessage());
            clientShutdown();
        }

    }

    private void callRemoteProcedureOnServer(Runnable runnable) {
        PublisherEvent procedureEvent = new PublisherEvent(CMD_SERVER_REMOTE_PROCEDURE_CALL, runnable).toServerCommand();
        sendEventToServer(procedureEvent);
    }


    //=============================================================================================

    private void saveServerFileToReceivedFolder(FileContext fileContext) {
        Path filename = Paths.get(_receivedPath.toString(), fileContext.getFileName());

        byte[] fileContent = fileContext.getFileContent();
//            System.out.println("+++" + fileContent.length);

        try {
            Files.write(filename, fileContent, StandardOpenOption.CREATE);
        } catch (IOException e) {
            messageAndLog("Error in saveServerFileToReceivedFolder");
            toLog(e.getMessage());
        }

        message("Done");
    }

    private void showServerFilesList() {

        for (FileContext file : _serverFilesListCache) {
            int index = _serverFilesListCache.indexOf(file);
            file.setFileIndex(index);
            message(String.format("%1$s: %2$s (%3$s KiB)", index, file.getFileName(), file.getFormattedSizeKb()));
        }
    }

    private void publishTransitionEventFromServer(PublisherEvent eventFromServer) {
        Publisher.getInstance().sendPublisherEvent(eventFromServer);
    }


    //=============================================================================================

    private void showClientMenu() {
        if (_skipClientMenu) {
            _skipClientMenu = false;
            return;
        }

        message( "   You choice: \n" +
                "1. Send file to server\n" +
                "2. Receive file from server\n" +
                "3. List server files\n" +
                "4. List client files to send\n" +
                "5. Run task factory\n" +
                "6. Remote procedure call on server\n" +
                "7. Transition event demo\n" +
                "8. Terminate the program\n");
    }

    private class ClientInterface implements  Runnable {

        @Override
        public void run() {
            clientCommandListener();
        }

        private void clientCommandListener() {
            try {

                showClientMenu();

                while (_uiActive && _sessionActive) {

                    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                    String choice = stdin.readLine();

                    switch (choice) {
                        case "1": {
                            selectAndSendFilesToServer();
                            break;
                        }
                        case "2": {
                            selectFilesToReceiveFromServer();
                            break;
                        }
                        case "3": {
                            listServerFiles();
                            break;
                        }
                        case "4": {
                            listClientFiles();
                            break;
                        }
                        case "5": {
                            runTaskFactory();
                            break;
                        }
                        case "6": {
                            callRemoteProcedure();
                            break;
                        }
                        case "7": {
                            transitionEventDemo();
                            break;
                        }
                        case "8": {
                        }
                        clientShutdown();
                    }

                }
            } catch (IOException e) {
                toLog(e.getMessage());
            }

            message("/Client UI terminate/");

        }

        private void selectAndSendFilesToServer() {
            List<FileContext> fileList = getClientOutcommingPathContent();

            message("[SendFilesToServer]");
            showClientFilesList(fileList);
            message("Select file index to send it to the server: ");

            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String choice = "";
            try {
                choice = stdin.readLine();
            } catch (IOException e) {
                toLog(e.getMessage());
            }
            int selectedIndex = Integer.parseInt(choice);

            if (selectedIndex >= fileList.size() || selectedIndex < 0) {
                message("Incorrect file index!");
                return;
            }

            List<FileContext> selectedFiles = new ArrayList<>();
            selectedFiles.add(fileList.get(selectedIndex));

            sendSelectedFilesToServer(selectedFiles);

        }

        private void sendSelectedFilesToServer(List<FileContext> selectedFiles) {
            for (FileContext fileContext : selectedFiles) {
                sendFileToServer(fileContext);
            }
        }

        private void sendFileToServer(FileContext fileContext) {
            Path fileToSend = Paths.get(fileContext.getFileFullPath()).normalize();

            if (!Files.exists(fileToSend)) {
                message("File: " + fileToSend.toString() + " is not exist");
                return;
            }

            byte[] fileContent = new byte[0];
            try {
                fileContent = Files.readAllBytes(fileToSend);
            } catch (IOException e) {
                toLog(e.getMessage());
            }
            long fileSize = fileContent.length;

            fileContext.setFileSize(fileSize);
            fileContext.setFileContent(fileContent);

            PublisherEvent eventToClient = new PublisherEvent(CMD_SERVER_ADD_FILES, fileContext).toServerCommand();
            sendEventToServer(eventToClient);

            Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_FILE_TO_STATISTICS, fileToSend.toString());

        }


        private void selectFilesToReceiveFromServer() {
            message("[ReceiveFileFromServer]");
            if (_serverFilesListCache.size() == 0) {
                skipClientMenu();
                listServerFiles();
            } else {
                showServerFilesList();
            }

            message("Select index of server file to receive it: ");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String choice = "";
            try {
                choice = stdin.readLine();
            } catch (IOException e) {
                toLog(e.getMessage());
            }
            int selectedIndex = Integer.parseInt(choice);

            if (selectedIndex >= _serverFilesListCache.size() || selectedIndex < 0) {
                message("Incorrect file index!");
                showClientMenu();
                return;
            }

            List<FileContext> selectedFiles = new ArrayList<>();
            selectedFiles.add(_serverFilesListCache.get(selectedIndex));

            PublisherEvent eventToServer = new PublisherEvent(
                    CMD_SERVER_GET_FILES, selectedFiles).toServerCommand();
            sendEventToServer(eventToServer);

        }

        private void listServerFiles() {
            message("[ListServerFiles]");
            PublisherEvent eventToServer = new PublisherEvent(CMD_SERVER_GET_FILES_LIST).toServerCommand();
            sendEventToServer(eventToServer);

        }

        private void listClientFiles() {
            message("[ListClientFiles]");
            showClientFilesList(getClientOutcommingPathContent());
        }

        private void runTaskFactory() {
            Publisher.getInstance().sendPublisherEvent(CMD_TASK_EXECUTOR_START, getClientID());

        }

        private void callRemoteProcedure() {
            message("[RemoteProcedureCall]");
            callRemoteProcedureOnServer(new WebServiceExecutable(getClientID()));
        }

        private void transitionEventDemo() {
            message("[Transition event demo...]");
            PublisherEvent publisherEvent = new PublisherEvent(CMD_EXECUTOR_DEMO,
                    "CLIENT: THIS OUTCOME MESSAGE send to CMD_EXECUTOR_DEMO")
                    .addServerCommand(CMD_SERVER_TRANSITION_EVENT);

            sendEventToServer(publisherEvent);
        }

        private void showClientFilesList(List<FileContext> fileList) {
            for (FileContext file : fileList) {
                int index = fileList.indexOf(file);
                message(String.format("%1$s: %2$s (%3$s KiB)", index, file.getFileName(), file.getFormattedSizeKb()));
            }
        }

        private List<FileContext> getClientOutcommingPathContent() {
            List<FileContext> fileList = new ArrayList<>();

            try {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
                        _outcomingPath, ConfigManager.getOutcomingTypesGlob());
                for (Path file : directoryStream) {
                    if (!isCorrectFile(file)) continue;
                    FileContext fileContext = new FileContext(file);
                    fileContext.setFileSize(Files.size(file));
                    fileList.add(fileContext);
                }

            } catch (IOException e) {
                toLog(e.getMessage());
            }

            return fileList;
        }

        private boolean isCorrectFile(Path pathname) {
            if (Files.isSymbolicLink(pathname)
                    || !Files.isReadable(pathname)
                    || Files.isDirectory(pathname)) return false;

            return true;
        }

    }


    //=============================================================================================

    private void setClientID(String clientID) {
        if (_clientID != null) return;
        _clientID = clientID;
    }
    private String getClientID() {
        return _clientID;
    }

    private void skipClientMenu() {
        _skipClientMenu = true;
    }

    private void message(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_CONSOLE_MESSAGE, message);
    }

    private void messageAndLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_RECORD, message);
    }


    //=============================================================================================

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerNewSubscriber(this, TRANSITION_EVENT_GROUP_CLIENT);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[] {
                CMD_NET_CLIENT_UI_BREAK,
                GLOBAL_SHUTDOWN
        };
    }

    @Override
    public void listenerHandler(IPublisherEvent publisherEvent) {
        if (publisherEvent.getServerCommand().equals(CMD_SERVER_TRANSITION_EVENT)) {
//            message("Send transition event to Server, type is: " + publisherEvent.getGroupName());
            sendEventToServer((PublisherEvent) publisherEvent);
            return;
        }
        if (publisherEvent.getType().equals(EVENT_TYPE_GROUP)) {
            message("NetClient received group event ("
                    + publisherEvent.getGroupName() + "): \n" + publisherEvent.getBody().toString());
        }

        switch (publisherEvent.getInterestName()) {
            case CMD_NET_CLIENT_UI_BREAK: {
                message("CMD_NET_CLIENT_UI_BREAK " + publisherEvent.getBody());
                _uiActive = false;
                break;
            }

            case GLOBAL_SHUTDOWN: {
                clientShutdown();
                break;
            }

        }

    }

}