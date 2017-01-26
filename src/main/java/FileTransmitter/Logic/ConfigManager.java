package FileTransmitter.Logic;

import FileTransmitter.ServerStarter;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigManager {

    /**
     ConfigManager

     Property keys from config file
     default: filetransmitter.conf.xml
     */

    private static final String CONFIG_FILE_PATH =              "filetransmitter.conf.xml";


    private static final String RECEIVED_PATH =                 "received_path";
    private static final String OUTCOMING_PATH =                "outcoming_path";
    private static final String SENT_PATH =                     "sent_path";
    private static final String LOG_FILE_PATH =                 "log_file_path_name";
    private static final String THREAD_POOL_SIZE =              "thread_pool_size";
    private static final String OUTCOMING_TYPES_GLOB =          "outcoming_file_type_glob";

    private static final ConcurrentHashMap<String, String> _properties;

    static {
        Configurations configs = new Configurations();
        _properties = new ConcurrentHashMap<>();
        try {
            FileBasedConfigurationBuilder<XMLConfiguration> builder = configs.xmlBuilder(CONFIG_FILE_PATH);
            XMLConfiguration config = builder.getConfiguration();
            Iterator<String> iterator = config.getKeys();
            while (iterator.hasNext()) {
                String propertyName = iterator.next();
                System.err.println(propertyName + ": " + config.getString(propertyName));
                _properties.put(propertyName, config.getString(propertyName));
            }

        } catch (Exception cex) {
            System.err.println("Correct config file not found: " + CONFIG_FILE_PATH);
            ServerStarter.stopAndExit(1);
        }
    }

    private ConfigManager() {
    }

    public static void init() {
    }

    public static Path getReceivedPath() {
        return Paths.get(_properties.get(RECEIVED_PATH));
    }

    public static Path getOutcomingPath() {
        return Paths.get(_properties.get(OUTCOMING_PATH));
    }

    public static Path getSentPath() {
        return Paths.get(_properties.get(SENT_PATH));
    }

    public static Path getLogFilePath() {
        return Paths.get(_properties.get(LOG_FILE_PATH));
    }

    public static int getThreadPoolSize() {
        return Integer.parseInt(_properties.get(THREAD_POOL_SIZE));
    }

    public static String getOutcomingTypesGlob() {
        return _properties.get(OUTCOMING_TYPES_GLOB);
    }

}