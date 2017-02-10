package Transmitter.Logic.Workers;

import Transmitter.Logic.ConfigManager;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class FileProcessingThread implements Callable {

    private Path _fileNameToSend;


    public FileProcessingThread(Path fileName) {
        _fileNameToSend = fileName;
    }

    @Override
    public ArrayList call() {
        return sendFileToRemoteServer();
    }

    private ArrayList sendFileToRemoteServer() {
        ArrayList<String> resultset = new ArrayList<>();

        System.out.println(_fileNameToSend + " SEND TO SERVER");

        if (!moveFile()) resultset.add("Error replace file: " + _fileNameToSend);

        return resultset;
    }

    private boolean moveFile() {

        Path target = Paths.get(ConfigManager.getSentPath().toString(), _fileNameToSend.getFileName().toString());

        try {
            Files.move(_fileNameToSend, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (FileSystemException fse) {
            System.err.println("File: " + _fileNameToSend.getFileName().
                    toString() + " is already used!");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return false;
    }

}

