package org.ton.mylocalton.main;

import static java.util.Objects.nonNull;

import com.google.common.net.InetAddresses;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.settings.MyLocalTonSettings;
import org.ton.mylocalton.utils.MyLocalTonUtils;

@Slf4j
public class Main {

  public static final AtomicBoolean appActive = new AtomicBoolean(true);
  public static final AtomicBoolean inElections = new AtomicBoolean(false);
  public static final File file = new File(MyLocalTonSettings.LOCK_FILE);
  public static RandomAccessFile randomAccessFile;
  public static FileLock fileLock;

  public static void main(String[] args) throws Throwable {

    for (String arg : args) {
      if (arg.equalsIgnoreCase("version")) {
        System.out.println("v1.29");
        System.exit(0);
      }
    }

    log.debug("myLocalTon lock file location: {}", MyLocalTonSettings.LOCK_FILE);

    if (Arrays.asList(args).contains("restart")) {
      log.info("RESETTING application data due to 'restart' arg");
      FileUtils.deleteQuietly(
          new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "MyLocalTonDB"));
      FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "genesis"));
      FileUtils.deleteQuietly(
          new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "templates"));
      FileUtils.deleteQuietly(
          new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "myLocalTon.log"));
    }

    MyLocalTon myLocalTon = MyLocalTon.getInstance();
    myLocalTon.setSettings(MyLocalTonUtils.loadSettings());
    myLocalTon.saveSettingsToGson(); // create default config
    MyLocalTonSettings settings = myLocalTon.getSettings();
    log.info("myLocalTon config file location: {}", MyLocalTonSettings.SETTINGS_FILE);

    MyLocalTonUtils.setMyLocalTonLogLevel(settings.getGenesisNode().getMyLocalTonLogLevel());

    for (String arg : args) {
      if (arg.equalsIgnoreCase("debug")) {
        MyLocalTonUtils.setMyLocalTonLogLevel("DEBUG");
        settings.getGenesisNode().setTonLogLevel("DEBUG");
        settings.getUiSettings().setEnableDebugMode(true);
        log.info("running in debug mode");
      }

      if (arg.equalsIgnoreCase("nogui")) {
        settings.getUiSettings().setEnableNoGuiMode(true);
        System.setProperty("java.awt.headless", "true");
        log.info("running in nogui mode");
      }

      if (arg.startsWith("custom-binaries")) {
        String path = StringUtils.remove(arg, "custom-binaries=");
        settings.setCustomTonBinariesPath(path);
        log.info("using custom TON binaries at path {}", path);
      }

      if (arg.equalsIgnoreCase("ton-http-api")) {
        log.info("enabling ton-http-api on start (default port 8081)");
        settings.getUiSettings().setEnableTonHttpApi(true);
      }

      if (arg.equalsIgnoreCase("explorer")) {
        log.info("enabling ton-blockchain explorer on start (default port 8000)");
        settings.getUiSettings().setEnableBlockchainExplorer(true);
      }

      if (arg.equalsIgnoreCase("data-generator")) {
        log.info("enabling data-generator on start");
        settings.getUiSettings().setEnableDataGenerator(true);
      }

      if (InetAddresses.isInetAddress(arg)) {
        log.info("listening on public IP " + arg);
        settings.getGenesisNode().setPublicIp(arg);
      }
    }

    if (settings.getUiSettings().isEnableDebugMode()) {
      MyLocalTonUtils.setMyLocalTonLogLevel("DEBUG");
      settings.getGenesisNode().setTonLogLevel("DEBUG");
    }

    if (settings.getUiSettings().isEnableNoGuiMode()) {
      System.setProperty("java.awt.headless", "true");
    }

    if (GraphicsEnvironment.isHeadless()) {
      log.info("You are using headless version of Java. GUI will not be available.");
    }

    System.setProperty("objectdb.home", MyLocalTonSettings.DB_DIR);
    System.setProperty("objectdb.conf", MyLocalTonSettings.DB_SETTINGS_FILE);

    if (lockInstance(args)) {

      Thread.currentThread().setName("MyLocalTon - main");

      Thread.setDefaultUncaughtExceptionHandler(
          (t, e) -> {
            log.debug("UI Uncaught Exception in thread '{}': {}", t.getName(), e.getMessage());
            log.debug(ExceptionUtils.getStackTrace(e));
          });

      log.info("Starting application at path {}", MyLocalTonUtils.getMyPath());
      App.main(settings, myLocalTon, args);

    } else {
      log.error("Instance already running.");
      System.exit(1);
    }
  }

  private static Scene getScene() {
    return App.scene;
  }

  private static boolean lockInstance(String[] args) {
    try {
      randomAccessFile = new RandomAccessFile(file, "rw");
      fileLock = randomAccessFile.getChannel().tryLock();
      if (nonNull(fileLock)) {
        boolean notTesting =
            Arrays.stream(args).noneMatch(arg -> arg.equalsIgnoreCase("test-binaries"));

        if (notTesting) {
          Runtime.getRuntime()
              .addShutdownHook(
                  new Thread(
                      () -> {
                        log.debug("Shutdown hook triggered...");

                        if (nonNull(MyLocalTon.getInstance().getMonitorExecutorService())) {
                          MyLocalTon.getInstance().getMonitorExecutorService().shutdownNow();
                        }

                        if (!GraphicsEnvironment.isHeadless()) {
                          App.mainController.saveSettings();
                          App.mainController.showShutdownMsg("Shutting down TON blockchain...", 5);
                        }

                        MyLocalTonUtils.doShutdown();
                      }));
        }
        return true;
      }
    } catch (Exception e) {
      log.error(
          "Unable to create and/or lock file: {} , exception: {}",
          MyLocalTonSettings.LOCK_FILE,
          e.getMessage());
    }
    return false;
  }
}
