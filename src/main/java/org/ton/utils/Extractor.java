package org.ton.utils;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.actions.MyLocalTon;
import org.ton.settings.MyLocalTonSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class Extractor {
    private static final String CURRENT_DIR = System.getProperty("user.dir");

    public static final String MY_LOCAL_TON_ROOT_DIR = CURRENT_DIR + File.separator + "myLocalTon" + File.separator;
    public static final String SMARTCONT = "smartcont";
    public static final String BIN = "bin";
    public static final String TEMPLATES = "templates";

    public static final String TONLIB_KEYSTORE = "tonlib-keystore";
    public static final String UTILS = "utils";
    public static final String DB = "db";
    public static final String WINDOWS_ZIP = "ton-win-x86_64.zip";

    private final String nodeName;

    public Extractor(String pNodeName) throws IOException {
        log.info("Working Directory = " + CURRENT_DIR);
        nodeName = pNodeName;
        extractBinaries();
    }

    private void extractBinaries() throws IOException {
        synchronized (Extractor.class) {

            if (Files.notExists(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "smartcont" + File.separator + "auto" + File.separator + "config-code.fif"), LinkOption.NOFOLLOW_LINKS)) {

                log.info("Detected OS: {}", System.getProperty("os.name"));

                Files.createDirectories(Paths.get(MyLocalTonSettings.DB_DIR));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "tonlib-keystore"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "wallets"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "zerostate"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "certs"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "import"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "static"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "keyring"));
                Files.createDirectories(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + DB + File.separator + "log"));
                if (nonNull(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath())) {
                    Files.createDirectories(Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()+ File.separator + "tonlib-keystore"));
                    Files.createDirectories(Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath() + File.separator + "wallets"));
                    Files.createDirectories(Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()+ File.separator + "zerostate"));
                    Files.createDirectories(Paths.get(MyLocalTon.getInstance().getSettings().getCustomTonBinariesPath()+ File.separator + "certs"));

                }


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
                    log.error("You are running neither on Windows nor Linux nor MacOS. We don't have TON binaries compiled for it.");
                    System.exit(0); // initiating shutdown hook
                }

                // extract other cross-platform files
                InputStream readGlobalConfig = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/global.config.json");
                Files.copy(readGlobalConfig, Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "global.config.json"), StandardCopyOption.REPLACE_EXISTING);
                readGlobalConfig.close();

                InputStream controlTemlate = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/control.template");
                Files.copy(controlTemlate, Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "control.template"), StandardCopyOption.REPLACE_EXISTING);
                controlTemlate.close();

                InputStream globalConfigTemplate = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/ton-private-testnet.config.json.template");
                Files.copy(globalConfigTemplate, Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "ton-private-testnet.config.json.template"), StandardCopyOption.REPLACE_EXISTING);
                globalConfigTemplate.close();

                InputStream exampleConfigJson = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/example.config.json");
                Files.copy(exampleConfigJson, Paths.get(MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "example.config.json"), StandardCopyOption.REPLACE_EXISTING);
                exampleConfigJson.close();

                InputStream dbConfig = Extractor.class.getClassLoader().getResourceAsStream("org/ton/db/objectsdb.conf");
                Files.copy(dbConfig, Paths.get(MyLocalTonSettings.DB_DIR + File.separator + "objectsdb.conf"), StandardCopyOption.REPLACE_EXISTING);
                dbConfig.close();

                InputStream showAddr = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/show-addr.fif");
                Files.copy(showAddr, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "show-addr.fif"), StandardCopyOption.REPLACE_EXISTING);
                showAddr.close();

                InputStream genZeroStateFif = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/patches/gen-zerostate.fif");
                Files.copy(genZeroStateFif, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + SMARTCONT + File.separator + "gen-zerostate.fif"), StandardCopyOption.REPLACE_EXISTING);
                genZeroStateFif.close();

            } else {
                log.info("Binaries already extracted.");
            }
        }
    }

    private void extractWindowsUtils() {
        try {
            if (!Files.exists(Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "du.exe"), LinkOption.NOFOLLOW_LINKS)) {
                InputStream sendSignalCtrlC64 = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/SendSignalCtrlC64.exe");
                Files.copy(sendSignalCtrlC64, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "SendSignalCtrlC64.exe"), StandardCopyOption.REPLACE_EXISTING);
                sendSignalCtrlC64.close();

                InputStream du = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/du.exe");
                Files.copy(du, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "du.exe"), StandardCopyOption.REPLACE_EXISTING);
                du.close();

                InputStream cygWinDll = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/cygwin1.dll");
                Files.copy(cygWinDll, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygwin1.dll"), StandardCopyOption.REPLACE_EXISTING);
                cygWinDll.close();

                InputStream cygIntlDll = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/cygintl-8.dll");
                Files.copy(cygIntlDll, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygintl-8.dll"), StandardCopyOption.REPLACE_EXISTING);
                cygIntlDll.close();

                InputStream cygIconDll = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/utils/cygiconv-2.dll");
                Files.copy(cygIconDll, Paths.get(MY_LOCAL_TON_ROOT_DIR + UTILS + File.separator + "cygiconv-2.dll"), StandardCopyOption.REPLACE_EXISTING);
                cygIconDll.close();
            }
        } catch (Exception e) {
            log.error("Error extracting windows utils. The file might be in use.");
        }
    }

    private void extractWindowsBinaries() {
        try {
            log.info("extracting " + WINDOWS_ZIP + " on windows");

            InputStream binaries = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/" + WINDOWS_ZIP);
            if (isNull(binaries)) {
                log.error("MyLocalTon executable does not contain resource " + WINDOWS_ZIP);
                System.exit(1);
            }

            Files.copy(binaries, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + WINDOWS_ZIP), StandardCopyOption.REPLACE_EXISTING);
            binaries.close();
            ZipFile zipFile = new ZipFile(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + WINDOWS_ZIP);
            zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
            Files.delete(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + WINDOWS_ZIP));

            log.debug("windows binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);

            extractWindowsUtils();
        } catch (Throwable e) {
            log.error("Cannot extract TON binaries. Error {} ", ExceptionUtils.getStackTrace(e));
            System.exit(44);
        }
    }

    private void extractUbuntuBinaries(String platform) {
        log.info("extracting " + platform + " on linux");

        try {
            InputStream binaries = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/" + platform + ".zip");
            Files.copy(binaries, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform + ".zip"), StandardCopyOption.REPLACE_EXISTING);
            binaries.close();
            ZipFile zipFile = new ZipFile(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform + ".zip");
            zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
            Files.delete(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform + ".zip"));

            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "create-hardfork").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "create-state").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "dht-server").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "fift").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "func").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "generate-random-id").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "lite-client").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine-console").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "blockchain-explorer").start();

            log.debug("ubuntu binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);

        } catch (Throwable e) {
            log.error("Cannot extract TON binaries. Error {} ", ExceptionUtils.getStackTrace(e));
            System.exit(44);
        }
    }

    private void extractMacBinaries(String platform) {
        log.info("extracting " + platform + " on macos");
        try {
            InputStream binaries = Extractor.class.getClassLoader().getResourceAsStream("org/ton/binaries/" + platform);
            Files.copy(binaries, Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform), StandardCopyOption.REPLACE_EXISTING);
            binaries.close();
            ZipFile zipFile = new ZipFile(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform);
            zipFile.extractAll(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);
            Files.delete(Paths.get(MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + platform));

            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "create-hardfork").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "create-state").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "dht-server").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "fift").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "func").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "generate-random-id").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "lite-client").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "validator-engine-console").start();
            new ProcessBuilder("chmod", "755", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN + File.separator + "blockchain-explorer").start();

            log.debug("mac binaries path: {}", MY_LOCAL_TON_ROOT_DIR + nodeName + File.separator + BIN);

        } catch (Throwable e) {
            log.error("Cannot extract TON binaries. Error {} ", ExceptionUtils.getStackTrace(e));
            System.exit(44);
        }
    }
}
