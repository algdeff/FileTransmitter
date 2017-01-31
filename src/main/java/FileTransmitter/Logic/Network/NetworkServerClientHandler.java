package FileTransmitter.Logic.Network;

import FileTransmitter.Facade;
import FileTransmitter.Logic.ConfigManager;
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

public class NetworkServerClientHandler implements Runnable {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    private AsynchronousSocketChannel _socketChanhel;
    private ObjectInputStream _objectInputStream;
    private ObjectOutputStream _objectOutputStream;

    public NetworkServerClientHandler(AsynchronousSocketChannel socketChanhel) {
        _socketChanhel = socketChanhel;
        _receivedPath = ConfigManager.getReceivedPath();
        _outcomingPath = ConfigManager.getOutcomingPath();
        _sentPath = ConfigManager.getSentPath();
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
                Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                        "New client connected: " + clientAddress);

                InputStream inputStream = Channels.newInputStream(_socketChanhel);
                _objectInputStream = new ObjectInputStream(inputStream);

                OutputStream outputStream = Channels.newOutputStream(_socketChanhel);
                _objectOutputStream = new ObjectOutputStream(outputStream);

                while (true) {
                    PublisherEvent eventFromClient = (PublisherEvent) _objectInputStream.readObject();

                    if (!eventFromClient.getType().equals(Facade.EVENT_TYPE_SERVERGROUP_CMD)) {
                        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                                "WRONG EVENT TYPE!");
                        break;
                    }
                    String command = eventFromClient.getGroupName();

                    if (command.equals(Facade.CMD_SERVER_TERMINATE)) {
                        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                                "CMD_SERVER_TERMINATE");
                        //clientSocket.close();
                        break;
                    }
                    parseCommandFromClient(eventFromClient);
                }

//                clientSocket.shutdownInput();
//                clientSocket.shutdownOutput();
                System.err.println("Client (" + clientAddress + ") successfully terminated");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("ClassNF Ex");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Client ("+ clientAddress +") is breakdown!");
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

        }

    }

    private void saveClientFileToReceivedFolder(PublisherEvent eventFromClient) {
        Path filename = Paths.get(_receivedPath.toString(), (String) eventFromClient.getArgs()[0]);
        System.out.println(filename.toString() + " @@@ " + (long) eventFromClient.getArgs()[1]);

        byte[] fileContent = (byte[]) eventFromClient.getBody();

//        System.out.println("+++" + fileContent.length);

        try {
            Files.write(filename, fileContent, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
            ServerStarter.stopAndExit(1);
        }

    }

    private void sendServerFileToClient(String filename) {
        Path fileToSend = Paths.get(filename).normalize();

        byte[] fileContent = new byte[0];
        try {
            fileContent = Files.readAllBytes(fileToSend);
        } catch (IOException e) {
            e.printStackTrace();
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

    private void sendEventToClient(PublisherEvent publisherEvent) {
        try {
            _objectOutputStream.writeObject(publisherEvent);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        } catch (IOException ioe) {
            System.err.println("directoryWalking: ioe");
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
}
