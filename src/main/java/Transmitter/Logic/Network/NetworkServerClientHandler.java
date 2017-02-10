package Transmitter.Logic.Network;

import Transmitter.Facade;
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

                sendEventToClient(new PublisherEvent(Facade.SERVER_SET_CLIENT_ID, _clientID).toServerCommand());

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

                    if (eventFromClient.getServerCommand().equals(Facade.SERVER_TERMINATE)) {
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
            Publisher.getInstance().sendPublisherEvent(Facade.CMD_SERVER_INTERNAL_CLIENT_SHUTDOWN, _clientID);
            _clientHandlerThreads.interrupt();
        }

//            ThreadPoolManager.getInstance().shutdownRunnableTasks();
    }

    private void parseCommandFromClient(PublisherEvent eventFromClient) {

        switch (eventFromClient.getServerCommand()) {
            case Facade.SERVER_ADD_FILES: {
//                System.err.println("Command: " + command);
                saveClientFileToReceivedFolder(eventFromClient);
                return;
            }
            case Facade.SERVER_GET_FILES: {
//                System.err.println("Command: " + command);
                sendServerFileToClient((String) eventFromClient.getBody());
                return;
            }
            case Facade.SERVER_GET_FILES_LIST: {
//                System.err.println("Command: " + command);
                sendServerFileListToClient();
                return;
            }
            case Facade.SERVER_TRANSITION_EVENT: {
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

    private void saveClientFileToReceivedFolder(PublisherEvent eventFromClient) {
        Path filename = Paths.get(_receivedPath.toString(), (String) eventFromClient.getArgs()[0]);
        System.out.println(filename.toString() + " @@@ " + (long) eventFromClient.getArgs()[1]);

        byte[] fileContent = (byte[]) eventFromClient.getBody();

//        System.out.println("+++" + fileContent.length);

        try {
            Files.write(filename, fileContent, StandardOpenOption.CREATE);
        } catch (IOException e) {
            messageLog(e.getMessage());
            ServerStarter.stopAndExit(1);
        }

    }

    private void sendServerFileToClient(String filename) {
        Path fileToSend = Paths.get(filename).normalize();

        byte[] fileContent = new byte[0];
        try {
            fileContent = Files.readAllBytes(fileToSend);
        } catch (IOException e) {
            toLog(e.getMessage());
        }
        long fileSize = fileContent.length;
//        System.out.println(filename + " " + fileToSend.toString() + " " + fileSize);

        PublisherEvent eventToClient = new PublisherEvent(Facade.SERVER_GET_FILES, fileContent).toServerCommand();
        eventToClient.setArgs(fileToSend.toString(), fileSize);
        sendEventToClient(eventToClient);

    }

    private void sendServerFileListToClient() {
        PublisherEvent eventToServer = new PublisherEvent(Facade.SERVER_GET_FILES_LIST).toServerCommand();
        eventToServer.setBody(getServerOutcommingPathContent());
        sendEventToClient(eventToServer);

    }

    private List<String> getServerOutcommingPathContent() {
        List<String> fileList = new ArrayList<>();

        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(_outcomingPath, ConfigManager
                    .getOutcomingTypesGlob());
            for (Path file : directoryStream) {
//                if (!isCorrectFile(file)) continue;
                fileList.add(file.toString());
            }

        } catch (IOException e) {
            toLog("directoryWalking:" + e.getMessage());
        }

        return fileList;
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

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_RECORD, message);
    }

    @Override
    public void registerOnPublisher() {
        Publisher.getInstance().registerRemoteUser(this, _clientID, Facade.TRANSITION_EVENT_GROUP_ALL_USERS);
    }

    @Override
    public String[] subscriberInterests() {
        return new String[0];
    }

    @Override
    public void listenerHandler(IPublisherEvent outcomeTransitionEvent) {
        if (outcomeTransitionEvent.getServerCommand().equals(Facade.SERVER_TRANSITION_EVENT)) {
            messageLog("Send transition event to client: " + _clientID);
            sendEventToClient((PublisherEvent) outcomeTransitionEvent);
            return;
        }

    }
}
