package Transmitter.Logic.Network;

import java.io.Serializable;

import java.nio.file.Path;

import java.text.DecimalFormat;

public class FileContext implements Serializable {

    private String _fileFullPath, _fileName;
    private long _fileSize;
    private byte[] _fileContent;
    private int _fileIndex;

    public FileContext(Path filename) {
        _fileFullPath = filename.toString();
        _fileName = filename.getFileName().toString();
        _fileSize = 0L;
        _fileIndex = -1;
    }

    public String getFileName() {
        return _fileName;
    }

    public String getFileFullPath() {
        return _fileFullPath;
    }

    public void setFileSize(long fileSize) {
        _fileSize = fileSize;
    }
    public long getFileSize() {
        return _fileSize;
    }

    public String getFormattedSizeKb() {
        DecimalFormat formatter = new DecimalFormat("###,###"); //for decimal separator x.xx ("###,###.##");
        float sizeKb = _fileSize / 1024F;
        return formatter.format(sizeKb);
    }


    public void setFileIndex(int fileIndex) {
        _fileIndex = fileIndex;
    }
    public int getFileIndex() {
        return _fileIndex;
    }

    public void setFileContent(byte[] fileContent) {
        _fileContent = fileContent;
    }
    public byte[] getFileContent() {
        return _fileContent;
    }

}
