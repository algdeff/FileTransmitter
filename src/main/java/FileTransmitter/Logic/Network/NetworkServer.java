package FileTransmitter.Logic.Network;

import FileTransmitter.Facade;
import FileTransmitter.Publisher.Publisher;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.concurrent.*;

public class NetworkServer {

    private AsynchronousChannelGroup _group;
    private int _counter;
    private int _portNumber;

    private boolean _isServerRole = false;

    public NetworkServer(int portNumber) {
        _portNumber = portNumber;
    }


    public void start() {
        startServerEngine();

    }

    private void startServerEngine() {
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", _portNumber);

        try {
            _group = AsynchronousChannelGroup.withThreadPool(Executors.newWorkStealingPool(5)); //timeot ad ends this method
            _counter = 0;

            AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(_group).bind(hostAddress);
            messageLog("Server started on: " + listener.getLocalAddress());

            listener.accept("youID=" + _counter, new CompletionHandler<AsynchronousSocketChannel, String>() {
                @Override
                public void completed(AsynchronousSocketChannel channel, String attachment) {
                    try {
                        messageLog("Client connected: " + channel.getRemoteAddress() + " / " + attachment);
                        _counter++;
                        listener.accept("youID=" + _counter, this);

                        //clientHandle(channel);
                        new Thread(new NetworkServerClientHandler(channel)).start();

                        //System.err.println("Client terminated: " + channel.toString());
                        //channel.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void failed(Throwable exc, String attachment) {
                    messageLog("Failed");
                }
            });
            _group.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            toLog(ie.getMessage());
        } catch (BindException be) {
            messageLog("Server already running on port: " + hostAddress.getPort());
        } catch (IOException e) {
            toLog(e.getMessage());
        }

    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_RECORD, message);
    }

}
