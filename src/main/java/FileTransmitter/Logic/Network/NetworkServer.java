package FileTransmitter.Logic.Network;

import FileTransmitter.Logic.ConfigManager;
import FileTransmitter.Logic.ThreadPoolManager;
import FileTransmitter.ServerStarter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NetworkServer {

    private  ServerSocket serverSocket1;
    private  ServerSocket serverSocket2;
    private  Socket clientSocket1 = null;
    private  Socket clientSocket2 = null;

    public void start() {

        int portNumber = ConfigManager.getRemoteServerPort();
        prepareWorkFolders();

        try {
            serverSocket1 = new ServerSocket(portNumber);
            serverSocket2 = new ServerSocket(portNumber + 1);

            System.out.println("Server started at port: " + portNumber);
        } catch (Exception e) {
            System.err.println("Port already in use.");
            ServerStarter.stopAndExit(1);
        }

        while (true) {
            try {
                clientSocket1 = serverSocket1.accept();
                clientSocket2 = serverSocket2.accept();

                System.out.println("Accepted connection : " + clientSocket1 + " " + clientSocket2);
                ThreadPoolManager.getInstance().execute(new ClientThread(clientSocket1, clientSocket2));

            } catch (Exception e) {
                System.err.println("Error in connection attempt.");
            }
        }
    }

    private void prepareWorkFolders() {

        Path receivePath = ConfigManager.getReceivedPath();

        try {
            Files.createDirectories(receivePath);
        } catch (FileAlreadyExistsException faee) {
            System.err.println("Please rename this file: " + receivePath);
            ServerStarter.stopAndExit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
}
