package FileTransmitter;

import FileTransmitter.Logic.*;
import FileTransmitter.Logic.Workers.LogFileWorker;
import FileTransmitter.Publisher.Publisher;

public class ServerStarter {

    private static String _serverName = "FileTransmitter";
    private static ServerStarter _instance;

    private ModeSelector _process;

    private ServerStarter() {
    }

    public static void main(String[] args) {

        //InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

        try {
            _instance = new ServerStarter();
            _instance.start();

        } catch (Throwable ex) {
            Publisher.getInstance().sendPublisherEvent(Facade.CMD_LOGGER_ADD_LOG,
                    "Failed start server:" + ex.getMessage());
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

        Facade.getInstance().init();
        ConfigManager.init();
        LogFileWorker.getInstance().init();
        ThreadPoolManager.getInstance()
                .init(ConfigManager.getThreadPoolSize());

        ServerTaskProducer serverTaskProducer = new ServerTaskProducer();
        serverTaskProducer.init();

        ModeSelector modeSelector = new ModeSelector();
        modeSelector.start();

        ClientTaskExecutor clientTaskExecutor = new ClientTaskExecutor();
        clientTaskExecutor.init();

    }

    private void stop() {

        Publisher.getInstance().sendPublisherEvent(Facade.SERVER_TERMINATE);
        System.err.println("Server STOP");
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
