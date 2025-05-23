package org.ton.mylocalton.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.settings.MyLocalTonSettings;

@Slf4j
public class Extractor {
  public static final String ZEROSTATE = "zerostate";
  public static final String SMARTCONT = "smartcont";
  public static final String BIN = "bin";
  public static final String TEMPLATES = "templates";
  public static final String TONLIB_KEYSTORE = "tonlib-keystore";
  public static final String UTILS = "utils";
  public static final String DB = "db";
  public static final String WINDOWS_ZIP = "ton-win-x86_64.zip";
  private static final String CURRENT_DIR = System.getProperty("user.dir");
  public static final String MY_LOCAL_TON_ROOT_DIR =
      CURRENT_DIR + File.separator + "myLocalTon" + File.separator;
  private final String nodeName;

  public Extractor(String pNodeName) throws IOException {
    log.info("Working Directory = " + CURRENT_DIR);
    nodeName = pNodeName;
    extractBinaries();
  }

  private void extractBinaries() throws IOException {
    synchronized (Extractor.class) {
      if (Files.notExists(
          Paths.get(
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "smartcont"
                  + File.separator
                  + "auto"
                  + File.separator
                  + "config-code.fif"),
          LinkOption.NOFOLLOW_LINKS)) {

        log.info("Detected OS: {}", System.getProperty("os.name"));

        Files.createDirectories(Paths.get(MyLocalTonSettings.DB_DIR));
        Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES));
        Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS));
        Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB));
        Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN));
        Files.createDirectories(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + "tonlib-keystore"));
        Files.createDirectories(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + "wallets"));
        Files.createDirectories(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + "zerostate"));
        Files.createDirectories(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + "certs"));
        Files.createDirectories(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + DB
                    + File.separator
                    + "import"));
        Files.createDirectories(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + DB
                    + File.separator
                    + "static"));
        Files.createDirectories(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + DB
                    + File.separator
                    + "keyring"));
        Files.createDirectories(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "log"));
        //                if
        // (nonNull(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath())) {
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "tonlib-keystore"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "wallets"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "zerostate"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "certs"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "smartcont" + File.separator + "validator-keys-1.pub"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "smartcont" + File.separator + "validator-keys-2.pub"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "smartcont" + File.separator + "validator-keys-3.pub"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "smartcont" + File.separator + "validator-keys-4.pub"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "smartcont" + File.separator + "validator-keys-5.pub"));
        //                    FileUtils.deleteQuietly(new
        // File(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator +
        // "smartcont" + File.separator + "validator-keys-6.pub"));
        //
        //
        // Files.createDirectories(Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()+ File.separator + "tonlib-keystore"));
        //
        // Files.createDirectories(Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator + "wallets"));
        //
        // Files.createDirectories(Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()+ File.separator + "zerostate"));
        //
        // Files.createDirectories(Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()+ File.separator + "certs"));
        //                }

        if (SystemUtils.IS_OS_WINDOWS) {
          extractWindowsBinaries();
        } else if (SystemUtils.IS_OS_LINUX) {
          if (MyLocalTonUtils.isArm64()) {
            extractUbuntuBinaries("ton-linux-arm64");
          } else {
            extractUbuntuBinaries("ton-linux-x86_64");
          }
        } else if (SystemUtils.IS_OS_MAC) {
          if (MyLocalTonUtils.isMacOsArm()) {
            extractMacBinaries("ton-mac-arm64.zip");
          } else {
            extractMacBinaries("ton-mac-x86_64.zip");
          }
        } else {
          log.error(
              "You are running neither on Windows nor Linux nor MacOS. We don't have TON binaries compiled for it.");
          System.exit(0); // initiating shutdown hook
        }

        // extract other cross-platform files
        extractFile(
            "org/ton/mylocalton/binaries/global.config.json",
            MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "global.config.json");

        extractFile(
            "org/ton/mylocalton/binaries/control.template",
            MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "control.template");

        extractFile(
            "org/ton/mylocalton/binaries/ton-private-testnet.config.json.template",
            MY_LOCAL_TON_ROOT_DIR
                + TEMPLATES
                + File.separator
                + "ton-private-testnet.config.json.template");

        extractFile(
            "org/ton/mylocalton/binaries/example.config.json",
            MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "example.config.json");

        extractFile(
            "org/ton/mylocalton/db/objectsdb.conf",
            MyLocalTonSettings.DB_DIR + File.separator + "objectsdb.conf");

        extractFile(
            "org/ton/mylocalton/binaries/patches/show-addr.fif",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + SMARTCONT
                + File.separator
                + "show-addr.fif");

        extractFile(
            "org/ton/mylocalton/binaries/patches/gen-zerostate.fif",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + SMARTCONT
                + File.separator
                + "gen-zerostate.fif");

        extractFile(
            "org/ton/mylocalton/binaries/patches/faucet.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "faucet.pk");
        extractFile(
            "org/ton/mylocalton/binaries/patches/faucet-highload.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "faucet-highload.pk");
        extractFile(
            "org/ton/mylocalton/binaries/patches/data-highload.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "data-highload.pk");
        extractFile(
            "org/ton/mylocalton/binaries/patches/validator.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "validator.pk");
        extractFile(
            "org/ton/mylocalton/binaries/patches/validator-1.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "validator-1.pk");
        extractFile(
            "org/ton/mylocalton/binaries/patches/validator-2.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "validator-2.pk");
        extractFile(
            "org/ton/mylocalton/binaries/patches/validator-3.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "validator-3.pk");
        extractFile(
            "org/ton/mylocalton/binaries/patches/validator-4.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "validator-4.pk");
        extractFile(
            "org/ton/mylocalton/binaries/patches/validator-5.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "validator-5.pk");

        extractFile(
            "org/ton/mylocalton/binaries/patches/validator-6.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "validator-6.pk");

        extractFile(
            "org/ton/mylocalton/binaries/patches/validator-7.pk",
            MY_LOCAL_TON_ROOT_DIR
                + nodeName
                + File.separator
                + BIN
                + File.separator
                + ZEROSTATE
                + File.separator
                + "validator-7.pk");

        Files.copy(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + SMARTCONT
                    + File.separator
                    + "auto"
                    + File.separator
                    + "highload-wallet-v2-code.fif"),
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + SMARTCONT
                    + File.separator
                    + "highload-wallet-v2-code.fif"),
            StandardCopyOption.COPY_ATTRIBUTES);

      } else {
        log.info("Binaries already extracted.");
      }
    }
  }

  private void extractFile(String name, String MY_LOCAL_TON_ROOT_DIR) throws IOException {
    InputStream file = Extractor.class.getClassLoader().getResourceAsStream(name);
    assert file != null;
    Files.copy(file, Paths.get(MY_LOCAL_TON_ROOT_DIR), StandardCopyOption.REPLACE_EXISTING);
    file.close();
    log.debug("extracted file: {}", name);
  }

  private void extractWindowsUtils() {
    try {
      if (!Files.exists(
          Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "du.exe"),
          LinkOption.NOFOLLOW_LINKS)) {
        InputStream sendSignalCtrlC64 =
            Extractor.class
                .getClassLoader()
                .getResourceAsStream("org/ton/mylocalton/binaries/utils/SendSignalCtrlC64.exe");
        Files.copy(
            sendSignalCtrlC64,
            Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "SendSignalCtrlC64.exe"),
            StandardCopyOption.REPLACE_EXISTING);
        sendSignalCtrlC64.close();

        InputStream du =
            Extractor.class
                .getClassLoader()
                .getResourceAsStream("org/ton/mylocalton/binaries/utils/du.exe");
        Files.copy(
            du,
            Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "du.exe"),
            StandardCopyOption.REPLACE_EXISTING);
        du.close();

        InputStream cygWinDll =
            Extractor.class
                .getClassLoader()
                .getResourceAsStream("org/ton/mylocalton/binaries/utils/cygwin1.dll");
        Files.copy(
            cygWinDll,
            Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygwin1.dll"),
            StandardCopyOption.REPLACE_EXISTING);
        cygWinDll.close();

        InputStream cygIntlDll =
            Extractor.class
                .getClassLoader()
                .getResourceAsStream("org/ton/mylocalton/binaries/utils/cygintl-8.dll");
        Files.copy(
            cygIntlDll,
            Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygintl-8.dll"),
            StandardCopyOption.REPLACE_EXISTING);
        cygIntlDll.close();

        InputStream cygIconDll =
            Extractor.class
                .getClassLoader()
                .getResourceAsStream("org/ton/mylocalton/binaries/utils/cygiconv-2.dll");
        Files.copy(
            cygIconDll,
            Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygiconv-2.dll"),
            StandardCopyOption.REPLACE_EXISTING);
        cygIconDll.close();
      }
    } catch (Exception e) {
      log.error("Error extracting windows utils. The file might be in use.");
    }
  }

  void copyFolder(Path src, Path dest) {
    try {
      Files.walk(src)
          .forEach(
              s -> {
                try {
                  Path d = dest.resolve(src.relativize(s));
                  if (Files.isDirectory(s)) {
                    if (!Files.exists(d)) Files.createDirectory(d);
                    return;
                  }
                  Files.copy(s, d); // use flag to override existing
                } catch (Exception e) {
                  e.printStackTrace();
                }
              });
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void extractWindowsBinaries() {
    try {
      if (nonNull(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath())) {
        log.info(
            "copying binaries from "
                + MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()
                + " on windows");
        copyFolder(
            Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()),
            Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN));
      } else {
        log.info("extracting " + WINDOWS_ZIP + " on windows");

        InputStream binaries =
            Extractor.class
                .getClassLoader()
                .getResourceAsStream("org/ton/mylocalton/binaries/" + WINDOWS_ZIP);
        if (isNull(binaries)) {
          log.error("MyLocalTon executable does not contain resource " + WINDOWS_ZIP);
          System.exit(1);
        }

        Files.copy(
            binaries,
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + WINDOWS_ZIP),
            StandardCopyOption.REPLACE_EXISTING);
        binaries.close();
        ZipFile zipFile =
            new ZipFile(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + WINDOWS_ZIP);
        zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
        Files.delete(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + WINDOWS_ZIP));
      }
      log.debug(
          "windows binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);

      extractWindowsUtils();

    } catch (Throwable e) {
      log.error("Cannot extract TON binaries. Error {} ", ExceptionUtils.getStackTrace(e));
      System.exit(44);
    }
  }

  private void extractUbuntuBinaries(String platform) {
    try {
      if (nonNull(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath())) {
        log.info(
            "copying binaries from "
                + MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()
                + " on ubuntu");
        copyFolder(
            Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()),
            Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN));
      } else {
        log.info("extracting " + platform + " on linux");
        InputStream binaries =
            Extractor.class
                .getClassLoader()
                .getResourceAsStream("org/ton/mylocalton/binaries/" + platform + ".zip");
        Files.copy(
            binaries,
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + platform
                    + ".zip"),
            StandardCopyOption.REPLACE_EXISTING);
        binaries.close();
        ZipFile zipFile =
            new ZipFile(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + platform
                    + ".zip");
        zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
        Files.delete(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + platform
                    + ".zip"));
      }
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "create-hardfork")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "create-state")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "dht-server")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "fift")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "func")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "generate-random-id")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "lite-client")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "validator-engine")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "validator-engine-console")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "blockchain-explorer")
          .start();

      log.debug(
          "ubuntu binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);

    } catch (Throwable e) {
      log.error("Cannot extract TON binaries. Error {} ", ExceptionUtils.getStackTrace(e));
      System.exit(44);
    }
  }

  private void extractMacBinaries(String platform) {
    try {
      if (nonNull(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath())) {
        log.info(
            "copying binaries from "
                + MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()
                + " on mac");
        copyFolder(
            Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()),
            Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN));
      } else {
        log.info("extracting " + platform + " on macos");
        InputStream binaries =
            Extractor.class
                .getClassLoader()
                .getResourceAsStream("org/ton/mylocalton/binaries/" + platform);
        Files.copy(
            binaries,
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + platform),
            StandardCopyOption.REPLACE_EXISTING);
        binaries.close();
        ZipFile zipFile =
            new ZipFile(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + platform);
        zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
        Files.delete(
            Paths.get(
                MY_LOCAL_TON_ROOT_DIR
                    + nodeName
                    + File.separator
                    + BIN
                    + File.separator
                    + platform));
      }
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "create-hardfork")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "create-state")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "dht-server")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "fift")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "func")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "generate-random-id")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "lite-client")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "validator-engine")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "validator-engine-console")
          .start();
      new ProcessBuilder(
              "chmod",
              "755",
              MY_LOCAL_TON_ROOT_DIR
                  + nodeName
                  + File.separator
                  + BIN
                  + File.separator
                  + "blockchain-explorer")
          .start();

      log.debug("mac binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);

    } catch (Throwable e) {
      log.error("Cannot extract TON binaries. Error {} ", ExceptionUtils.getStackTrace(e));
      System.exit(44);
    }
  }
}
