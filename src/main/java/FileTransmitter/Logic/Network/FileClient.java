package FileTransmitter.Logic.Network;

import FileTransmitter.Facade;
import FileTransmitter.Logic.ConfigManager;
import FileTransmitter.Publisher.PublisherEvent;
import io.netty.channel.Channel;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.*;

public class FileClient {

    private String _remoteServerUrl;
    private int _remoteServerPort;

    private AsynchronousSocketChannel _clientSocketChannel;

    public FileClient(String remoteServerUrl, int remoteServerPort) {
        _remoteServerUrl = remoteServerUrl;
        _remoteServerPort = remoteServerPort;
        _clientSocketChannel = null;
    }

    public void start() {
        openConnectionToServer();

    }

    private void openConnectionToServer() {
        int threadsNumber = 10;
        ExecutorService executorService = Executors.newWorkStealingPool(threadsNumber); //ForkJoinPool.commonPool(); //Executors.newFixedThreadPool(50);

        try {
            InetSocketAddress  hostAddress = new InetSocketAddress(InetAddress.getByName(_remoteServerUrl),_remoteServerPort);
//            AsynchronousSocketChannel clientSocketChannel = AsynchronousSocketChannel.open(); //AsynchronousChannelGroup.withThreadPool(executorService));

            boolean isConnected = false;
            while (!isConnected) {
                _clientSocketChannel = AsynchronousSocketChannel.open();
                System.out.println(_clientSocketChannel.isOpen());

                try {
                    //connectFuture.get(); // Wait until connection is done.
                    _clientSocketChannel.connect(hostAddress).get();
                    isConnected = true;
                } catch (ExecutionException | InterruptedException ee) {
                    _clientSocketChannel.close(); ////////////////////////////////////////////////
                    System.err.println("Connecting to server: " + hostAddress + "......");
                }

                System.out.println(_clientSocketChannel.isOpen());

                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.err.println("Connected to server " + hostAddress);

            OutputStream outputStream = Channels.newOutputStream(_clientSocketChannel);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

            InputStream inputStream = Channels.newInputStream(_clientSocketChannel);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            new Thread(() -> {
                while (true) {
                    Object object = null;
                    try {
                        System.err.println("Wait ECHO FROM SERVER....");
                        object = objectInputStream.readObject();
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("(echo reader) Server breakdown!");
                        //e.printStackTrace();
                        break;
                    }
                    System.err.println("READ ECHO: " + object.toString());
                }
                System.err.println("echo thread close");
            }).start();


            PublisherEvent eventToServer = new PublisherEvent(Facade.CMD_SERVER_GET_FILES_LIST).toServerCommand();
            System.out.println(eventToServer.getName()
                    + eventToServer.getType()
                    + eventToServer.getGroupName());

            objectOutputStream.writeObject(eventToServer);

            //objectOutputStream.writeObject("EOF");

//            clientSocketChannel.shutdownInput();
//            clientSocketChannel.shutdownOutput();
//            objectInputStream.close();
//            objectOutputStream.close();
            //clientSocketChannel.close();
        } catch (IOException e) {
            System.err.println("Server breakdown!");
        }

    }

}