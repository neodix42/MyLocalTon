package org.ton.main;

import lombok.extern.slf4j.Slf4j;
import org.ton.settings.MyLocalTonSettings;
import org.ton.utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
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
        if (GraphicsEnvironment.isHeadless()) {

            log.error("You have headless version of Java installed. Please install full Java version.");
            System.exit(1);
        }
        log.info("myLocalTon lock file location: {}", MyLocalTonSettings.LOCK_FILE);

        if (lockInstance()) {

            Thread.currentThread().setName("MyLocalTon - main");

            log.info("Starting application at path {}", Utils.getMyPath());

            App.main(args);

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
                    Utils.doShutdown();
                }));
                return true;
            }
        } catch (Exception e) {
            log.error("Unable to create and/or lock file: {} , exception: {}", MyLocalTonSettings.LOCK_FILE, e.getMessage());
        }
        return false;
    }
}
