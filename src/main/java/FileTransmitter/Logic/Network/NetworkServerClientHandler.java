package FileTransmitter.Logic.Network;

import FileTransmitter.Facade;
import FileTransmitter.Logic.ConfigManager;
import FileTransmitter.Logic.ThreadPoolManager;
import FileTransmitter.Publisher.Interfaces.IListener;
import FileTransmitter.Publisher.Interfaces.IPublisherEvent;
import FileTransmitter.Publisher.Publisher;
import FileTransmitter.Publisher.PublisherEvent;
import FileTransmitter.ServerStarter;

import java.io.*;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class NetworkServerClientHandler implements Runnable {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    private BlockingQueue<PublisherEvent> _outcomeServerEventsQueue;

    private AsynchronousSocketChannel _serverSocketChanhel;
    private ObjectInputStream _objectInputStream;
    private ObjectOutputStream _objectOutputStream;

    public NetworkServerClientHandler(AsynchronousSocketChannel socketChanhel) {
        _serverSocketChanhel = socketChanhel;
        _receivedPath = ConfigManager.getReceivedPath();
        _outcomingPath = ConfigManager.getOutcomingPath();
        _sentPath = ConfigManager.getSentPath();
        _outcomeServerEventsQueue = new LinkedBlockingQueue<>(50);
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
                messageLog("New client connected: " + clientAddress);

                InputStream inputStream = Channels.newInputStream(_serverSocketChanhel);
                _objectInputStream = new ObjectInputStream(inputStream);
                initOutcomeServerEventsQueue();
                initTransitionEventSender();

                while (true) {
                    Object receivedObject = _objectInputStream.readObject();

                    if (!receivedObject.getClass().getName().equals(PublisherEvent.class.getName())) {
                        messageLog("Incorrect event object type");
                        continue;
                    }
                    PublisherEvent eventFromClient = (PublisherEvent) receivedObject;

                    if (eventFromClient.getServerCommand() == null) {
                        messageLog("No server command found in event: " + eventFromClient.getName());
                        continue;
                    }

                    if (eventFromClient.getServerCommand().equals(Facade.CMD_SERVER_TERMINATE)) {
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
        }

        //System.out.println("Client terminated " + clientAddress.toString());
    }

    private void parseCommandFromClient(PublisherEvent eventFromClient) {

        switch (eventFromClient.getServerCommand()) {
            case Facade.CMD_SERVER_ADD_FILES: {
//                System.err.println("Command: " + command);
                saveClientFileToReceivedFolder(eventFromClient);
                return;
            }
            case Facade.CMD_SERVER_GET_FILES: {
//                System.err.println("Command: " + command);
                sendServerFileToClient((String) eventFromClient.getBody());
                return;
            }
            case Facade.CMD_SERVER_GET_FILES_LIST: {
//                System.err.println("Command: " + command);
                sendServerFileListToClient();
                return;
            }
            case Facade.CMD_SERVER_TRANSITION_EVENT: {
                publishTransitionEvent(eventFromClient);
                return;
            }

        }
        messageLog("Incorrect client command: " + eventFromClient.getServerCommand());

    }

    private void publishTransitionEvent(PublisherEvent eventFromClient) {
        Publisher.getInstance().sendPublisherEvent(eventFromClient.toGenericEvent());
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

        PublisherEvent eventToClient = new PublisherEvent(Facade.CMD_SERVER_GET_FILES, fileContent).toServerCommand();
        eventToClient.setArgs(fileToSend.toString(), fileSize);
        sendEventToClient(eventToClient);

    }

    private void sendServerFileListToClient() {
        PublisherEvent eventToServer = new PublisherEvent(Facade.CMD_SERVER_GET_FILES_LIST).toServerCommand();
        eventToServer.setBody(getServerOutcommingPathContent());
        sendEventToClient(eventToServer);

    }

    private void sendEventToClient(PublisherEvent publisherEvent) {  //synchronyzed if no queue
        try {
            _outcomeServerEventsQueue.put(publisherEvent);
        } catch (InterruptedException e) {
            toLog(e.getMessage());
        }
    }

    private void initOutcomeServerEventsQueue() {
        Thread outcomeQueueThread = new Thread(() -> {
            OutputStream outputStream = Channels.newOutputStream(_serverSocketChanhel);
            try {
                _objectOutputStream = new ObjectOutputStream(outputStream);

                while (true) {
                    PublisherEvent publisherEvent = _outcomeServerEventsQueue.take();
                    _objectOutputStream.writeObject(publisherEvent);
                }

            } catch (InterruptedException | IOException e) {
                messageLog("Output stream break!");
            }

        });
        outcomeQueueThread.setName("outcomeServerEventsQueue");
        outcomeQueueThread.start();

    }

    private void initTransitionEventSender() {
        Thread transitionEventSenderThread = new Thread(() -> {
            while (true) {
                PublisherEvent outcomeTransitionEvent = Publisher.getInstance().getTransitionEvent();
                outcomeTransitionEvent.setServerCommand(Facade.CMD_SERVER_TRANSITION_EVENT);
                sendEventToClient(outcomeTransitionEvent);
                System.out.println("outcomeTransitionEvent send");

            }
        });
        transitionEventSenderThread.setName("transitionEventSenderThread");
        transitionEventSenderThread.start();

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

}
