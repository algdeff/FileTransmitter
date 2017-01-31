package FileTransmitter.Logic.Network;

import FileTransmitter.Facade;
import FileTransmitter.Logic.ConfigManager;
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
                    Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                            "Connecting to server: " + hostAddress + "......");
                    try {
                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                    "Connected to server " + hostAddress);

            OutputStream outputStream = Channels.newOutputStream(_clientSocketChannel);
            _objectOutputStream = new ObjectOutputStream(outputStream);

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
            Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                    "Server breakdown!");
        }
    }

    private class ClientInterface implements  Runnable {

        @Override
        public void run() {
            clientCommandListener();
        }

        private void clientCommandListener() {
            while (true) {

                Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                          "   You choice: \n" +
                                "1. Send file to server\n" +
                                "2. Receive file from server\n" +
                                "3. List server files\n" +
                                "4. List client files to send\n" +
                                "5. Terminate the program\n");
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                String choice = "";
                try {
                    choice = stdin.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
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
                    }
                    ServerStarter.stopAndExit(0);
                }

            }

        }

        private void sendFileToServer() {
            Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                    "[SendFileToServer]");
            for (String file : getClientOutcommingPathContent()) {
                int index = _clientFileListCache.indexOf(file);
                Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                        String.format("%1$s: %2$s", index, file));
            }

            Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                    "Select file index to send it to the server: ");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String choice = "";
            try {
                choice = stdin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int selectedIndex = Integer.parseInt(choice);

            Path fileToSend = Paths.get(_clientFileListCache.get(selectedIndex));

            byte[] fileContent = new byte[0];
            try {
                fileContent = Files.readAllBytes(fileToSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
            long fileSize = fileContent.length;

//            System.out.println(fileToSend.toString() + " " + fileSize);

            PublisherEvent eventToServer = new PublisherEvent(Facade.CMD_SERVER_ADD_FILES, fileContent).toServerCommand();
            eventToServer.setArgs(fileToSend.getFileName().toString(), fileSize);
//            System.out.println(eventToServer.getName());
            sendEventToServer(eventToServer);


        }

        private void receiveFileFromServer() {
            Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                    "[ReceiveFileFromServer]");
            if (_serverFileListCache.size() == 0) return;

            for (String file : _serverFileListCache) {
                int index = _serverFileListCache.indexOf(file);
                System.err.println(String.format("%1$s: %2$s", index, file));
            }

            System.out.println("Select index of server file to receive it: ");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String choice = "";
            try {
                choice = stdin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            int selectedIndex = Integer.parseInt(choice);

            PublisherEvent eventToServer = new PublisherEvent(Facade.CMD_SERVER_GET_FILES,
                    _serverFileListCache.get(selectedIndex)).toServerCommand();
            System.out.println(eventToServer.getName());
            sendEventToServer(eventToServer);

        }

        private void listServerFiles() {
            Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                    "[ListServerFiles]");
            PublisherEvent eventToServer = new PublisherEvent(Facade.CMD_SERVER_GET_FILES_LIST).toServerCommand();
//            System.out.println(eventToServer.getName());
            sendEventToServer(eventToServer);

        }

        private void printListClientFilesToSend() {
            Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                    "[ListClientFilesToSend]");
            List<String> fileList = getClientOutcommingPathContent();
            for (String file : fileList) {
                int index = fileList.indexOf(file);
                Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                        String.format("%1$s: %2$s", index, file));
            }
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

            } catch (IOException ioe) {
                System.err.println("directoryWalking: ioe");
            }

            _clientFileListCache.clear();
            _clientFileListCache.addAll(fileList);

            return fileList;
        }

        private void sendEventToServer(PublisherEvent eventToServer) {

            try {
                _objectOutputStream.writeObject(eventToServer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private class ServerEventMonitor implements Runnable {

        @Override
        public void run() {
            serverCommandListener();
        }

        private void serverCommandListener() {
            while (true) {
                try {
                    PublisherEvent eventFromServer = (PublisherEvent) _objectInputStream.readObject();

                    if (!eventFromServer.getType().equals(Facade.EVENT_TYPE_SERVERGROUP_CMD)) {
                        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                                "WRONG EVENT TYPE!");
                        break;
                    }
                    String command = eventFromServer.getGroupName();

                    if (command.equals(Facade.CMD_SERVER_TERMINATE)) {
                        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                                "CMD_SERVER_TERMINATE");
                        //clientSocket.close();
                        break;
                    }
                    parseCommandFromServer(eventFromServer);

                } catch (IOException e) {

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
//            System.err.println("echo thread close");
        }

        private void parseCommandFromServer(PublisherEvent eventFromServer) {
            String command = eventFromServer.getGroupName();

            switch (command) {
                case Facade.CMD_SERVER_ADD_FILES: {

//                    System.err.println("Command: " + command);
                    break;
                }
                case Facade.CMD_SERVER_GET_FILES: {
//                    System.err.println("Command: " + command);
                    saveServerFileToReceivedFolder(eventFromServer);
                    break;
                }
                case Facade.CMD_SERVER_GET_FILES_LIST: {
//                    System.err.println("Command: " + command);
                    _serverFileListCache.clear();
                    _serverFileListCache.addAll((List<String>) eventFromServer.getBody());
                    printListFilesFromServer();
                    break;
                }

            }

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
                Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                        String.format("%1$s: %2$s", index, file));
            }
        }

    }

}