package Transmitter.Logic;

import static Transmitter.Facade.*;

import Transmitter.Facade;
import Transmitter.Logic.Network.RemoteClient;
import Transmitter.Logic.Network.NetworkServer;
import Transmitter.Publisher.Publisher;
import Transmitter.ServerStarter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModeSelector {

    private Path _receivedPath;
    private Path _outcomingPath;
    private Path _sentPath;

    public ModeSelector() {
        _receivedPath = ConfigManager.getReceivedPath();
        _outcomingPath = ConfigManager.getOutcomingPath();
        _sentPath = ConfigManager.getSentPath();
    }

    public void start() {

        boolean isSelected = false;

        while (!isSelected) {
            messageLog(    "   You choice: \n" +
                           "1. Start CLIENT role\n" +
                           "2. Start SERVER role\n" +
                           "3. Terminate the program\n");
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String choice = "";
            try {
                choice = stdin.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            switch (choice) {
                case "1": {
                    isSelected = true;
                    startClientRole();
                    break;
                }
                case "2": {
                    isSelected = true;
                    startServerRole();
                    break;
                }
                case "3": {
                }

                ServerStarter.stopAndExit(0);
            }

        }

    }

    private void startClientRole() {
        messageLog("StartClientRole");

        Facade.setServerRole(false);
        prepareWorkFolders();

        RemoteClient remoteClient = new RemoteClient(
                ConfigManager.getRemoteServerURL(),
                ConfigManager.getRemoteServerPort());
        remoteClient.start();

    }

    private void startServerRole() {
        messageLog("StartServerRole");

        Facade.setServerRole(true);
        prepareWorkFolders();

        NetworkServer networkServer = new NetworkServer(
                ConfigManager.getRemoteServerPort());
        networkServer.start();

    }

    private void prepareWorkFolders() {

        try {
//            if (!_isServerRole) Files.createDirectories(_outcomingPath);
            Files.createDirectories(_outcomingPath);
            Files.createDirectories(_receivedPath);
            Files.createDirectories(_sentPath);
        } catch (FileAlreadyExistsException faee) {
            System.err.println("Please rename this files: "
                    + _receivedPath + ", " + _outcomingPath
                    + " or " + _sentPath);
            ServerStarter.stopAndExit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private void messageLog(String message) {
        Publisher.getInstance().sendPublisherEvent(CMD_LOGGER_ADD_LOG, message);
    }

}

