package Transmitter.Logic.Workers;


public class FileStatisticsContext {

    private static int _totalDownloads = 0;

    private String _filename;
    private int _numberOfDownloads = 0;
    private boolean _isProcessed = false;


    public FileStatisticsContext(String filename) {
        _filename = filename;
    }

    public void incrementNumberOfDownloads() {
        _isProcessed = false;
        _numberOfDownloads++;
        _totalDownloads++;
    }

    public void processedChanges() {
        _isProcessed = true;
    }

    public String getFileName() {
        return _filename;
    }

    public int getNumberOfDownloads() {
        return _numberOfDownloads;
    }

    public int getTotalDownloads() {
        return _totalDownloads;
    }

    public boolean isProcessed() {
        return _isProcessed;
    }

}
