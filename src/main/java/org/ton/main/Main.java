package org.ton.main;

import com.google.common.net.InetAddresses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.actions.MyLocalTon;
import org.ton.settings.MyLocalTonSettings;
import org.ton.utils.MyLocalTonUtils;

import java.awt.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.nonNull;

@Slf4j
public class Main {

    public static final AtomicBoolean appActive = new AtomicBoolean(true);
    public static final AtomicBoolean inElections = new AtomicBoolean(false);
    //public static final AtomicBoolean parsingBlocks = new AtomicBoolean(false);
    public static final File file = new File(MyLocalTonSettings.LOCK_FILE);
    public static RandomAccessFile randomAccessFile;
    public static FileLock fileLock;

    public static void main(String[] args) throws Throwable {

        log.debug("myLocalTon lock file location: {}", MyLocalTonSettings.LOCK_FILE);

        if (!Arrays.asList(args).isEmpty()) {
            if (args[0].equalsIgnoreCase("restart")) {
                log.info("R E S E T T I N G: {}", Arrays.asList(args));
                Thread.sleep(1000);
                FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "MyLocalTonDB"));
                FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "genesis"));
                FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "templates"));
                FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "myLocalTon.log"));
            }
        }

        MyLocalTon myLocalTon = MyLocalTon.getInstance();
        myLocalTon.setSettings(MyLocalTonUtils.loadSettings());
        myLocalTon.saveSettingsToGson(); //create default config
        MyLocalTonSettings settings = myLocalTon.getSettings();
        log.info("myLocalTon config file location: {}", MyLocalTonSettings.SETTINGS_FILE);

        MyLocalTonUtils.setMyLocalTonLogLevel(settings.getGenesisNode().getMyLocalTonLogLevel());

        if (!Arrays.asList(args).isEmpty()) {
            for (String arg : args) {

                if (arg.equalsIgnoreCase("debug")) {
                    MyLocalTonUtils.setMyLocalTonLogLevel("DEBUG");
                    settings.getGenesisNode().setTonLogLevel("DEBUG");
                    log.info("running in debug mode");
                }

                if (arg.equalsIgnoreCase("nogui")) {
                    System.setProperty("java.awt.headless", "true");
                }

                if (InetAddresses.isInetAddress(arg)) {
                    log.info("listening on public IP " + arg);
                    settings.getGenesisNode().setPublicIp(arg);
                }
            }
        }

        if (GraphicsEnvironment.isHeadless()) {
            log.info("You are using headless version of Java. GUI will not be available.");
        }

        System.setProperty("objectdb.home", MyLocalTonSettings.DB_DIR);
        System.setProperty("objectdb.conf", MyLocalTonSettings.DB_SETTINGS_FILE);

        if (lockInstance()) {

            Thread.currentThread().setName("MyLocalTon - main");

            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                log.debug("UI Uncaught Exception in thread '{}': {}", t.getName(), e.getMessage());
                log.debug(ExceptionUtils.getStackTrace(e));
            });

            log.info("Starting application at path {}", MyLocalTonUtils.getMyPath());

            App.main(settings, myLocalTon);

        } else {
            log.error("Instance already running.");
            System.exit(1);
        }
    }

    private static boolean lockInstance() {
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileLock = randomAccessFile.getChannel().tryLock();
            if (nonNull(fileLock)) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.debug("Shutdown hook triggered...");
                    MyLocalTonUtils.doShutdown();
                }));
                return true;
            }
        } catch (Exception e) {
            log.error("Unable to create and/or lock file: {} , exception: {}", MyLocalTonSettings.LOCK_FILE, e.getMessage());
        }
        return false;
    }
}
