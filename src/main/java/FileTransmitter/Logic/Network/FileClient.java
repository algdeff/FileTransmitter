package FileTransmitter.Logic.Network;

import FileTransmitter.Logic.ConfigManager;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileClient {

    private static final String CMD_RECEIVE_FILE_FROM_CLIENT = "1";
    private static final String CMD_SEND_FILE_TO_CLIENT = "2";
    private static final String CMD_SEND_SERVER_FILELIST_TO_CLIENT = "3";
    private static final String CMD_RECEIVE_CLIENT_CHOISE = "4";
    private static final String CMD_CLOSE_CONNECTION = "0";

    private  Boolean isClosed = false;
    private  Socket sock1;
    private  Socket sock2;

    private  BufferedReader stdin;
    private  ObjectOutputStream cmd;

    public FileClient() {
    }

    public void start() {
        String hostName = ConfigManager.getRemoteServerURL();
        int portNumber = ConfigManager.getRemoteServerPort();

        try {
            sock1 = new Socket(hostName, portNumber);
            sock2 = new Socket(hostName, portNumber + 1);
            stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Client connected to server: " + hostName + " at port: " + portNumber);
        } catch (Exception e) {
            System.err.println("Cannot connect to the server: " + hostName + ":" + portNumber
                    + ", try again later.");
            System.exit(1);
        }

        OutputStream os = null;
        try {
            os = sock2.getOutputStream();
            cmd = new ObjectOutputStream(os);

            while (!isClosed) {
                switch (selectCommand()) {
                    case "1":
                        sendCommandToServer(CMD_RECEIVE_FILE_FROM_CLIENT);
                        chooseFileAndSend();
                        break;
                    case "2":
                        sendCommandToServer(CMD_SEND_SERVER_FILELIST_TO_CLIENT);
                        chooseFileAndReceive();
                        break;
                    case "3":
                        sendCommandToServer(CMD_SEND_SERVER_FILELIST_TO_CLIENT);
                        listServerFiles();
                        break;
                    case "4":
                        listClientFiles();
                        break;
                    case "0":
                        sendCommandToServer(CMD_CLOSE_CONNECTION);
                        isClosed = true;
                        break;
                }
            }

            cmd.close();
            sock1.close();
            sock2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }





    private String selectCommand() throws  IOException{
        System.out.println("-----------------------");
        System.out.println("1. Send file.");
        System.out.println("2. Recieve file.");
        System.out.println("3. List Server files.");
        System.out.println("4. List Client files.");
        System.out.println("0. Exit");
        System.out.println("-----------------------");
        System.out.print("\nMake selection: ");
        return stdin.readLine();
    }

    private void sendCommandToServer(String command) { ///////////1
        try {
            cmd.writeUTF(command);
            cmd.flush();
        } catch (Exception e) {
            System.err.println("Command to server not send!");
        }
    }

    private void chooseFileAndSend() {
        try {
            String fileName = "";
            ArrayList<File> clientFiles = getClientFileList(".");

            for (File fil: clientFiles) {
                System.out.println(clientFiles.indexOf(fil) + ". " + fil.getName() + " - " + fil.length() + " bytes");
            }
            System.err.print("Choose you file number: ");
            String fileNumber = stdin.readLine();
            for (File fil: clientFiles) {
                if (clientFiles.indexOf(fil) == Integer.parseInt(fileNumber)) {
                    fileName = fil.getName();
                    break;
                }
            }

            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            OutputStream os = sock1.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            dis.close();
            System.out.println("File "+fileName+" sent to Server.");
        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }

    private void receiveFileFromServer() {

        try {
            int bytesRead;
            InputStream in = sock1.getInputStream();
            DataInputStream clientData = new DataInputStream(in);
            Path fileName = Paths.get(ConfigManager.getReceivedPath().toString(), clientData.readUTF());
            OutputStream output = new FileOutputStream(fileName.toAbsolutePath().toString());

            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();

            System.out.println("File " + fileName + " received from Server.");
        } catch (IOException ex) {
            System.out.println("receiveFileFromServer error");
        }
    }

    private void chooseFileAndReceive() { ////////////////////////2
        try {
            ArrayList<String> serverFileList = getServerFileList();
            for (String fil: serverFileList) {
                System.out.println(serverFileList.indexOf(fil) + ". " + fil);
            }
            System.err.print("Choose you file number: ");
            String fileNumber = stdin.readLine();
            for (String fil: serverFileList) {
                if (serverFileList.indexOf(fil) == Integer.parseInt(fileNumber)) {
                    sendCommandToServer(CMD_RECEIVE_CLIENT_CHOISE);
                    sendClientChoice(fil);
                    receiveFileFromServer();
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }

    private void sendClientChoice(String choice) {
        try {
            OutputStream os = sock1.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeUTF(choice);
            oos.flush();
            System.out.println("Choice: '" + choice + "' sent to Server.");
        } catch (Exception e) {
            System.err.println("sendClientChoice error");
        }
    }

    private void listServerFiles() {
        for (String fil: getServerFileList()) {
            System.out.println(fil);
        }
    }

    private void listClientFiles() {
        for (File fil: getClientFileList(".")) {
            System.out.println(fil.getName() + " - " + fil.length() + " bytes");
        }
    }

    private ArrayList<String> getServerFileList() { //////////////3
        ArrayList<String> fileList = new ArrayList<>();
        try {
            InputStream in = sock1.getInputStream();
            ObjectInputStream serverData = new ObjectInputStream(in);
            fileList = (ArrayList<String>) serverData.readObject();
        } catch (Exception ex) {
            System.out.println("getServerFileList error");
        }
        return fileList;
    }

    private ArrayList<File> getClientFileList(String str) {
        ArrayList<File> fileList = new ArrayList<>();
        try {
            File fil = new File(str);
            for (File f: fil.listFiles()) {
                if (f.isFile()) {
                    fileList.add(f);
                } else if (f.isDirectory()) {
                    //listWithFileNames.add(f.getAbsolutePath());
                    //getListFiles(s.getAbsolutePath());
                }
            }
//            File myFolder = new File(".");
//            File[] fileList = myFolder.listFiles();
//            for (int i = 0; i < fileList.length; i++) {
//                System.out.println(fileList[i].toString());
//            }

        } catch (Exception e) {
            System.err.println("getClientFileList error");
        }
        return fileList;
    }

    public static void main(String[] args) {
        new FileClient().start();
    }

}