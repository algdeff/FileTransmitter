package FileTransmitter.Logic.Workers;

import FileTransmitter.Facade;
import FileTransmitter.Logic.ConfigManager;
import FileTransmitter.Publisher.PublisherEvent;

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
                System.err.println("New client connected: " + clientAddress);

                InputStream inputStream = Channels.newInputStream(_socketChanhel);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                OutputStream outputStream = Channels.newOutputStream(_socketChanhel);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

                while (true) {
                    PublisherEvent eventFromClient = (PublisherEvent) objectInputStream.readObject();

                    System.out.println(eventFromClient.getName() + "/"
                            + eventFromClient.getType() + "/"
                            + eventFromClient.getGroupName());


                    if (!eventFromClient.getType().equals(Facade.EVENT_TYPE_SERVERGROUP_CMD)) {
                        System.err.println("WRONG EVENT TYPE!");
                        break;
                    }
                    String command = eventFromClient.getGroupName();

                    if (command.equals(Facade.CMD_SERVER_TERMINATE)) {
                        System.err.println("CMD_SERVER_TERMINATE");
//                        objectOutputStream.writeObject("READ OK" + object.toString());
                        //clientSocket.close();
                        break;
                    }

                    parseCommandFromClient(eventFromClient);

//                    System.err.println("Received from client (" + clientAddress + "):\n" + object);
                    objectOutputStream.writeObject("ECHO: Received from client: " + eventFromClient.getGroupName());
                }

//                objectOutputStream.close();
//                objectInputStream.close();
//                inputStream.close();
//                outputStream.close();
//                clientSocket.close();
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

//        ByteBuffer bb = ByteBuffer.allocate(16364);
//        result.read(bb);
//        System.out.println(new String(bb.array()));
    }

    private void parseCommandFromClient(PublisherEvent eventFromClient) {
        String command = eventFromClient.getGroupName();

        switch (command) {
            case Facade.CMD_SERVER_ADD_FILES: {

                System.err.println("Command: " + command);
                break;
            }
            case Facade.CMD_SERVER_GET_FILES: {
                System.err.println("Command: " + command);
                break;
            }
            case Facade.CMD_SERVER_GET_FILES_LIST: {
                System.err.println("Command: " + command);


                break;
            }

        }

    }

    private List<Path> getServerOutcommingPathContent() {
        List<Path> fileList = new ArrayList<>();

        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(_outcomingPath, ConfigManager
                    .getOutcomingTypesGlob());
            for (Path file : directoryStream) {
//                if (!isCorrectFile(file)) continue;
                fileList.add(file);
//                ThreadPoolManager.getInstance().executeFutureTask(new FileProcessingThread(file));
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
