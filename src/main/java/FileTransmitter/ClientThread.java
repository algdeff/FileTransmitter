package FileTransmitter;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread implements Runnable {

    private static final String DEFAULT_FOLDER = "received";

    public static final String CMD_RECEIVE_FILE_FROM_CLIENT = "1";
    public static final String CMD_SEND_FILE_TO_CLIENT = "2";
    public static final String CMD_SEND_SERVER_FILELIST_TO_CLIENT = "3";
    public static final String CMD_RECEIVE_CLIENT_CHOISE = "4";
    public static final String CMD_CLOSE_CONNECTION = "0";

    private Boolean isClosed = false;
    private Socket clientSocket1;
    private Socket clientSocket2;

    public ClientThread(Socket sock1, Socket sock2) {
        this.clientSocket1 = sock1;
        this.clientSocket2 = sock2;
    }

    @Override
    public void run() {
        try {
            InputStream is = clientSocket2.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            String commandFromClient;
            while (!isClosed) {
                commandFromClient = ois.readUTF();
                //System.out.println("cmd:" + commandFromClient);
                switch (commandFromClient) {
                    case CMD_RECEIVE_FILE_FROM_CLIENT:
                        System.out.println("RECEIVE_FILE_FROM_CLIENT");
                        receiveFile();
                        break;
//                    case CMD_SEND_FILE_TO_CLIENT:
//                        System.out.println("SEND_FILE_TO_CLIENT-----------------------");
//                        //sendFileToClient();
//                        break;
                    case CMD_SEND_SERVER_FILELIST_TO_CLIENT:
                        System.out.println("SEND_SERVER_FILELIST_TO_CLIENT");
                        sendServerFileList();
                        break;
                    case CMD_RECEIVE_CLIENT_CHOISE:
                        System.out.println("RECEIVE_CLIENT_CHOICE");
                        receiveClientChoice();
                        break;
                    case CMD_CLOSE_CONNECTION:
                        isClosed = true;
                        System.out.println("CLOSE_CONNECTION");
                        break;
                    default:
                        System.out.println("Incorrect command received.");
                        break;
                }
                //break;
            }
            ois.close();
            clientSocket1.close();
            clientSocket2.close();

        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveClientChoice() {
        String clientChoice = getClientChoice();
        System.out.println("clientChoice: " + clientChoice);
        sendFile(clientChoice);
    }

    public void receiveFile() {
        try {
            int bytesRead;
            DataInputStream clientData = new DataInputStream(clientSocket1.getInputStream());
            String fileName = clientData.readUTF();

            File fil = new File(DEFAULT_FOLDER);
            Boolean mkDirResult = fil.mkdir();
            if (mkDirResult) {
                System.out.println("Folder '" + fil.getAbsolutePath() + "' is now created.");
            }

            OutputStream output = new FileOutputStream((DEFAULT_FOLDER + "\\" + fileName));
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            output.close();
            System.out.println("File " + fileName + " received from client.");
        } catch (IOException ex) {
            System.err.println("Client error. Connection closed.");
        }
    }

    public void sendFile(String fileName) {
        try {
            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            OutputStream os = clientSocket1.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            dis.close();
            System.out.println("File "+fileName+" sent to client.");
        } catch (Exception e) {
            System.err.println("File '" + fileName + "' does not exist!");
        }
    }

    public void sendServerFileList() {
        ArrayList<File> serverFilesList = getServerFileList(".");
        try {
            OutputStream os = clientSocket1.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);

            ArrayList<String> serverFilesInfo = new ArrayList<>();
            String fileInfo;
            for (File fil: serverFilesList) {
                fileInfo = fil.getName();// + " - " + fil.length() + " bytes";
                serverFilesInfo.add(fileInfo);
                System.out.println(fileInfo);
            }
            oos.writeObject(serverFilesInfo);
            oos.flush();
            System.out.println(serverFilesList.size()+" file names sent to Client.");

        } catch (Exception e) {
            System.err.println("sendServerFileList error");
        }
    }

    public ArrayList<File> getServerFileList(String str) {
        ArrayList<File> listWithFileNames = new ArrayList<>();
        try {
            File fil = new File(str);
            for (File f: fil.listFiles()) {
                if (f.isFile()) {
                    listWithFileNames.add(f);
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
            System.err.println("getServerFileList error");
        }

        return listWithFileNames;
    }

    public String getClientChoice() {
        String clientChoice = "";
        try {
            InputStream in = clientSocket1.getInputStream();
            ObjectInputStream clientData = new ObjectInputStream(in);
            clientChoice = clientData.readUTF();
        } catch (Exception ex) {
            System.out.println("getClientChoice error");
        }
        return clientChoice;
    }
}