package FileTransmitter;

import FileTransmitter.Logic.ConfigManager;
//import FileTransmitter.Logic.Network.FileChunkReqWriteHandler;
//import FileTransmitter.Logic.Network.Server;
//import FileTransmitter.Logic.Network.chat.ContainerClient;
//import FileTransmitter.Logic.Network.chat.ContainerServer;
import FileTransmitter.Logic.ThreadPoolManager;
import FileTransmitter.Logic.FileSystemMonitor;
import FileTransmitter.Logic.Workers.LogFileWorker;
import FileTransmitter.Utils.ConsoleUtils;

public class ServerStarter {

    private static String _serverName = "XmlMonitorService";
    private static ServerStarter _instance;

    private FileSystemMonitor _fileSystemMonitor;
//    private ContainerClient _fileClient;
//    private ContainerServer _fileServer;

    private ServerStarter() {
    }

    public static void main(String[] args) {

        //InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

        try {
            _instance = new ServerStarter();
            _instance.start();

        } catch (Throwable ex) {
            //ConsoleUtils.tprintf("Failed start server: %s", ex.getMessage());
            ex.printStackTrace(System.err);
            stopServerInstance();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //_timer.cancel();
            stopServerInstance();
            //LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            //lc.stop();
        }));
    }


    private void start() {

        ConfigManager.init();
        ThreadPoolManager.getInstance()
                .init(ConfigManager.getThreadPoolSize());

//        _networkServer = new NetworkServer();
//        if (_networkServer.start()) {
//            ConsoleUtils.tprintf("File server is running.");
//        }

//        _fileServer = new FileServer();
//        if (_fileServer.start()) {
//            ConsoleUtils.tprintf("File server is running.");
//            //startPSTimer(config.getLong("perf", "log_period", 60000L));
//        } else {
//            ConsoleUtils.tprintf("Failed to start file server.");
//            stop();
//        }

//        _fileServer = new ContainerServer();
//        try {
//            _fileServer.start();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        String server = "192.168.1.4";
//        int port = 5252;
//        int containerPort = 8094;
//        _fileClient = new ContainerClient(server, port, containerPort);
//        _fileClient.start();


//        Server server = new Server();
//        try {
//            server.setFileReqHandler(new FileChunkReqWriteHandler());
//            server.setPackagePort(5252);
//            server.start();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        _fileSystemMonitor = new FileSystemMonitor();
        _fileSystemMonitor.start();

        LogFileWorker.getInstance().init();
    }

    private void stop() {

//        if (_networkServer != null) {
//            _networkServer.stop();
//            _networkServer = null;
//        }

//        if (_fileServer != null && _fileServer.isRunning()) {
//            _fileServer.stop();
//            _fileServer = null;
//        }

        System.err.println("Server STOP");
//        ThreadPoolManager.reset();
//        Facade.shutdown();
//        DatabaseManager.closeAll();
    }

    private static void stopServerInstance() {
        if (_instance != null) {
            final ServerStarter serverStarter = _instance;
            _instance = null;
            serverStarter.stop();
        }
    }

    public static void stopAndExit(int status) {
        stopServerInstance();
        System.exit(status);
    }

    public static String getServerName() {
        return _serverName;
    }
}
