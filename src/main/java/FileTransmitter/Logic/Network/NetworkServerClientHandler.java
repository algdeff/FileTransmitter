package FileTransmitter.Logic.Network;

import FileTransmitter.Facade;
import FileTransmitter.Logic.ConfigManager;
import FileTransmitter.Logic.ThreadPoolManager;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NetworkServerClientHandler implements Runnable {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    private BlockingQueue<PublisherEvent> _outcomeServerEventsQueue;

    private AsynchronousSocketChannel _socketChanhel;
    private ObjectInputStream _objectInputStream;
    private ObjectOutputStream _objectOutputStream;

    public NetworkServerClientHandler(AsynchronousSocketChannel socketChanhel) {
        _socketChanhel = socketChanhel;
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
            clientAddress = _socketChanhel.getRemoteAddress();
            if (_socketChanhel.isOpen()) {
                messageLog("New client connected: " + clientAddress);

                InputStream inputStream = Channels.newInputStream(_socketChanhel);
                _objectInputStream = new ObjectInputStream(inputStream);

//                OutputStream outputStream = Channels.newOutputStream(_socketChanhel);
//                _objectOutputStream = new ObjectOutputStream(outputStream);
                outcomeServerEventsQueueInit();

                while (true) {
                    PublisherEvent eventFromClient = (PublisherEvent) _objectInputStream.readObject();

                    if (!eventFromClient.getType().equals(Facade.EVENT_TYPE_SERVERGROUP_CMD)) {
                        messageLog("WRONG EVENT TYPE!");
                        break;
                    }
                    String command = eventFromClient.getGroupName();

                    if (command.equals(Facade.CMD_SERVER_TERMINATE)) {
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
        String command = eventFromClient.getGroupName();

        switch (command) {
            case Facade.CMD_SERVER_ADD_FILES: {
//                System.err.println("Command: " + command);
                saveClientFileToReceivedFolder(eventFromClient);
                break;
            }
            case Facade.CMD_SERVER_GET_FILES: {
//                System.err.println("Command: " + command);
                sendServerFileToClient((String) eventFromClient.getBody());
                break;
            }
            case Facade.CMD_SERVER_GET_FILES_LIST: {
//                System.err.println("Command: " + command);
                sendServerFileListToClient();
                break;
            }
            case Facade.CMD_SERVER_ADD_FUTURE_TASK: {
                messageLog("Command: " + command);
                addFutureTaskToPool(eventFromClient);
                break;
            }


        }

    }

    private void addFutureTaskToPool(PublisherEvent eventFromClient) {
        Callable task = (Callable) eventFromClient.getBody();
        ThreadPoolManager.getInstance().executeFutureTask(task);
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

    private void outcomeServerEventsQueueInit() {
        Thread outcomeQueueThread = new Thread(() -> {
            OutputStream outputStream = Channels.newOutputStream(_socketChanhel);
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
