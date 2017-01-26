package FileTransmitter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private static int DEFAULT_PORT = 1403;

    private static ServerSocket serverSocket1;
    private static ServerSocket serverSocket2;
    private static Socket clientSocket1 = null;
    private static Socket clientSocket2 = null;

    public static void main(String[] args) throws IOException {

        int portNumber = 0;
        if (args.length > 0) {
            portNumber = Integer.parseInt(args[0]);
        }
        if (portNumber < 1025) {
            portNumber = DEFAULT_PORT;
        }

        try {
            serverSocket1 = new ServerSocket(portNumber);
            serverSocket2 = new ServerSocket(portNumber + 1);

            System.out.println("Server started at port: " + portNumber);
        } catch (Exception e) {
            System.err.println("Port already in use.");
            System.exit(1);
        }

        while (true) {
            try {
                clientSocket1 = serverSocket1.accept();
                clientSocket2 = serverSocket2.accept();

                System.out.println("Accepted connection : " + clientSocket1 + " " + clientSocket2);

                Thread connectionThread = new Thread(new ClientThread(clientSocket1, clientSocket2));
                connectionThread.start();
            } catch (Exception e) {
                System.err.println("Error in connection attempt.");
            }
        }
    }
}