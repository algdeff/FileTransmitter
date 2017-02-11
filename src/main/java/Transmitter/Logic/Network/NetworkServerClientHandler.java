package Transmitter.Logic.Network;

import static Transmitter.Facade.*;
import Transmitter.Logic.ConfigManager;
import Transmitter.Publisher.Interfaces.ISubscriber;
import Transmitter.Publisher.Interfaces.IPublisherEvent;
import Transmitter.Publisher.Publisher;
import Transmitter.Publisher.PublisherEvent;
import Transmitter.ServerStarter;

import java.io.*;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class NetworkServerClientHandler implements ISubscriber, Runnable {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    private String _clientID;

    private volatile boolean _sessionActive = true;

    private ThreadGroup _clientHandlerThreads;

    private BlockingQueue<PublisherEvent> _outcomeEventsToClientQueue;

    private AsynchronousSocketChannel _serverSocketChanhel;
    private ObjectInputStream _objectInputStream;
    private ObjectOutputStream _objectOutputStream;

    public NetworkServerClientHandler(AsynchronousSocketChannel socketChanhel, String clientID) {
        _clientID = clientID;
        _serverSocketChanhel = socketChanhel;
        _receivedPath = ConfigManager.getReceivedPath();
        _outcomingPath = ConfigManager.getOutcomingPath();
        _sentPath = ConfigManager.getSentPath();
        _clientHandlerThreads = new ThreadGroup("clientHandlerThreads");
        _outcomeEventsToClientQueue = new LinkedBlockingQueue<>(50);
    }

    @Override
    public void run() {
        clientHandle();
    }

    private void clientHandle() {
        SocketAddress clientAddress = null;
        try {
            clientAddress = _serverSocketChanhel.getRemoteAddress();
            if (_serverSocketChanhel.isOpen()) {
                messageLog("New client (" + _clientID + ") connected: " + clientAddress);

                InputStream inputStream = Channels.newInputStream(_serverSocketChanhel);
                _objectInputStream = new ObjectInputStream(inputStream);
                initOutcomeEventsToClientQueue();
//                initTransitionEventSender();
                registerOnPublisher();

                sendEventToClient(new PublisherEvent(SERVER_SET_CLIENT_ID, _clientID).toServerCommand());

                while (_sessionActive) {
                    Object receivedObject = _objectInputStream.readObject();

                    if (!receivedObject.getClass().getName().equals(PublisherEvent.class.getName())) {
                        messageLog("Incorrect event object type");
                        continue;
                    }
                    PublisherEvent eventFromClient = (PublisherEvent) receivedObject;

                    if (eventFromClient.getServerCommand() == null) {
                        messageLog("No server command found in event: " + eventFromClient.getInterestName());
                        continue;
                    }

                    if (eventFromClient.getServerCommand().equals(SERVER_TERMINATE)) {
                        messageLog("CMD_SERVER_TERMINATE");
                        //clientSocket.close();
                        break;
                    }
                    parseCommandFromClient(eventFromClient);
                }

//                clientSocket.shutdownInput();
//                clientSocket.shutdownOutput();
                messageLog("Client (" + clientAddress + ") successfully terminated");
            }
        } catch (ClassNotFoundException e) {
            toLog(e.getMessage());
        } catch (IOException e) {
            messageLog("Client ("+ clientAddress +") is breakdown!");
        } finally {
            clientShutdown();
        }

        //System.out.println("Client terminated " + clientAddress.toString());
    }

    private void clientShutdown() {
        if (!_sessionActive) {
            return;
        }
        _sessionActive = false;

        try {
            _objectOutputStream.flush();
//            _objectOutputStream.close();
            _serverSocketChanhel.shutdownOutput();
            _serverSocketChanhel.shutdownInput();
            _serverSocketChanhel.close();
        } catch (IOException e) {
            toLog("Client shutdown...IOException");
        } finally {
            messageLog("CLIENT (" + _clientID + ") SHUTDOWN...");
            Publisher.getInstance().unregisterRemoteUser(_clientID);
            Publisher.getInstance().sendPublisherEvent(CMD_SERVER_INTERNAL_CLIENT_SHUTDOWN, _clientID);
            _clientHandlerThreads.interrupt();
        }

//            ThreadPoolManager.getInstance().shutdownRunnableTasks();
    }

    private void parseCommandFromClient(PublisherEvent eventFromClient) {

        switch (eventFromClient.getServerCommand()) {
            case SERVER_ADD_FILES: {
//                System.err.println("Command: " + command);
                saveClientFileToReceivedFolder((FileContext) eventFromClient.getBody());
                return;
            }
            case SERVER_GET_FILES: {
//                System.err.println("Command: " + command);
                sendSelectedServerFilesToClient((List<FileContext>) eventFromClient.getBody());
                return;
            }
            case SERVER_GET_FILES_LIST: {
//                System.err.println("Command: " + command);
                sendServerFileListToClient();
                return;
            }
            case SERVER_TRANSITION_EVENT: {
                publishTransitionEventFromClient(eventFromClient);
                return;
            }

        }
        messageLog("Incorrect client command: " + eventFromClient.getServerCommand());

    }

    private void publishTransitionEventFromClient(PublisherEvent eventFromClient) {
        Publisher.getInstance().sendPublisherEvent(eventFromClient);

    }

    private void sendEventToClient(PublisherEvent publisherEvent) {  //synchronized if no queue
        try {
            _outcomeEventsToClientQueue.put(publisherEvent);
        } catch (InterruptedException e) {
            toLog(e.getMessage());
        }

    }

    private void initOutcomeEventsToClientQueue() {
        Thread outcomeQueueThread = new Thread(_clientHandlerThreads, () -> {
            OutputStream outputStream = Channels.newOutputStream(_serverSocketChanhel);
            try {
                _objectOutputStream = new ObjectOutputStream(outputStream);

                while (_sessionActive) {
                    PublisherEvent publisherEvent = _outcomeEventsToClientQueue.take();
                    _objectOutputStream.writeObject(publisherEvent);
                }

            } catch (InterruptedException | IOException e) {
                toLog("Output stream break!");
            } finally {
                clientShutdown();
            }

        }, "outcomeEventsToClientQueue");
        outcomeQueueThread.start();
//        ThreadPoolManager.getInstance().addRunnableTask(_outcomeQueueThread);

    }

    private void saveClientFileToReceivedFolder(FileContext fileContext) {
        Path filename = Paths.get(_receivedPath.toString(), fileContext.getFileName());

        byte[] fileContent = fileContext.getFileContent();
//            System.out.println("+++" + fileContent.length);

        try {
            Files.write(filename, fileContent, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
            ServerStarter.stopAndExit(1);
        }

        sendMessageToClient("Received transmission", _clientID);
        messageLog("Done");

    }

    private void sendSelectedServerFilesToClient(List<FileContext> selectedFiles) {
        for (FileContext fileContext : selectedFiles) {
            sendFileToClient(fileContext);
        }
    }


    private void sendFileToClient(FileContext fileContext) {
        Path fileToSend = Paths.get(fileContext.getFileFullPath()).normalize();

        if (!Files.exists(fileToSend)) {
            sendMessageToClient("(for client " + _clientID + "): File "
                    + fileToSend.toString() + " is not exist", _clientID);
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

        sendMessageToClient("Start transmission: " + fileToSend.getFileName().toString()
                + " @ " + fileSize + " bytes...", _clientID);

        PublisherEvent eventToClient = new PublisherEvent(SERVER_GET_FILES, fileContext).toServerCommand();
        sendEventToClient(eventToClient);

        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_FILE_TO_STATISTICS, fileToSend.toString());

    }

    private void sendServerFileListToClient() {
        PublisherEvent eventToServer = new PublisherEvent(SERVER_GET_FILES_LIST).toServerCommand();
        eventToServer.setBody(getServerOutcommingPathContent());
        sendEventToClient(eventToServer);

    }

    private List<FileContext> getServerOutcommingPathContent() {
        List<FileContext> fileList = new ArrayList<>();

        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(_outcomingPath, ConfigManager
                    .getOutcomingTypesGlob());
            for (Path file : directoryStream) {
                if (!isCorrectFile(file)) continue;
                FileContext fileContext = new FileContext(file);
                fileContext.setFileSize(Files.size(file));
                fileList.add(fileContext);
            }

        } catch (IOException e) {
            toLog("directoryWalking:" + e.getMessage());
        }

        return fileList;
    }

    private boolean isCorrectFile(Path pathname) {
        if (Files.isSymbolicLink(pathname)
                || !Files.isReadable(pathname)
                || Files.isDirectory(pathname)) return false;

        return true;

//        PathMatcher pathMatcher = FileSystems.getDefault()
//                .getPathMatcher("glob:" + ConfigManager
//                        .getOutcomingTypesGlob());
//
//        return pathMatcher.matches(pathname.getFileName());
    }

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerRemoteUser(this, _clientID, TRANSITION_EVENT_GROUP_ALL_USERS);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[0];
    }

    @Override
    public void listenerHandler(IPublisherEvent outcomeTransitionEvent) {
        if (outcomeTransitionEvent.getServerCommand().equals(SERVER_TRANSITION_EVENT)) {
            messageLog("Send transition event to client: " + _clientID);
            sendEventToClient((PublisherEvent) outcomeTransitionEvent);
            return;
        }

    }

    private void sendMessageToClient(String message, String ClientID) {
        Publisher.getInstance().sendTransitionEvent(new PublisherEvent(
                CMD_LOGGER_CONSOLE_MESSAGE, "[SERVER] " + message), ClientID);
    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_CONSOLE_MESSAGE, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_RECORD, message);
    }

}
