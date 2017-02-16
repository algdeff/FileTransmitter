package Transmitter.Logic.Network;

import static Transmitter.Facade.*;
import Transmitter.Publisher.Publisher;
import Transmitter.ServerStarter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.BindException;
import java.net.InetSocketAddress;

import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkServer {

    public static final String CLIENT_ID_PREF = "FT_CID_";

    private AsynchronousChannelGroup _group;
    private int _counter;
    private int _portNumber;

    public NetworkServer(int portNumber) {
        _portNumber = portNumber;
    }


    public void start() {
        new Thread(new ServerInterface()).start();
        startServerEngine();
    }

    private void startServerEngine() {
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", _portNumber);

        try {
            _group = AsynchronousChannelGroup.withThreadPool(Executors.newWorkStealingPool(5)); //timeot ad ends this method
            _counter = 0;

            AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(_group).bind(hostAddress);
            messageLog("Server started on: " + listener.getLocalAddress());

            listener.accept(CLIENT_ID_PREF + _counter, new CompletionHandler<AsynchronousSocketChannel, String>() {
                @Override
                public void completed(AsynchronousSocketChannel channel, String clientID) {
                    try {
                        messageLog("Client connected: " + channel.getRemoteAddress() + " / " + clientID);
                        _counter++;
                        listener.accept(CLIENT_ID_PREF + _counter, this);

                        new Thread(new NetworkServerClientHandler(channel, clientID)).start();

                    } catch (IOException e) {
                        toLog(e.getMessage());
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
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_LOG, message);
    }

    private void toLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_RECORD, message);
    }


    private class ServerInterface implements Runnable {

        @Override
        public void run() {
            serverUI();
        }

        private void serverUI() {

            boolean isSelected = false;

            while (!isSelected) {
                messageLog(  "   You choice: \n" +
                             "1. Start ServerTaskProducer\n" +
                             "2. Terminate the program\n");

                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                String choice = "";
                try {
                    choice = stdin.readLine();
                } catch (IOException e) {
                    toLog(e.getMessage());
                }

                switch (choice) {
                    case "1": {
                        isSelected = true;
                        startServerTaskProducer();
                        break;
                    }
                    case "2": {
                    }
                    ServerStarter.stopAndExit(0);
                }

            }
            messageLog("/Server UI terminate/");

        }

        private void startServerTaskProducer() {
            messageLog("ServerTaskProducer start...");
            Publisher.getInstance().sendPublisherEvent(CMD_TASK_PRODUCER_START);
        }
    }

}
