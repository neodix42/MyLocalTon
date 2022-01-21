package org.ton.actions;

import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXListView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.executors.createstate.CreateStateExecutor;
import org.ton.executors.dhtserver.DhtServerExecutor;
import org.ton.executors.fift.FiftExecutor;
import org.ton.executors.generaterandomid.RandomIdExecutor;
import org.ton.executors.liteclient.LiteClientExecutor;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.executors.liteclient.api.ResultListBlockTransactions;
import org.ton.executors.liteclient.api.block.Address;
import org.ton.executors.liteclient.api.block.Message;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.executors.liteclient.api.block.Value;
import org.ton.executors.validatorengine.ValidatorEngineExecutor;
import org.ton.executors.validatorengineconsole.ValidatorEngineConsoleExecutor;
import org.ton.main.App;
import org.ton.main.Main;
import org.ton.parameters.SendToncoinsParam;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.ui.controllers.MainController;
import org.ton.utils.Extractor;
import org.ton.utils.Utils;
import org.ton.wallet.Wallet;
import org.ton.wallet.WalletAddress;
import org.ton.wallet.WalletVersion;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.ton.main.App.fxmlLoader;
import static org.ton.main.App.mainController;

@Slf4j
@Getter
@Setter
public class MyLocalTon {

    public static final String VALIDATOR = "validator";
    public static final String LITESERVER = "liteserver";
    public static final String ZEROSTATE = "zerostate";
    public static final String UNINITIALIZED = "Uninitialized";
    public static final String ACTIVE = "Active";
    public static final String NONEXISTENT = "Nonexistent";
    public static final String EXTERNAL = "external";
    public static final String FROZEN = "Frozen";
    private static MyLocalTon singleInstance = null;
    private static final String CURRENT_DIR = System.getProperty("user.dir");

    private static final String DOUBLE_SPACE = "  ";
    private static final String SPACE = " ";
    private static final String MY_TON_FORKED_CONFIG_JSON = "my-ton-forked.config.json";

    private static final String SETTINGS_JSON = "settings.json";
    private static final String CONFIG_JSON = "config.json";

    public static final String MY_LOCAL_TON = "myLocalTon";
    public static final String SETTINGS_FILE = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + SETTINGS_JSON;
    public static final String TEMPLATES = "templates";
    public static final String EXAMPLE_GLOBAL_CONFIG = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + TEMPLATES + File.separator + "example.config.json";
    public static final int YEAR = 31556952;

    AtomicBigInteger prevBlockSeqno;
    ConcurrentHashMap<String, Long> concurrentBlocksHashMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Long> concurrentTxsHashMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, Long> concurrentAccountsHashMap = new ConcurrentHashMap<>();

    Boolean autoScroll;
    AtomicLong blocksScrollBarHighWaterMark = new AtomicLong(30);
    AtomicLong txsScrollBarHighWaterMark = new AtomicLong(30);
    AtomicLong accountsScrollBarHighWaterMark = new AtomicLong(30);
    public static final int SCROLL_BAR_DELTA = 30;
    public static final Long MAX_ROWS_IN_GUI = 1000L;

    ScheduledExecutorService monitorExecutorService;
    Process dhtServerProcess;
    Process genesisValidatorProcess;
    private MyLocalTonSettings settings;

    private MyLocalTon() {
        prevBlockSeqno = new AtomicBigInteger(new BigInteger("0"));
        autoScroll = true;
    }

    public static MyLocalTon getInstance() {
        if (singleInstance == null)
            singleInstance = new MyLocalTon();

        return singleInstance;
    }

    public MyLocalTonSettings getSettings() {
        return settings;
    }

    public void setSettings(MyLocalTonSettings settings) {
        this.settings = settings;
    }

    public static final class AtomicBigInteger {

        private final AtomicReference<BigInteger> valueHolder = new AtomicReference<>();

        public AtomicBigInteger(BigInteger bigInteger) {
            valueHolder.set(bigInteger);
        }

        public void set(BigInteger value) {
            BigInteger current = valueHolder.get();
            BigInteger next = new BigInteger(value.toString());
            valueHolder.compareAndSet(current, next);
        }

        public BigInteger get() {
            return valueHolder.get();
        }
    }

    public Process initGenesis(Node node) throws Exception {

        if (!Files.exists(Paths.get(node.getTonDbDir() + "state"), LinkOption.NOFOLLOW_LINKS)) {
            log.info("Initializing genesis network");
            Thread.sleep(1000);
            Platform.runLater(() -> mainController.showWarningMsg("Initializing TON blockchain very first time. It can take up to 2 minutes, please wait.", 60 * 5L));

            generateValidatorKeys(node, true);
            configureGenesisZeroState();
            createZeroState(node);
            //result: created tonDbDir + File.separator + MY_TON_GLOBAL_CONFIG_JSON with replace FILE_HASH and ROOT_HASH, still to fill [NODES]

            initDhtServer(node, EXAMPLE_GLOBAL_CONFIG, node.getNodeGlobalConfigLocation());  // result: generated dht-server/config.json
            dhtServerProcess = startDhtServer(node, node.getNodeGlobalConfigLocation());

            //run nodeInit.sh, run validator very first time
            initFullnode(node, node.getNodeGlobalConfigLocation());

            Process validatorGenesisProcess = createGenesisValidator(node, node.getNodeGlobalConfigLocation());

            installLiteServer(node, node.getNodeGlobalConfigLocation(), false);

            return validatorGenesisProcess;
        } else {
            log.info("Found non-empty state; Skip genesis initialization.");
            dhtServerProcess = startDhtServer(node, node.getNodeGlobalConfigLocation());
            Thread.sleep(100);
            return null;
        }
    }

    public void installLiteServer(Node node, String myGlobalConfig, boolean reinstall) throws Exception {
        if (Files.exists(Paths.get(node.getTonDbKeyringDir() + LITESERVER)) && (!reinstall)) {
            log.info("lite-server exists! Skipping...");
        } else {
            log.info("Installing lite-server...");

            Pair<String, String> liteServerKeys = generateLiteServerKeys(node);
            String liteServers = "\"liteservers\" : [{\"id\":\"" + liteServerKeys.getRight() + "\",\"port\":\"" + node.getLiteServerPort() + "\"}";
            log.info("liteservers: {} ", liteServers);

            //convert pub key to key
            String liteserverPubkeyBase64 = Utils.convertPubKeyToBase64(node.getTonDbKeyringDir() + "liteserver.pub");
            int publicIpNum = getIntegerIp(node.getPublicIp());

            // replace liteservers array in config.json
            String configJson = FileUtils.readFileToString(new File(node.getTonDbDir() + CONFIG_JSON), StandardCharsets.UTF_8);
            String existingLiteservers = "\"liteservers\" : " + Utils.sbb(configJson, "\"liteservers\" : [");
            String configJsonNew = StringUtils.replace(configJson, existingLiteservers, liteServers + "\n]");
            FileUtils.writeStringToFile(new File(node.getTonDbDir() + CONFIG_JSON), configJsonNew, StandardCharsets.UTF_8);

            String myGlobalTonConfig = FileUtils.readFileToString(new File(myGlobalConfig), StandardCharsets.UTF_8);
            String myGlobalTonConfigNew;
            String liteServerConfigNew;
            if (myGlobalTonConfig.contains("liteservers")) {
                //replace exiting lite-servers in global config
                String existingLiteserver = Utils.sbb(myGlobalTonConfig, "\"liteservers\":[");
                liteServerConfigNew = "[{\"id\":{\"key\":\"" + liteserverPubkeyBase64 + "\", \"@type\":\"pub.ed25519\"}, \"port\": " + node.getLiteServerPort() + ", \"ip\": " + publicIpNum + "}\n]";
                myGlobalTonConfigNew = StringUtils.replace(myGlobalTonConfig, existingLiteserver, liteServerConfigNew);
                FileUtils.writeStringToFile(new File(myGlobalConfig), myGlobalTonConfigNew, StandardCharsets.UTF_8);
            } else {
                //add new lite-servers about "validator":{
                liteServerConfigNew = "\"liteservers\":[{\"id\":{\"key\":\"" + liteserverPubkeyBase64 + "\", \"@type\":\"pub.ed25519\"}, \"port\": " + node.getLiteServerPort() + ", \"ip\": " + publicIpNum + "}\n],\n \"validator\": {";
                myGlobalTonConfigNew = StringUtils.replace(myGlobalTonConfig, "\"validator\": {", liteServerConfigNew);
                FileUtils.writeStringToFile(new File(myGlobalConfig), myGlobalTonConfigNew, StandardCharsets.UTF_8);
            }
            log.info("lite-server installed");
        }
    }

    public Process startValidator(Node node, String myGlobalConfig) {
        log.info("starting validator-engine {}", node.getNodeName());

        Pair<Process, Future<String>> validator = new ValidatorEngineExecutor().execute(node,
                "-v", Utils.getTonLogLevel(settings.getLogSettings().getTonLogLevel()),
                "-t", "1",
                "-C", myGlobalConfig,
                "--db", node.getTonDbDir(),
                "-l", node.getTonLogDir() + Utils.toUtcNoSpace(System.currentTimeMillis()),
                "--ip", node.getPublicIp() + ":" + node.getPublicPort(),
                "-S", settings.getBlockchainSettings().getValidatorSyncBefore().toString(), // 1 year, in initial sync download all blocks for last given seconds
                "-s", settings.getBlockchainSettings().getValidatorStateTtl().toString(), // state will be gc'd after this time (in seconds), default 3600
                "-b", settings.getBlockchainSettings().getValidatorBlockTtl().toString(), // blocks will be gc'd after this time (in seconds), default=7*86400
                "-A", settings.getBlockchainSettings().getValidatorArchiveTtl().toString(), // archived blocks will be deleted after this time (in seconds), default 365*86400
                "-K", settings.getBlockchainSettings().getValidatorKeyProofTtl().toString() // 10 years key blocks will be deleted after this time (in seconds), default 365*86400*10
        );
        node.setNodeProcess(validator.getLeft());
        return validator.getLeft();
    }

    public void saveSettingsToGson() throws InterruptedException {
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(this::saveSettingsToGsonSynchronized);
        service.shutdown();
        Thread.sleep(30);
    }

    private synchronized void saveSettingsToGsonSynchronized() {
        try {
            String abJson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(settings);
            FileUtils.writeStringToFile(new File(SETTINGS_FILE), abJson, StandardCharsets.UTF_8);
        } catch (Throwable e) {
            log.error("Error saving {} file: {}", SETTINGS_JSON, e.getMessage());
            e.printStackTrace();
        }
    }

    public Process createGenesisValidator(Node node, String myGlobalConfig) throws Exception {
        if (Files.exists(Paths.get(node.getTonDbDir() + File.separator + "validatorGenesis"))) {
            log.info("Found non-empty state of genesis validator.");
            return null;
        } else {

            String validatorIdHex = node.getValidatorIdHex();
            //shortly start validator
            log.info("Starting temporary full-node...");
            ValidatorEngineExecutor validatorGenesis = new ValidatorEngineExecutor();
            Pair<Process, Future<String>> validatorProcess = validatorGenesis.execute(node,
                    "-v", Utils.getTonLogLevel(settings.getLogSettings().getTonLogLevel()),
                    "-t", "1",
                    "-l", node.getTonDbDir() + "validatorGenesis",
                    "-C", myGlobalConfig,
                    "--db", node.getTonDbDir(),
                    "--ip", node.getPublicIp() + ":" + node.getPublicPort());

            log.debug("sleep 5sec");
            Thread.sleep(5000);

            String newNodeKey = generateNewNodeKey(node);
            log.info("newNodeKey {}", newNodeKey);
            String newValAdnl = generateNewNodeKey(node);
            settings.getNode(node).setValidatorAdnlAddrHex(newValAdnl);
            log.info("newValAdnl {}", newValAdnl);

            long startWorkTime = Instant.now().getEpochSecond();
            long electionId = 0L;
            long electedForDuration = startWorkTime + YEAR;

            addPermKey(node, validatorIdHex, electionId, electedForDuration);
            addTempKey(node, validatorIdHex, electionId, electedForDuration);
            addAdnl(node, newValAdnl);
            addAdnl(node, validatorIdHex);
            addValidatorAddr(node, validatorIdHex, newValAdnl, electionId, electedForDuration);

            addAdnl(node, newNodeKey);
            changeFullNodeAddr(node, newNodeKey);
            importF(node, validatorIdHex);

            saveSettingsToGson();

            return isNull(validatorProcess) ? null : validatorProcess.getLeft();
        }
    }

    private Process startDhtServer(Node node, String globalConfigFile) {
        // start dht-server in background
        log.info("genesis dht-server started at {}", node.getPublicIp() + ":" + node.getDhtPort());
        Pair<Process, Future<String>> dhtServer = new DhtServerExecutor().execute(node,
                "-v", Utils.getTonLogLevel(settings.getLogSettings().getTonLogLevel()),
                "-t", "1",
                "-C", globalConfigFile,
                "-l", node.getDhtServerDir() + Utils.toUtcNoSpace(System.currentTimeMillis()),
                "-D", node.getDhtServerDir(),
                "-I", node.getPublicIp() + ":" + node.getDhtPort());
        node.setDhtServerProcess(dhtServer.getLeft());
        return dhtServer.getLeft();
    }

    /**
     * creates dht-server/keyring directory with a key inside and generate config.json based on example.config.json.
     * Also replaces [NODES] in my-ton-global.config.json
     */
    private void initDhtServer(Node node, String exampleConfigJson, String myGlobalConfig) throws Exception {
        int publicIpNum = getIntegerIp(node.getPublicIp());
        log.debug("publicIpNum {}", publicIpNum);
        Files.createDirectories(Paths.get(node.getDhtServerDir()));

        log.info("Initializing dht-server, creating key in dht-server/keyring/hex and config.json...");
        Pair<Process, Future<String>> dhtServerInit = new DhtServerExecutor().execute(node,
                "-v", Utils.getTonLogLevel(settings.getLogSettings().getTonLogLevel()),
                "-t", "1",
                "-C", exampleConfigJson,
                "-D", node.getDhtServerDir(),
                "-I", node.getPublicIp() + ":" + node.getDhtPort());
        log.debug("dht-server result: {}", dhtServerInit.getRight().get());

        replaceOutPortInConfigJson(node.getDhtServerDir(), node.getDhtOutPort()); // no need - FYI - config.json update?

        List<String> dhtNodes = generateDhtKeys(node, publicIpNum);

        String content = FileUtils.readFileToString(new File(myGlobalConfig), StandardCharsets.UTF_8);
        if (content.contains("NODES")) { //very first creation
            log.debug("Replace NODES placeholder with dht-server entry");
            String replaced = StringUtils.replace(content, "NODES", String.join(",", dhtNodes));
            FileUtils.writeStringToFile(new File(myGlobalConfig), replaced, StandardCharsets.UTF_8);
            log.debug("dht-nodes added: {}", Files.readString(Paths.get(myGlobalConfig), StandardCharsets.UTF_8));
        } else { // modify existing
            log.debug("Replace current list of dht nodes with a new one");
            String existingNodes = Utils.sbb(content, "\"nodes\": [");
            String backToTemlate = StringUtils.replace(content, existingNodes, "[NODES]");
            String replacedLocalConfig = StringUtils.replace(backToTemlate, "NODES", String.join(",", dhtNodes));
            FileUtils.writeStringToFile(new File(myGlobalConfig), replacedLocalConfig, StandardCharsets.UTF_8);
            log.debug("dht-nodes updated: {}", Files.readString(Paths.get(myGlobalConfig), StandardCharsets.UTF_8));
        }
    }

    private void replaceOutPortInConfigJson(String path, Integer port) throws IOException {
        String contentConfigJson = Files.readString(Paths.get(path + CONFIG_JSON), StandardCharsets.UTF_8);
        String replacedConfigJson = StringUtils.replace(contentConfigJson, "3278", String.valueOf(port));
        Files.writeString(Paths.get(path + CONFIG_JSON), replacedConfigJson, StandardOpenOption.CREATE);
    }

    private int getIntegerIp(String publicIP) throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(publicIP);
        return ByteBuffer.wrap(addr.getAddress()).getInt();
    }

    private List<String> generateDhtKeys(Node node, long publicIpNum) throws Exception {

        List<String> dhtNodes = new ArrayList<>();

        String[] keyFiles = new File(node.getDhtServerKeyringDir()).list();

        if (!isNull(keyFiles) && keyFiles.length == 0) {
            throw new Exception("No keyrings found in " + node.getTonDbKeyringDir());
        }
        for (String file : keyFiles) {
            if (file.length() == 64) { //take only hash files
                log.debug("found keyring file {}", file);

                if (isWindows()) {
                    dhtNodes.add(new RandomIdExecutor().execute(node, "-m", "dht", "-k", node.getDhtServerKeyringDir() + file,
                            "-a", "\"{\\\"@type\\\": \\\"adnl.addressList\\\",  \\\"addrs\\\":[{\\\"@type\\\": \\\"adnl.address.udp\\\", \\\"ip\\\": " + publicIpNum + ", \\\"port\\\": " + node.getDhtPort() + " } ], \\\"version\\\": 0, \\\"reinit_date\\\": 0, \\\"priority\\\": 0, \\\"expire_at\\\": 0}\""));
                } else {
                    dhtNodes.add(new RandomIdExecutor().execute(node, "-m", "dht", "-k", node.getDhtServerKeyringDir() + file,
                            "-a", "{\"@type\": \"adnl.addressList\", \"addrs\":[{\"@type\": \"adnl.address.udp\", \"ip\": " + publicIpNum + ", \"port\": " + node.getDhtPort() + " } ], \"version\": 0, \"reinit_date\": 0, \"priority\": 0, \"expire_at\": 0}"));
                }
            }
        }

        log.debug(String.join(",", dhtNodes));

        return dhtNodes;
    }

    private void createZeroState(Node node) throws Exception {

        while (!generateZeroState(node)) ;

        byte[] zerostateRootHashFile = Files.readAllBytes(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.rhash"));
        byte[] zerostateFileHashFile = Files.readAllBytes(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.fhash"));
        log.info("ROOT_HASH {}", Base64.encodeBase64String(zerostateRootHashFile));
        log.info("FILE HASH {}", Base64.encodeBase64String(zerostateFileHashFile));

        settings.setZeroStateFileHashBase64(Base64.encodeBase64String(zerostateFileHashFile));
        settings.setZeroStateRootHashBase64(Base64.encodeBase64String(zerostateRootHashFile));

        log.debug(settings.toString());
        saveSettingsToGson();

        //mv zerostate.boc ../db/static/$ZEROSTATE_FILEHASH
        Files.move(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.boc"), Paths.get(node.getTonDbStaticDir() + settings.getZeroStateFileHashHex()), StandardCopyOption.REPLACE_EXISTING);

        //mv basestate0.boc ../db/static/$BASESTATE_FILEHASH
        Files.move(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "basestate0.boc"), Paths.get(node.getTonDbStaticDir() + settings.getBaseStateFileHashHex()), StandardCopyOption.REPLACE_EXISTING);

        String content = Files.readString(Paths.get(Extractor.MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "ton-private-testnet.config.json.template"), StandardCharsets.UTF_8);
        content = content.replace("ROOT_HASH", settings.getZeroStateRootHashBase64());
        content = content.replace("FILE_HASH", settings.getZeroStateFileHashBase64());
        Files.writeString(Paths.get(node.getNodeGlobalConfigLocation()), content, StandardCharsets.UTF_8);
    }

    private boolean generateZeroState(Node node) throws IOException {
        CreateStateExecutor createState = new CreateStateExecutor();
        String createStateResult = createState.execute(node, node.getTonBinDir() + "smartcont" + File.separator + "gen-zerostate.fif");
        log.debug("create zerostate output: {}", createStateResult);
        String mainWalletAddrBoth = sb(createStateResult, "wallet address = ", "(Saving address to file");
        String electroSmcAddrBoth = sb(createStateResult, "elector smart contract address = ", "(Saving address to file");
        String configSmcAddrBoth = sb(createStateResult, "config smart contract address = ", "(Saving address to file");
        String piece = StringUtils.substring(createStateResult, createStateResult.indexOf("(Initial masterchain state saved to file zerostate.boc)"));
        String masterFileHashBoth = sb(piece, "file hash= ", "root hash= ");
        String masterRootHashBoth = sb(piece, "root hash= ", "Basestate0 root hash= ");
        String basestateRootHashBoth = sb(piece, "Basestate0 root hash= ", "Basestate0 file hash= ");
        String basestateFileHashBoth = sb(piece, "Basestate0 file hash= ", "Zerostate root hash= ");
        String zerostateRootHashBoth = sb(piece, "Zerostate root hash= ", "Zerostate file hash= ");
        String zerostateFileHashBoth = StringUtils.substring(piece, piece.indexOf("Zerostate file hash= ") + "Zerostate file hash= ".length());

        String[] mainWalletAddr = mainWalletAddrBoth.split(SPACE);
        settings.setMainWalletAddrFull(mainWalletAddr[0].trim());
        settings.setMainWalletAddrBase64(mainWalletAddr[1].trim());
        byte[] mainWalletPrvKey = FileUtils.readFileToByteArray(new File(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet.pk"));
        settings.setMainWalletPrvKey(Hex.encodeHexString(mainWalletPrvKey));
        settings.setMainWalletFilenameBaseLocation(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet");

        String[] electorSmcAddr = electroSmcAddrBoth.split(SPACE);
        settings.setElectorSmcAddrHex(electorSmcAddr[0].trim());
        settings.setElectorSmcAddrBase64(electorSmcAddr[1].trim());

        String[] configSmcAddr = configSmcAddrBoth.split(SPACE);
        settings.setConfigSmcAddrHex(configSmcAddr[0].trim());
        settings.setConfigSmcAddrBase64(configSmcAddr[1].trim());
        byte[] configMasterPrvKey = FileUtils.readFileToByteArray(new File(node.getTonBinDir() + ZEROSTATE + File.separator + "config-master.pk"));
        settings.setConfigSmcPrvKey(Hex.encodeHexString(configMasterPrvKey));

        String[] masterStateFile = masterFileHashBoth.split(DOUBLE_SPACE);
        settings.setMasterStateFileHashHex(masterStateFile[0].trim());
        settings.setMasterStateFileHashBase64(masterStateFile[1].trim());

        String[] masterStateRoot = masterRootHashBoth.split(DOUBLE_SPACE);
        settings.setMasterStateRootHashHex(masterStateRoot[0].trim());
        settings.setMasterStateRootHashBase64(masterStateRoot[1].trim());

        String[] baseStateFile = basestateFileHashBoth.split(DOUBLE_SPACE); //ok
        settings.setBaseStateFileHashHex(baseStateFile[0].trim());
        settings.setBaseStateFileHashBase64(baseStateFile[1].trim());

        String[] baseStateRoot = basestateRootHashBoth.split(DOUBLE_SPACE);
        settings.setBaseStateRootHashHex(baseStateRoot[0].trim());
        settings.setBaseStateRootHashBase64(baseStateRoot[1].trim());

        String[] zeroStateFile = zerostateFileHashBoth.split(DOUBLE_SPACE); //ok
        settings.setZeroStateFileHashHex(zeroStateFile[0].trim());
        settings.setZeroStateFileHashHuman(zeroStateFile[1].trim());

        String[] zeroStateRoot = zerostateRootHashBoth.split(DOUBLE_SPACE);
        settings.setZeroStateRootHashHex(zeroStateRoot[0].trim());
        settings.setZeroStateRootHashHuman(zeroStateRoot[1].trim());
        if (
                (settings.getMasterStateFileHashHex().length() < 64) ||
                        (settings.getMasterStateRootHashHex().length() < 64) ||
                        (settings.getBaseStateFileHashHex().length() < 64) ||
                        (settings.getBaseStateRootHashHex().length() < 64) ||
                        (settings.getZeroStateRootHashHex().length() < 64) ||
                        (settings.getZeroStateFileHashHex().length() < 64)
        ) {
            log.debug("gen-zerostate.fif generated wrong hashes, recreating...");
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "config-master.addr"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.fhash"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.rhash"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "basestate0.fhash"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "basestate0.rhash"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "elector.addr"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet.addr"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "config-master.pk"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet.pk"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "basestate0.boc"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.boc"));
            return false;
        }
        log.debug(createStateResult);
        return true;
    }

    public Pair<String, String> generateLiteServerKeys(Node node) throws Exception {
        String liteserverKeys = new RandomIdExecutor().execute(node, "-m", "keys", "-n", node.getTonDbKeyringDir() + LITESERVER);
        String[] liteServerHexBase64 = liteserverKeys.split(SPACE);
        String liteServerIdHex = liteServerHexBase64[0].trim();
        String liteServerIdBase64 = liteServerHexBase64[1].trim();
        log.info("liteServerIdHex {}, liteServerIdBase64 {} on {}", liteServerIdHex, liteServerIdBase64, node.getNodeName());

        Files.copy(Paths.get(node.getTonDbKeyringDir() + LITESERVER), Paths.get(node.getTonDbKeyringDir() + liteServerIdHex), StandardCopyOption.REPLACE_EXISTING);
        return Pair.of(liteServerIdHex, liteServerIdBase64);
    }

    /**
     * creates files in db directory; prv key - validator, and pub key - validator.pub
     */
    public void generateValidatorKeys(Node node, boolean updateGenZeroStateFif) throws Exception {

        String validatorKeys = new RandomIdExecutor().execute(node, "-m", "keys", "-n", node.getTonDbKeyringDir() + VALIDATOR);
        String[] valHexBase64 = validatorKeys.split(SPACE);
        String validatorIdHex = valHexBase64[0].trim();
        String validatorIdBase64 = valHexBase64[1].trim();
        log.info("{}, validatorIdHex {}, validatorIdBase64 {}", node.getNodeName(), validatorIdHex, validatorIdBase64);

        Files.copy(Paths.get(node.getTonDbKeyringDir() + VALIDATOR), Paths.get(node.getTonDbKeyringDir() + validatorIdHex), StandardCopyOption.REPLACE_EXISTING);
        //convert pub key to key
        byte[] validatorPubKey = Files.readAllBytes(Paths.get(node.getTonDbKeyringDir() + "validator.pub"));
        byte[] removed4bytes = Arrays.copyOfRange(validatorPubKey, 4, validatorPubKey.length);

        node.setValidatorIdHex(validatorIdHex);
        node.setValidatorIdPubKeyHex(Hex.encodeHexString(removed4bytes));
        saveSettingsToGson();
        // create validator-keys.pub
        Files.write(Paths.get(node.getValidatorKeyPubLocation()), Hex.decodeHex(node.getValidatorIdPubKeyHex()), StandardOpenOption.CREATE);

        if (updateGenZeroStateFif) {
            // make full node validator
            // replace path to validator-key.pub in gen-zerostate.fif
            String genZeroStateFif = FileUtils.readFileToString(new File(node.getGenesisGenZeroStateFifLocation()), StandardCharsets.UTF_8);
            String genZeroStateFifNew = StringUtils.replace(genZeroStateFif, "// \"path_to_" + node.getNodeName() + "_pub_key\"", "\"" + node.getValidatorKeyPubLocation() + "\"");
            genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "initial_stake_" + node.getNodeName(), node.getInitialStake().toString());
            FileUtils.writeStringToFile(new File(node.getGenesisGenZeroStateFifLocation()), genZeroStateFifNew, StandardCharsets.UTF_8);
        }
    }

    private void getStats(Node node) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "getstats");
        log.debug(result.getLeft());
    }

    private void importF(Node node, String validatorIdHex) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "importf " + node.getTonDbKeyringDir() + validatorIdHex);
        log.debug(result.getLeft());
    }

    private void changeFullNodeAddr(Node node, String newNodeKey) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "changefullnodeaddr " + newNodeKey);
        log.debug(result.getLeft());
    }

    private void addValidatorAddr(Node node, String validatorIdHex, String newValAdnl, long electionId, long electedForDuration) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addvalidatoraddr  " + validatorIdHex + " " + newValAdnl + " " + (electionId + electedForDuration + 1)); // TODO some bug here
        log.debug(result.getLeft());
    }

    private void addPermKey(Node node, String validatorIdHex, long electionId, long electedForDuration) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addpermkey " + validatorIdHex + " " + electionId + " " + (electionId + electedForDuration + 1));
        log.debug(result.getLeft());
    }

    private void addTempKey(Node node, String validatorIdHex, long electionId, long electedForDuration) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addtempkey " + validatorIdHex + " " + validatorIdHex + " " + (electionId + electedForDuration + 1));
        log.debug(result.getLeft());
    }

    private void addAdnl(Node node, String newValAdnl) {
        Pair<String, Process> result = new ValidatorEngineConsoleExecutor().execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "addadnl " + newValAdnl + " 0");
        log.debug(result.getLeft());
    }

    private String generateNewNodeKey(Node node) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "newkey");
        log.debug(result.getLeft());

        return result.getLeft().substring(result.getLeft().length() - 65).trim();
    }

    private String exportPubKey(Node node, String newkey) {
        ValidatorEngineConsoleExecutor validatorConsole = new ValidatorEngineConsoleExecutor();
        Pair<String, Process> result = validatorConsole.execute(node, "-k", node.getTonBinDir() + "certs" + File.separator + "client", "-p", node.getTonBinDir() + "certs" + File.separator + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "exportpub " + newkey);
        log.debug(result.getLeft());

        return result.getLeft().substring(result.getLeft().indexOf("got public key:") + 15).trim();
    }

    public void initFullnode(Node node, String sharedGlobalConfig) throws Exception {
        //run full node very first time
        if (Files.exists(Paths.get(node.getTonDbDir() + "state"))) {
            log.info("Found non-empty state; Skip initialization!");
        } else {
            log.info("Initializing node validator, create keyrings and config.json...");
            Files.copy(Paths.get(sharedGlobalConfig), Paths.get(node.getNodeGlobalConfigLocation()), StandardCopyOption.REPLACE_EXISTING);
            ValidatorEngineExecutor validator = new ValidatorEngineExecutor();
            Pair<Process, Future<String>> validatorGenesisInit = validator.execute(node, "-C", node.getNodeGlobalConfigLocation(), "--db", node.getTonDbDir(), "--ip", node.getPublicIp() + ":" + node.getPublicPort());
            log.debug("Initialized {} validator, result {}", node.getNodeName(), validatorGenesisInit.getRight().get());

            replaceOutPortInConfigJson(node.getTonDbDir(), node.getOutPort());

            //enable access to full node from validator-engine-console - required if you want to become validator later
            String serverIdBase64 = generateServerCertificate(node);
            generateClientCertificate(node, serverIdBase64);
        }
    }

    /**
     * Creates server key in db/ and puts into db/keyring/hex.
     * Replaces CONSOLE-PORT, SERVER-ID, CLIENT-ID in control.template and puts them into db/config.json
     */
    private void generateClientCertificate(Node node, String serverIdHuman) throws Exception {
        // Generating server certificate
        if (Files.exists(Paths.get(node.getTonBinDir() + "certs" + File.separator + "client"))) {
            log.info("Found existing client certificate, skipping");
        } else {
            log.info("Generating client certificate for remote control");
            String clientIds = new RandomIdExecutor().execute(node, "-m", "keys", "-n", node.getTonBinDir() + "certs" + File.separator + "client");
            String[] clientIdsBoth = clientIds.split(SPACE);
            String clientIdHex = clientIdsBoth[0].trim();
            String clientIdBase64 = clientIdsBoth[1].trim();
            log.info("Generated client private certificate for {}: {} {}", node.getNodeName(), clientIdHex, clientIdBase64);

            //Adding client permissions
            String content = Files.readString(Paths.get(Extractor.MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "control.template"), StandardCharsets.UTF_8);
            String replacedTemplate = StringUtils.replace(content, "CONSOLE-PORT", String.valueOf(node.getConsolePort()));
            replacedTemplate = StringUtils.replace(replacedTemplate, "SERVER-ID", "\"" + serverIdHuman + "\"");
            replacedTemplate = StringUtils.replace(replacedTemplate, "CLIENT-ID", "\"" + clientIdBase64 + "\"");

            String configFile = Files.readString(Paths.get(node.getTonDbDir() + CONFIG_JSON), StandardCharsets.UTF_8);
            String configNew = StringUtils.replace(configFile, "\"control\" : [", replacedTemplate);
            Files.writeString(Paths.get(node.getTonDbDir() + CONFIG_JSON), configNew, StandardOpenOption.CREATE);
        }
    }

    /**
     * Creates server key in db and puts into directory db/keyring/hex.
     */
    private String generateServerCertificate(Node node) throws Exception {
        // Generating server certificate
        if (Files.exists(Paths.get(node.getTonBinDir() + "certs" + File.separator + "server"))) {
            log.info("Found existing server certificate, skipping!");
            return null;
        } else {
            log.info("Generating and installing server certificate for remote control");
            String serverIds = new RandomIdExecutor().execute(node, "-m", "keys", "-n", node.getTonBinDir() + "certs" + File.separator + "server");
            String[] serverIdsBoth = serverIds.split(SPACE);
            String serverIdHex = serverIdsBoth[0].trim();
            String serverIdBase64 = serverIdsBoth[1].trim();
            log.info("Server IDs for {}: {} {}", node.getNodeName(), serverIdHex, serverIdBase64);
            Files.copy(Paths.get(node.getTonBinDir() + "certs" + File.separator + "server"), Paths.get(node.getTonDbKeyringDir() + serverIdHex), StandardCopyOption.REPLACE_EXISTING);

            return serverIdBase64;
        }
    }

    private static String sb(String str, String from, String to) {
        return StringUtils.substringBetween(str, from, to);
    }

    private static int parseIp(String address) {
        int result = 0;
        for (String part : address.split(Pattern.quote("."))) {
            result = result << 8;
            result |= Integer.parseInt(part);
        }
        return result;
    }

    public void configureGenesisZeroState() throws IOException {
        String genZeroStateFifPath = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + "genesis" + File.separator + "bin" + File.separator + "smartcont" + File.separator + "gen-zerostate.fif";
        String genZeroStateFif = FileUtils.readFileToString(new File(genZeroStateFifPath), StandardCharsets.UTF_8);
        String genZeroStateFifNew = "";
        genZeroStateFifNew = StringUtils.replace(genZeroStateFif, "GLOBAL_ID", settings.getBlockchainSettings().getGlobalId().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "INITIAL_BALANCE", settings.getBlockchainSettings().getInitialBalance().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "GAS_PRICE_MC", settings.getBlockchainSettings().getGasPriceMc().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "GAS_PRICE", settings.getBlockchainSettings().getGasPrice().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "CELL_PRICE_MC", settings.getBlockchainSettings().getCellPriceMc().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "CELL_PRICE", settings.getBlockchainSettings().getCellPrice().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MAX_VALIDATORS", settings.getBlockchainSettings().getMaxValidators().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MAX_MAIN_VALIDATORS", settings.getBlockchainSettings().getMaxMainValidators().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MIN_VALIDATORS", settings.getBlockchainSettings().getMinValidators().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MIN_STAKE", settings.getBlockchainSettings().getMinValidatorStake().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MAX_STAKE", settings.getBlockchainSettings().getMaxValidatorStake().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MIN_TOTAL_STAKE", settings.getBlockchainSettings().getMinTotalValidatorStake().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MAX_FACTOR", settings.getBlockchainSettings().getMaxFactor().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ELECTED_FOR", settings.getBlockchainSettings().getElectedFor().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ELECTION_START_BEFORE", settings.getBlockchainSettings().getElectionStartBefore().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ELECTION_END_BEFORE", settings.getBlockchainSettings().getElectionEndBefore().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ELECTION_STAKE_FROZEN", settings.getBlockchainSettings().getElectionStakesFrozenFor().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ORIGINAL_VSET_VALID_FOR", settings.getBlockchainSettings().getOriginalValidatorSetValidFor().toString());
        FileUtils.writeStringToFile(new File(genZeroStateFifPath), genZeroStateFifNew, StandardCharsets.UTF_8);
    }

    public void waitForBlockchainReady(Node node) throws Exception {
        ResultLastBlock lastBlock;
        do {
            Thread.sleep(5000);
            lastBlock = LiteClientParser.parseLast(new LiteClientExecutor().executeLast(node));
        } while (isNull(lastBlock) || (lastBlock.getSeqno().compareTo(BigInteger.ONE) < 0));
    }

    public WalletEntity createWalletWithFundsAndSmartContract(Node fromNode, Node toNode, long workchain, long subWalletId, long amount) throws Exception {
        Wallet wallet = new Wallet();
        WalletVersion walletVersion;

        walletVersion = settings.getWalletSettings().getWalletVersion();
        WalletAddress walletAddress = wallet.createWallet(toNode, walletVersion, workchain, subWalletId);

        WalletEntity walletEntity = WalletEntity.builder()
                .wc(walletAddress.getWc())
                .hexAddress(walletAddress.getHexWalletAddress())
                .subWalletId(walletAddress.getSubWalletId())
                .walletVersion(walletVersion)
                .wallet(walletAddress)
                .preinstalled(true)
                .accountState(AccountState.builder().build())
                .createdAt(Instant.now().getEpochSecond())
                .build();

        App.dbPool.insertWallet(walletEntity);

        WalletAddress fromMasterWalletAddress = WalletAddress.builder()
                .fullWalletAddress(settings.getMainWalletAddrFull())
                .privateKeyHex(settings.getMainWalletPrvKey())
                .bounceableAddressBase64url(settings.getMainWalletAddrBase64())
                .filenameBase("main-wallet")
                .filenameBaseLocation(settings.getMainWalletFilenameBaseLocation())
                .build();

        SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                .executionNode(fromNode)
                .fromWallet(fromMasterWalletAddress)
                .fromWalletVersion(WalletVersion.V1)
                .fromSubWalletId(-1L)
                .destAddr(walletAddress.getNonBounceableAddressBase64Url())
                .amount(BigDecimal.valueOf(amount))
                .build();

        wallet.sendTonCoins(sendToncoinsParam);

        Thread.sleep(1000);

        wallet.installWalletSmartContract(fromNode, walletAddress);

        return walletEntity;
    }

    public void createPreInstalledWallets(Node genesisNode) throws Exception {

        long toInstall = settings.getWalletSettings().getNumberOfPreinstalledWallets();

        long installed = App.dbPool.getNumberOfPreinstalledWallets();
        log.info("Creating {} pre-installed wallets, created {}", toInstall, installed);

        if (installed < toInstall) {
            Thread.sleep(1000);
            mainController.showWarningMsg("Creating initial wallets...", 5 * 60L);
        }

        while (installed < toInstall) {
            if (App.dbPool.existsMainWallet() == 0) {
                createWalletEntity(genesisNode, getSettings().getGenesisNode().getTonBinDir() + ZEROSTATE + File.separator + "main-wallet", -1L, -1L); //WC -1
                Thread.sleep(500);
            }
            if (App.dbPool.existsConfigWallet() == 0) {
                createWalletEntity(genesisNode, getSettings().getGenesisNode().getTonBinDir() + ZEROSTATE + File.separator + "config-master", -1L, -1L); //WC -1
                Thread.sleep(500);
            }

            createWalletEntity(genesisNode, null, getSettings().getWalletSettings().getDefaultWorkChain(), getSettings().getWalletSettings().getDefaultSubWalletId());

            installed = App.dbPool.getNumberOfPreinstalledWallets();
            log.info("created {}", installed);
        }

        List<WalletEntity> wallets = App.dbPool.getAllWallets();

        for (WalletEntity wallet : wallets) {
            log.info("preinstalled wallet {}", wallet.getFullAddress());

            // always update account state on start
            //AccountState accountState = LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(genesisNode, wallet.getWc() + ":" + wallet.getHexAddress()));
            Pair<AccountState, Long> stateAndSeqno = getAccountStateAndSeqno(genesisNode, wallet.getWc() + ":" + wallet.getHexAddress());
            App.dbPool.updateWalletStateAndSeqno(wallet, stateAndSeqno.getLeft(), stateAndSeqno.getRight());

            wallet.setAccountState(stateAndSeqno.getLeft());
            wallet.setSeqno(stateAndSeqno.getRight());
            updateAccountsTabGui(wallet);
        }
    }

    public WalletEntity createWalletEntity(Node genesisNode, String fileBaseName, long workchain, long subWalletid) throws Exception {

        WalletEntity wallet;

        if (isNull(fileBaseName)) { //generate and read address of just generated wallet
            wallet = createWalletWithFundsAndSmartContract(genesisNode, genesisNode, workchain, subWalletid, settings.getWalletSettings().getInitialAmount());
            log.debug("create wallet address {}", wallet.getHexAddress());
        } else { //read address of initially created wallet (main-wallet and config-master)
            wallet = new FiftExecutor().getWalletByBasename(genesisNode, fileBaseName);
            log.info("read wallet address: {}", wallet.getHexAddress());
        }

        Pair<AccountState, Long> stateAndSeqno = getAccountStateAndSeqno(genesisNode, wallet.getWc() + ":" + wallet.getHexAddress());
        App.dbPool.updateWalletStateAndSeqno(wallet, stateAndSeqno.getLeft(), stateAndSeqno.getRight());

        wallet.setAccountState(stateAndSeqno.getLeft());
        wallet.setSeqno(stateAndSeqno.getRight());
        updateAccountsTabGui(wallet);

        return wallet;
    }

    public void runBlockchainSizeMonitor() {

        monitorExecutorService = Executors.newSingleThreadScheduledExecutor();
        monitorExecutorService.scheduleWithFixedDelay(() -> {
            Thread.currentThread().setName("MyLocalTon - Blockchain Size Monitor");

            MainController c = fxmlLoader.getController();

            Platform.runLater(() -> {
                long size = FileUtils.sizeOfDirectory(new File(CURRENT_DIR + File.separator + MY_LOCAL_TON));
                double sizePrecise = size / 1024 / 1024;
                double newSizePrecise = (sizePrecise >= 1000) ? (sizePrecise / 1024) : sizePrecise;
                String unitOfMeasurement = (sizePrecise >= 1000) ? "GB" : "MB";
                log.debug("DB size {}", String.format("%.1f%s", newSizePrecise, unitOfMeasurement));
                c.dbSizeId.setText(String.format("%.1f%s", newSizePrecise, unitOfMeasurement));
            });
        }, 0L, 15L, TimeUnit.SECONDS);
    }

    public void runBlockchainMonitor(Node node) {
        log.info("Starting node monitor");

        ExecutorService blockchainMonitorExecutorService = Executors.newSingleThreadExecutor();

        blockchainMonitorExecutorService.execute(() -> {
            Thread.currentThread().setName("MyLocalTon - Blockchain Monitor");
            ExecutorService executorService;
            while (Main.appActive.get()) {
                try {
                    executorService = Executors.newSingleThreadExecutor(); // smells
                    LiteClientExecutor liteClient = new LiteClientExecutor();

                    executorService.execute(() -> {
                        Thread.currentThread().setName("MyLocalTon - Dump Block " + prevBlockSeqno.get());
                        log.debug("Get last block");
                        ResultLastBlock lastBlock;

                        lastBlock = LiteClientParser.parseLast(liteClient.executeLast(node));

                        if (nonNull(lastBlock)) {

                            if ((!Objects.equals(prevBlockSeqno.get(), lastBlock.getSeqno())) && (lastBlock.getSeqno().compareTo(BigInteger.ZERO) != 0)) {

                                prevBlockSeqno.set(lastBlock.getSeqno());
                                log.info(lastBlock.getShortBlockSeqno());

                                List<ResultLastBlock> shardsInBlock = insertBlocksAndTransactions(node, liteClient, lastBlock, true);

                                updateTopInfoBarGui(shardsInBlock.size());
                            }
                        } else {
                            log.debug("last block is null");
                        }
                        log.debug("Thread is done {}", Thread.currentThread().getName());
                    });

                    executorService.shutdown();

                    Thread.sleep(1000);

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                }
            }
            blockchainMonitorExecutorService.shutdownNow();
        });
    }

    public List<ResultLastBlock> insertBlocksAndTransactions(Node node, LiteClientExecutor liteClient, ResultLastBlock lastBlock, boolean updateGuiNow) {

        insertBlockEntity(lastBlock);

        if (updateGuiNow) {
            updateBlocksTabGui(lastBlock);
        }

        List<ResultLastBlock> shardsInBlock = getShardsFromBlock(node, liteClient, lastBlock); // txs from basechain shards

        for (ResultLastBlock shard : shardsInBlock) {
            log.info(shard.getShortBlockSeqno());
            if (shard.getSeqno().compareTo(BigInteger.ZERO) != 0) {
                insertBlockEntity(shard);
                if (updateGuiNow) {
                    updateBlocksTabGui(shard);
                }
            }
        }

        dumpBlockTransactions(node, liteClient, lastBlock, updateGuiNow); // txs from master-chain

        shardsInBlock.stream().filter(b -> (b.getSeqno().compareTo(BigInteger.ZERO) != 0)).forEach(shard -> dumpBlockTransactions(node, liteClient, shard, updateGuiNow)); // txs from shards

        return shardsInBlock;
    }

    public List<ResultLastBlock> getShardsFromBlock(Node node, LiteClientExecutor liteClient, ResultLastBlock lastBlock) {
        try {
            List<ResultLastBlock> foundShardsInBlock = LiteClientParser.parseAllShards(liteClient.executeAllshards(node, lastBlock));
            log.debug("found {} shards in block {}", foundShardsInBlock.size(), foundShardsInBlock);
            return foundShardsInBlock;
        } catch (Exception e) {
            log.error("Error retrieving shards from the block, {}", e.getMessage());
            return null;
        }
    }

    public void dumpBlockTransactions(Node node, LiteClientExecutor liteClient, ResultLastBlock lastBlock, boolean updateGuiNow) {

        List<ResultListBlockTransactions> txs = LiteClientParser.parseListBlockTrans(liteClient.executeListblocktrans(node, lastBlock, 0));
        log.debug("found {} transactions in block {}", txs.size(), lastBlock.getShortBlockSeqno());

        for (ResultListBlockTransactions tx : txs) {
            Transaction txDetails = LiteClientParser.parseDumpTrans(liteClient.executeDumptrans(node, lastBlock, tx), settings.getUiSettings().isShowBodyInMessage());
            if (nonNull(txDetails)) {

                List<TxEntity> txEntities = extractTxsAndMsgs(lastBlock, tx, txDetails);

                txEntities.forEach(App.dbPool::insertTx);

                if (updateGuiNow) {
                    updateTxTabGui(lastBlock, tx, txDetails, txEntities);
                }

                detectNewAccount(lastBlock, updateGuiNow, tx, txDetails);
            }
        }
    }

    private void detectNewAccount(ResultLastBlock lastBlock, boolean updateGuiNow, ResultListBlockTransactions tx, Transaction txDetails) {
        if (
                (txDetails.getOrigStatus().equals(NONEXISTENT) && txDetails.getEndStatus().equals(UNINITIALIZED)) ||
                        (txDetails.getOrigStatus().equals(UNINITIALIZED) && txDetails.getEndStatus().equals(UNINITIALIZED)) ||
                        (txDetails.getOrigStatus().equals(UNINITIALIZED) && txDetails.getEndStatus().equals(ACTIVE))
        ) {
            log.info("New account detected! origStatus {}, endStatus {}, wallet created {}, txseqno {}, txLt {}, block {}", txDetails.getOrigStatus(), txDetails.getEndStatus(), txDetails.getAccountAddr(),
                    tx.getTxSeqno(), tx.getLt(), lastBlock.getShortBlockSeqno());
            WalletEntity walletEntity = insertNewAccountEntity(lastBlock, txDetails);

            if (updateGuiNow) {
                updateAccountsTabGui(walletEntity);
            }
        }
    }

    private void updateAccountsTabGui(WalletEntity walletEntity) {
        if (isNull(walletEntity)) {
            return;
        }
        log.debug("updateAccountsTabGui, wallet account addr {}, state {}", walletEntity.getHexAddress(), walletEntity.getAccountState().getStatus());

        if (Boolean.TRUE.equals(autoScroll)) {

            MainController c = fxmlLoader.getController();

            Platform.runLater(() -> {

                FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("accountrow.fxml"));
                javafx.scene.Node accountRow;
                try {
                    accountRow = fxmlLoader.load();
                } catch (IOException e) {
                    log.error("error loading accountrow.fxml file, {}", e.getMessage());
                    return;
                }

                if (walletEntity.getWc() == -1L) {
                    (accountRow.lookup("#accRowBorderPane")).setStyle("-fx-background-color: e9f4ff; -fx-padding: 10 0 0 5;");
                }

                showInGuiOnlyUniqueAccounts(walletEntity, c, accountRow);
            });
        }
    }

    private void showInGuiOnlyUniqueAccounts(WalletEntity walletEntity, MainController c, javafx.scene.Node accountRow) {
        if (isNull(concurrentAccountsHashMap.get(walletEntity.getWallet().getFullWalletAddress()))) {
            //show in gui only unique accounts. some might come from: scroll event, blockchain monitor or manual creation
            if (concurrentAccountsHashMap.size() > 100) {
                concurrentAccountsHashMap.keySet().removeAll(Arrays.asList(concurrentAccountsHashMap.keySet().toArray()).subList(concurrentAccountsHashMap.size() / 2, concurrentAccountsHashMap.size())); // truncate
            }

            concurrentAccountsHashMap.put(walletEntity.getWallet().getFullWalletAddress(), 1L);

            populateAccountRowWithData(walletEntity, accountRow, "--------------------");

            int size = c.accountsvboxid.getItems().size();

            if (size > accountsScrollBarHighWaterMark.get()) {
                c.accountsvboxid.getItems().remove(size - 1);
            }

            c.accountsvboxid.getItems().add(0, accountRow);
        } else {
            if (nonNull(walletEntity.getAccountState().getStatus())) {
                log.debug("update account details {}, {}", walletEntity.getFullAddress(), walletEntity.getAccountState().getStatus());
                for (javafx.scene.Node node : c.accountsvboxid.getItems()) {

                    if (((Label) node.lookup("#hexAddr")).getText().equals(walletEntity.getFullAddress())) {
                        populateAccountRowWithData(walletEntity, node, "--------------------");
                    }
                }
            }
        }
    }

    public void populateAccountRowWithData(WalletEntity walletEntity, javafx.scene.Node accountRow, String searchFor) {

        ((Label) accountRow.lookup("#hexAddr")).setText(walletEntity.getWallet().getFullWalletAddress());
        if (((Label) accountRow.lookup("#hexAddr")).getText().contains(searchFor)) {
            ((Label) accountRow.lookup("#hexAddr")).setTextFill(Color.GREEN);
        }

        ((Label) accountRow.lookup("#b64Addr")).setText(walletEntity.getWallet().getBounceableAddressBase64());
        ((Label) accountRow.lookup("#b64urlAddr")).setText(walletEntity.getWallet().getBounceableAddressBase64url());
        ((Label) accountRow.lookup("#nb64Addr")).setText(walletEntity.getWallet().getNonBounceableAddressBase64());
        ((Label) accountRow.lookup("#nb64urlAddr")).setText(walletEntity.getWallet().getNonBounceableAddressBase64Url());

        ((Label) accountRow.lookup("#createdat")).setText(Utils.toLocal((walletEntity.getCreatedAt())));

        if (isNull(walletEntity.getWalletVersion())) {
            ((Label) accountRow.lookup("#type")).setText("Unknown");
        } else {
            ((Label) accountRow.lookup("#type")).setText(walletEntity.getWalletVersion().getValue());
        }

        if (walletEntity.getSubWalletId() >= 0) {
            ((Label) accountRow.lookup("#walledId")).setVisible(true);
            ((Label) accountRow.lookup("#walledId")).setText("Wallet ID " + walletEntity.getSubWalletId());
        } else { //wallets V1 and V2
            ((Label) accountRow.lookup("#walledId")).setVisible(false);
            ((Label) accountRow.lookup("#walledId")).setText("-1");
        }

        if (walletEntity.getSeqno() > 0) {
            ((Label) accountRow.lookup("#seqno")).setVisible(true);
            ((Label) accountRow.lookup("#seqno")).setText("Seqno " + walletEntity.getSeqno());
        } else {
            ((Label) accountRow.lookup("#seqno")).setVisible(false);
            ((Label) accountRow.lookup("#seqno")).setText("Seqno -1");
        }

        Value value = walletEntity.getAccountState().getBalance();
        BigDecimal balance = isNull(value) ? BigDecimal.ZERO : value.getToncoins();
        String formattedBalance = String.format("%,.8f", balance.divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));
        ((Label) accountRow.lookup("#balance")).setText(formattedBalance);

        String status = isNull(walletEntity.getAccountState().getStatus()) ? "" : walletEntity.getAccountState().getStatus();
        ((Label) accountRow.lookup("#status")).setText(status);

        accountRow.lookup("#walletDeleteBtn").setDisable(
                settings.getConfigSmcAddrHex().contains(walletEntity.getHexAddress()) || settings.getMainWalletAddrFull().contains(walletEntity.getHexAddress()));

        accountRow.lookup("#accSendBtn").setDisable(
                status.equals(UNINITIALIZED) ||
                        status.equals(FROZEN) ||
                        settings.getConfigSmcAddrHex().contains(walletEntity.getHexAddress()) ||
                        isNull(walletEntity.getWallet().getPrivateKeyLocation())
        );
    }

    private void updateTxTabGui(ResultLastBlock lastBlock, ResultListBlockTransactions tx, Transaction txDetails, List<TxEntity> txEntities) {

        if (Boolean.TRUE.equals(autoScroll)) {

            MainController c = fxmlLoader.getController();

            Platform.runLater(() -> {

                applyTxGuiFilters(txEntities);

                for (TxEntity txE : txEntities) {

                    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("txrow.fxml"));
                    javafx.scene.Node txRow;
                    try {
                        txRow = fxmlLoader.load();
                    } catch (IOException e) {
                        log.error("error loading txrow.fxml file, {}", e.getMessage());
                        return;
                    }

                    if (txE.getTypeTx().equals("Message")) {
                        (txRow.lookup("#txRowBorderPane")).setStyle("-fx-background-color: #e9f4ff;");
                    }

                    showInGuiOnlyUniqueTxs(lastBlock, tx, txDetails, c, txE, txRow);  //show in gui only unique values. some might come from scroll event
                }
            });
        }
    }

    private void showInGuiOnlyUniqueTxs(ResultLastBlock lastBlock, ResultListBlockTransactions tx, Transaction txDetails, MainController c, TxEntity txE, javafx.scene.Node txRow) {
        String uniqueKey = lastBlock.getShortBlockSeqno() + txE.getTypeTx() + txE.getTypeMsg() + txE.getTxHash();
        log.debug("showInGuiOnlyUniqueTxs {}", uniqueKey);
        if (isNull(concurrentTxsHashMap.get(uniqueKey))) {
            if (concurrentTxsHashMap.size() > 800) {
                concurrentTxsHashMap.keySet().removeAll(Arrays.asList(concurrentTxsHashMap.keySet().toArray()).subList(concurrentTxsHashMap.size() / 2, concurrentTxsHashMap.size())); // truncate
            }

            concurrentTxsHashMap.put(uniqueKey, lastBlock.getCreatedAt());

            log.debug("showInGuiOnlyUniquShow {}", uniqueKey);

            populateTxRowWithData(lastBlock.getShortBlockSeqno(), tx, txDetails, txRow, txE);

            int size = c.transactionsvboxid.getItems().size();

            if (size > txsScrollBarHighWaterMark.get()) {
                c.transactionsvboxid.getItems().remove(size - 1);
            }

            c.transactionsvboxid.getItems().add(0, txRow);
        }
    }

    public void applyTxGuiFilters(List<TxEntity> txEntity) {
        if (!settings.getUiSettings().isShowMainConfigTransactions()) {
            txEntity.removeIf(t -> (settings.getMainWalletAddrFull().contains(t.getFrom().getAddr()) && settings.getElectorSmcAddrHex().contains(t.getTo().getAddr()))
            );
        }

        if (!settings.getUiSettings().isShowTickTockTransactions()) {
            txEntity.removeIf(t -> t.getTx().getDescription().getType().equals("Tick"));
            txEntity.removeIf(t -> t.getTx().getDescription().getType().equals("Tock"));
        }

        if (!settings.getUiSettings().isShowInOutMessages()) {
            txEntity.removeIf(t -> t.getTypeTx().contains("Message"));
        }
    }

    public void populateTxRowWithData(javafx.scene.Node txRow, TxEntity txEntity, String searchFor) {

        ((Label) txRow.lookup("#block")).setText(txEntity.getShortBlock());
        if (((Label) txRow.lookup("#block")).getText().equals(searchFor)) {
            ((Label) txRow.lookup("#block")).setTextFill(Color.GREEN);
        }
        ((Label) txRow.lookup("#typeTx")).setText(txEntity.getTypeTx());
        ((Label) txRow.lookup("#typeMsg")).setText(txEntity.getTypeMsg());

        ((Label) txRow.lookup("#status")).setText(txEntity.getStatus());
        ((Label) txRow.lookup("#txidHidden")).setText(txEntity.getTxHash());
        ((Label) txRow.lookup("#txAccAddrHidden")).setText(txEntity.getAccountAddress());
        ((Label) txRow.lookup("#txLt")).setText(String.valueOf(txEntity.getTxLt()));

        ((Label) txRow.lookup("#txid")).setText(txEntity.getTxHash().substring(0, 8) + "..." + txEntity.getTxHash().substring(56, 64));
        if (((Label) txRow.lookup("#txidHidden")).getText().equals(searchFor)) {
            ((Label) txRow.lookup("#txid")).setTextFill(Color.GREEN);
        }
        ((Label) txRow.lookup("#from")).setText(txEntity.getFrom().getAddr());
        if (searchFor.length() >= 64) {
            if (((Label) txRow.lookup("#from")).getText().contains(StringUtils.substring(searchFor, 4, -2))) {
                ((Label) txRow.lookup("#from")).setTextFill(Color.GREEN);
            }
        }
        if (((Label) txRow.lookup("#from")).getText().equals(searchFor)) {
            ((Label) txRow.lookup("#from")).setTextFill(Color.GREEN);
        }

        ((Label) txRow.lookup("#to")).setText(txEntity.getTo().getAddr());
        if (searchFor.length() >= 64) {
            if (((Label) txRow.lookup("#to")).getText().contains(StringUtils.substring(searchFor, 4, -2))) {
                ((Label) txRow.lookup("#to")).setTextFill(Color.GREEN);
            }
        }
        if (((Label) txRow.lookup("#to")).getText().equals(searchFor)) {
            ((Label) txRow.lookup("#to")).setTextFill(Color.GREEN);
        }
        ((Label) txRow.lookup("#amount")).setText(txEntity.getAmount().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING).toPlainString());

        ((Label) txRow.lookup("#fees")).setText(txEntity.getTx().getTotalFees().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING).toPlainString());
        ((Label) txRow.lookup("#time")).setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(new Timestamp(txEntity.getTx().getNow() * 1000).getTime())));

        showButtonWithMessage(txRow, txEntity);
    }

    public void populateTxRowWithData(String shortBlockSeqno, ResultListBlockTransactions tx, Transaction txDetails, javafx.scene.Node txRow, TxEntity txEntity) {

        ((Label) txRow.lookup("#block")).setText(shortBlockSeqno);
        ((Label) txRow.lookup("#typeTx")).setText(txEntity.getTypeTx());
        ((Label) txRow.lookup("#typeMsg")).setText(txEntity.getTypeMsg());

        ((Label) txRow.lookup("#status")).setText(txEntity.getStatus());
        ((Label) txRow.lookup("#txidHidden")).setText(tx.getHash());
        ((Label) txRow.lookup("#txAccAddrHidden")).setText(tx.getAccountAddress());
        ((Label) txRow.lookup("#txLt")).setText(txDetails.getLt().toString());
        ((Label) txRow.lookup("#txid")).setText(tx.getHash().substring(0, 8) + "..." + tx.getHash().substring(56, 64));

        ((Label) txRow.lookup("#from")).setText(txEntity.getFrom().getAddr());
        ((Label) txRow.lookup("#to")).setText(txEntity.getTo().getAddr());
        ((Label) txRow.lookup("#amount")).setText(txEntity.getAmount().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING).toPlainString());

        ((Label) txRow.lookup("#fees")).setText(txDetails.getTotalFees().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING).toPlainString());
        ((Label) txRow.lookup("#time")).setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(new Timestamp(txDetails.getNow() * 1000).getTime())));

        showButtonWithMessage(txRow, txEntity);
    }

    private void showButtonWithMessage(javafx.scene.Node txRow, TxEntity txEntity) {
        if (txEntity.getTypeTx().contains("Message") && !txEntity.getTypeMsg().contains("External In")) {
            if (!(txEntity.getTx().getInMsg().getBody().getCells().isEmpty()) && (txEntity.getTx().getInMsg().getValue().getToncoins().compareTo(BigDecimal.ZERO) > 0) &&
                    !txEntity.getTx().getInMsg().getBody().getCells().get(0).equals("FFFFFFFF")) {
                txRow.lookup("#txMsgBtn").setVisible(true);
                txRow.lookup("#txMsgBtn").setOnMouseClicked(mouseEvent -> {

                    String msg = txEntity.getTx().getInMsg().getBody().getCells().stream().map(c -> {
                        try {
                            return new String(Hex.decodeHex(c.toCharArray()));
                        } catch (DecoderException e) {
                            log.error("Cannot convert hex msg to string");
                            return "      Cannot convert hex msg to string";
                        }
                    }).collect(Collectors.joining());

                    log.info("in msg btn clicked on block {}, {}", ((Label) txRow.lookup("#block")).getText(), msg);
                    mainController.showMessage(msg.substring(5));
                });

            } else if ((!txEntity.getTx().getOutMsgs().isEmpty() && !txEntity.getTx().getOutMsgs().get(0).getBody().getCells().isEmpty())) {
                if (txEntity.getTx().getOutMsgs().get(0).getValue().getToncoins().compareTo(BigDecimal.ZERO) > 0 && !txEntity.getTx().getOutMsgs().get(0).getBody().getCells().get(0).equals("FFFFFFFF")) {

                    txRow.lookup("#txMsgBtn").setVisible(true);
                    txRow.lookup("#txMsgBtn").setOnMouseClicked(mouseEvent -> {
                        String msg = txEntity.getTx().getOutMsgs().get(0).getBody().getCells().stream().map(c -> {
                            try {
                                return new String(Hex.decodeHex(c.toCharArray()));
                            } catch (DecoderException e) {
                                log.error("Cannot convert hex msg to string");
                                return "     Cannot convert hex msg to string";
                            }
                        }).collect(Collectors.joining());

                        log.info("out msg btn clicked on block {}, {}", ((Label) txRow.lookup("#block")).getText(), msg);
                        mainController.showMessage(msg.substring(5));
                    });
                }
            }
        }
    }

    public List<TxEntity> extractTxsAndMsgs(ResultLastBlock lastBlock, ResultListBlockTransactions tx, Transaction txDetails) {
        List<TxEntity> txEntity = new ArrayList<>();
        Address to;
        Address from;
        BigDecimal amount = BigDecimal.ZERO;
        String txType;
        String msgType;
        String status;

        try {
            log.debug("extractTxsAndMsgs tx into db, block {}, hash {}", lastBlock.getShortBlockSeqno(), tx.getHash());

            String type = txDetails.getDescription().getType();

            // ordinary tx
            if (type.equals("Tick") || type.equals("Tock")) { // add tick tock tx into db
                log.debug("tick/tock tx, block {}", lastBlock.getShortBlockSeqno());
                from = Address.builder().wc(lastBlock.getWc()).addr(txDetails.getAccountAddr()).build();

                to = Address.builder().addr("").build();
                txType = "Tx";
                msgType = type;
                status = txDetails.getEndStatus();
            } else {
                log.debug("block {}, tx {}", lastBlock.getShortBlockSeqno(), txDetails);
                log.debug("Ordinary tx, block {}, inMsgSrc {}, inMsgDest {}, inMsgAmount {}, outMsgCount {}", lastBlock.getShortBlockSeqno(),
                        txDetails.getInMsg().getSrcAddr().getAddr(),
                        txDetails.getInMsg().getDestAddr().getAddr(),
                        txDetails.getInMsg().getValue().getToncoins(),
                        txDetails.getOutMsgsCount());

                from = txDetails.getInMsg().getSrcAddr();
                to = txDetails.getInMsg().getDestAddr();
                amount = txDetails.getInMsg().getValue().getToncoins();
                txType = "Tx";
                msgType = type;
                status = txDetails.getEndStatus();

                if ((StringUtils.isEmpty(from.getAddr())) && txDetails.getOutMsgsCount() > 0) {
                    from = txDetails.getOutMsgs().get(0).getSrcAddr();
                    to = txDetails.getOutMsgs().get(0).getDestAddr();
                    amount = txDetails.getOutMsgs().get(0).getValue().getToncoins();
                }

                if (Strings.isEmpty(from.getAddr()) && !Strings.isEmpty(to.getAddr())) {
                    from = Address.builder().wc(from.getWc()).addr(EXTERNAL).build();
                } else if (Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {
                    to = Address.builder().wc(to.getWc()).addr(EXTERNAL).build();
                } else if (!Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {

                } else {
                    log.error("DETECTED, TX where TO and FROM are empty");
                }
            } // end ordinary txs

            if (isNotEmpty(txType)) {
                txEntity.add(TxEntity.builder()
                        .wc(lastBlock.getWc())
                        .shard(lastBlock.getShard())
                        .seqno(lastBlock.getSeqno())
                        .blockRootHash(lastBlock.getRootHash())
                        .blockFileHash(lastBlock.getFileHash())
                        .txHash(tx.getHash())
                        .status(status)
                        .createdAt(txDetails.getNow())
                        .accountAddress(txDetails.getAccountAddr())
                        .txLt(txDetails.getLt())
                        .typeTx(txType)
                        .typeMsg(msgType)
                        .from(from)
                        .to(to)
                        .fromForSearch(from.getAddr())
                        .toForSearch(to.getAddr())
                        .amount(amount)
                        .fees(txDetails.getTotalFees().getToncoins())
                        .tx(txDetails)
                        .message(null)
                        .build());
            }

            // add in_msg and out_msgs into db
            //out msgs first
            if (txDetails.getOutMsgsCount() != 0L) {
                for (Message outMsg : txDetails.getOutMsgs()) {
                    log.debug("out msg {}", outMsg);
                    txType = "Message";
                    msgType = outMsg.getType();
                    from = outMsg.getSrcAddr();
                    to = outMsg.getDestAddr();
                    amount = outMsg.getValue().getToncoins();
                    status = "";
                    boolean ok = false;
                    if (Strings.isEmpty(from.getAddr()) && !Strings.isEmpty(to.getAddr())) {
                        from = Address.builder().wc(from.getWc()).addr(EXTERNAL).build();
                        ok = true;
                    } else if (Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {
                        to = Address.builder().wc(to.getWc()).addr(EXTERNAL).build();
                        ok = true;
                    } else if (!Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {
                        ok = true;
                    } else {
                        log.error("DETECTED, out msg with fields TO and FROM are empty");
                    }
                    if (ok) {
                        txEntity.add(TxEntity.builder()
                                .wc(lastBlock.getWc())
                                .shard(lastBlock.getShard())
                                .seqno(lastBlock.getSeqno())
                                .blockRootHash(lastBlock.getRootHash())
                                .blockFileHash(lastBlock.getFileHash())
                                .txHash(tx.getHash())
                                .status(status)
                                .createdAt(txDetails.getNow())
                                .accountAddress(txDetails.getAccountAddr())
                                .txLt(txDetails.getLt())
                                .typeTx(txType)
                                .typeMsg(msgType)
                                .from(from)
                                .to(to)
                                .fromForSearch(from.getAddr())
                                .toForSearch(to.getAddr())
                                .amount(amount)
                                .fees(outMsg.getFwdFee().add(outMsg.getIhrFee().add(outMsg.getImportFee()))) // total fee in out msg = fwd+ihr+import fee
                                .tx(txDetails)
                                .message(outMsg)
                                .build());
                    }
                }
            } // end out msgs

            //in msgs
            if (nonNull(txDetails.getInMsg())) {
                Message inMsg = txDetails.getInMsg();
                log.debug("in msg {}", inMsg);
                from = inMsg.getSrcAddr();
                to = inMsg.getDestAddr();
                amount = inMsg.getValue().getToncoins();
                txType = "Message";
                msgType = inMsg.getType();
                status = "";
                boolean ok = false;
                if (Strings.isEmpty(from.getAddr()) && !Strings.isEmpty(to.getAddr())) {
                    from = Address.builder().wc(from.getWc()).addr(EXTERNAL).build();
                    ok = true;
                } else if (Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {
                    to = Address.builder().wc(to.getWc()).addr(EXTERNAL).build();
                    ok = true;
                } else if (!Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {
                    ok = true;
                } else {
                    log.error("DETECTED, in msg where TO and FROM are empty");
                }
                if (ok) {
                    txEntity.add(TxEntity.builder()
                            .wc(lastBlock.getWc())
                            .shard(lastBlock.getShard())
                            .seqno(lastBlock.getSeqno())
                            .blockRootHash(lastBlock.getRootHash())
                            .blockFileHash(lastBlock.getFileHash())
                            .txHash(tx.getHash())
                            .status(status)
                            .createdAt(txDetails.getNow())
                            .accountAddress(txDetails.getAccountAddr())
                            .txLt(txDetails.getLt())
                            .typeTx(txType)
                            .typeMsg(msgType)
                            .from(from)
                            .to(to)
                            .fromForSearch(from.getAddr())
                            .toForSearch(to.getAddr())
                            .amount(amount)
                            .fees(inMsg.getFwdFee().add(inMsg.getIhrFee().add(inMsg.getImportFee()))) // total fee in out msg = fwd+ihr+import fee
                            .tx(txDetails)
                            .message(inMsg)
                            .build());
                }
            }
            return txEntity;
        } catch (Exception e) {
            log.error("Error inserting tx entity", e);
            return null;
        }
    }

    public WalletEntity insertNewAccountEntity(ResultLastBlock lastBlock, Transaction txDetails) {

        //AccountState accountState = LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(settings.getGenesisNode(), lastBlock.getWc() + ":" + txDetails.getAccountAddr()));
        Pair<AccountState, Long> stateAndSeqno = getAccountStateAndSeqno(settings.getGenesisNode(), lastBlock.getWc() + ":" + txDetails.getAccountAddr());
        log.debug("insertAccountEntity, wallet {}:{}, balance {}, state {}", stateAndSeqno.getLeft().getWc(), stateAndSeqno.getLeft().getAddress(), stateAndSeqno.getLeft().getBalance(), stateAndSeqno.getLeft().getStatus());

        Pair<WalletVersion, Long> walletVersionAndId = Utils.detectWalledVersionAndId(stateAndSeqno.getLeft());

        WalletEntity foundWallet = App.dbPool.findWallet(WalletPk.builder()
                .wc(lastBlock.getWc())
                .hexAddress(txDetails.getAccountAddr())
                .build());

        if (isNull(foundWallet)) {

            WalletAddress wa = new FiftExecutor().convertAddr(settings.getGenesisNode(), lastBlock.getWc() + ":" + txDetails.getAccountAddr());

            WalletAddress walletAddress = WalletAddress.builder()
                    .wc(lastBlock.getWc())
                    .hexWalletAddress(txDetails.getAccountAddr())
                    .subWalletId(walletVersionAndId.getRight())
                    .fullWalletAddress(lastBlock.getWc() + ":" + txDetails.getAccountAddr())
                    .bounceableAddressBase64url(wa.getBounceableAddressBase64url())
                    .nonBounceableAddressBase64Url(wa.getNonBounceableAddressBase64Url())
                    .bounceableAddressBase64(wa.getBounceableAddressBase64())
                    .nonBounceableAddressBase64(wa.getNonBounceableAddressBase64())
                    .build();

            WalletEntity walletEntity = WalletEntity.builder()
                    .wc(walletAddress.getWc())
                    .hexAddress(walletAddress.getHexWalletAddress())
                    .subWalletId(walletAddress.getSubWalletId())
                    .walletVersion(walletVersionAndId.getLeft())
                    .wallet(walletAddress)
                    .accountState(stateAndSeqno.getLeft())
                    .createdAt(txDetails.getNow())
                    .build();

            App.dbPool.insertWallet(walletEntity);

            return walletEntity;
        } else {
            foundWallet.setAccountState(stateAndSeqno.getLeft());
            foundWallet.setSeqno(stateAndSeqno.getRight());
            log.debug("Wallet found! Update state {}", foundWallet.getFullAddress()); // update state
            App.dbPool.updateWalletStateAndSeqno(foundWallet, stateAndSeqno.getLeft(), stateAndSeqno.getRight());

            return foundWallet;
        }
    }

    private void updateTopInfoBarGui(int shardsNum) {

        MainController c = fxmlLoader.getController();
        Platform.runLater(() -> {
            try {
                // update GUI
                c.shardsNum.setText(String.valueOf(shardsNum));
                c.liteClientInfo.setText(String.format("%s:%s", settings.getGenesisNode().getPublicIp(), settings.getGenesisNode().getLiteServerPort()));
            } catch (Exception e) {
                log.error("error updating top bar gui, {}", e.getMessage());
            }
        });
    }

    private void updateBlocksTabGui(ResultLastBlock lastBlock) {

        MainController c = fxmlLoader.getController();
        ResultLastBlock finalLastBlock = lastBlock;

        Platform.runLater(() -> {
            try {
                // update top bar
                if (finalLastBlock.getWc().equals(-1L)) {
                    c.currentBlockNum.setText(finalLastBlock.getSeqno().toString());
                }

                if (Boolean.TRUE.equals(autoScroll)) {

                    // update blocks row
                    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("blockrow.fxml"));
                    javafx.scene.Node blockRow = null;
                    try {
                        blockRow = fxmlLoader.load();
                    } catch (IOException e) {
                        log.error("error loading blockrow.fxml file, {}", e.getMessage());
                        return;
                    }
                    if (finalLastBlock.getWc() == -1L) {
                        (blockRow.lookup("#blockRowBorderPane")).setStyle("-fx-background-color: #e9f4ff;");
                    }

                    showInGuiOnlyUniqueBlocks(c, finalLastBlock, blockRow);
                }
            } catch (Exception e) {
                log.error("error displaying block, {}", e.getMessage());
            }
        });
    }

    private void showInGuiOnlyUniqueBlocks(MainController c, ResultLastBlock finalLastBlock, javafx.scene.Node blockRow) {
        if (isNull(concurrentBlocksHashMap.get(finalLastBlock.getShortBlockSeqno()))) {
            if (concurrentBlocksHashMap.size() > 400) {
                concurrentBlocksHashMap.keySet().removeAll(Arrays.asList(concurrentBlocksHashMap.keySet().toArray()).subList(concurrentBlocksHashMap.size() / 2, concurrentBlocksHashMap.size())); // truncate
            }

            concurrentBlocksHashMap.put(finalLastBlock.getShortBlockSeqno(), finalLastBlock.getCreatedAt());

            populateBlockRowWithData(finalLastBlock, blockRow, null);

            int size = c.blockslistviewid.getItems().size();
            if (size > blocksScrollBarHighWaterMark.get()) { // by default we show 30 blocks in GUI, we can fetch up to 1000 blocks by scrolling
                c.blockslistviewid.getItems().remove(size - 1);
                //concurrentHashMap.truncate
            }
            c.blockslistviewid.getItems().add(0, blockRow);
        }
    }

    public void showFoundBlocksInGui(List<BlockEntity> foundBlocks, String searchFor) {

        MainController c = fxmlLoader.getController();

        if (foundBlocks.isEmpty()) {
            c.foundBlocks.setText("Blocks (0)");
            return;
        }

        ObservableList<javafx.scene.Node> blockRows = FXCollections.observableArrayList();

        for (BlockEntity block : foundBlocks) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("blockrow.fxml"));
                javafx.scene.Node blockRow;

                blockRow = fxmlLoader.load();

                ResultLastBlock resultLastBlock = ResultLastBlock.builder()
                        .createdAt(block.getCreatedAt())
                        .seqno(block.getSeqno())
                        .rootHash(block.getRoothash())
                        .fileHash(block.getFilehash())
                        .wc(block.getWc())
                        .shard(block.getShard())
                        .build();

                populateBlockRowWithData(resultLastBlock, blockRow, searchFor);

                if (resultLastBlock.getWc() == -1L) {
                    blockRow.setStyle("-fx-background-color: e9f4ff;");
                }
                log.debug("adding block to found gui {} roothash {}", block.getSeqno(), block.getRoothash());

                blockRows.add(blockRow);

            } catch (IOException e) {
                log.error("error loading blockrow.fxml file, {}", e.getMessage());
                return;
            }
        }

        log.debug("blockRows.size  {}", blockRows.size());
        c.foundBlocks.setText("Blocks (" + blockRows.size() + ")");

        c.foundBlockslistviewid.getItems().addAll(blockRows);

    }

    public void showFoundTxsInGui(Tab tab, List<TxEntity> foundTxs, String searchFor, String accountAddr) {

        MainController c = fxmlLoader.getController();

        tab.setOnClosed(e -> {
            log.info("cls");
            if (c.foundTabs.getTabs().isEmpty()) {
                c.mainMenuTabs.getTabs().remove(c.searchTab);
                c.mainMenuTabs.getSelectionModel().selectFirst();
            }
        });

        if (foundTxs.isEmpty()) {
            if (StringUtils.isNotEmpty(accountAddr)) {
                tab.setText("Account " + Utils.getLightAddress(accountAddr) + " TXs (0)");
            } else {
                tab.setText("TXs (0)");
            }
            return;
        }

        ObservableList<javafx.scene.Node> txRows = FXCollections.observableArrayList();

        for (TxEntity tx : foundTxs) {
            try {
                FXMLLoader fxmlLoaderRow = new FXMLLoader(App.class.getResource("txrow.fxml"));
                javafx.scene.Node blockRow;

                blockRow = fxmlLoaderRow.load();

                populateTxRowWithData(blockRow, tx, searchFor);

                if (tx.getWc() == -1L) {
                    blockRow.setStyle("-fx-background-color: e9f4ff;");
                }
                log.debug("adding tx to found gui {} roothash {}", tx.getShortBlock(), tx.getTxHash());

                txRows.add(blockRow);

            } catch (IOException e) {
                log.error("error loading txrow.fxml file, {}", e.getMessage());
                return;
            }
        }

        log.debug("txRows.size {}", txRows.size());

        if (StringUtils.isNotEmpty(accountAddr)) {
            tab.setText("Account " + Utils.getLightAddress(accountAddr) + " TXs (" + txRows.size() + ")");
        } else {
            tab.setText("TXs (" + txRows.size() + ")");
        }

        ((JFXListView<javafx.scene.Node>) tab.getContent().lookup("#foundTxsvboxid")).getItems().clear();
        ((JFXListView<javafx.scene.Node>) tab.getContent().lookup("#foundTxsvboxid")).getItems().addAll(txRows);
    }

    public void showFoundAccountsInGui(List<WalletEntity> foundAccounts, String searchFor) {

        MainController c = fxmlLoader.getController();

        if (foundAccounts.isEmpty()) {
            c.foundAccounts.setText("Accounts (0)");
            return;
        }

        ObservableList<javafx.scene.Node> accountRows = FXCollections.observableArrayList();

        for (WalletEntity account : foundAccounts) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("accountrow.fxml"));
                javafx.scene.Node accountRow;

                accountRow = fxmlLoader.load();

                populateAccountRowWithData(account, accountRow, searchFor);

                if (account.getWc() == -1L) {
                    accountRow.setStyle("-fx-background-color: e9f4ff; -fx-padding: 10 0 0 5;");
                }
                log.debug("adding account to found gui {}", account.getFullAddress());

                accountRows.add(accountRow);

            } catch (IOException e) {
                log.error("error loading accountrow.fxml file, {}", e.getMessage());
                return;
            }
        }

        log.debug("accRows.size  {}", accountRows.size());
        c.foundAccounts.setText("Accounts (" + accountRows.size() + ")");

        c.foundAccountsvboxid.getItems().addAll(accountRows);
    }

    public void populateBlockRowWithData(ResultLastBlock finalLastBlock, javafx.scene.Node blockRow, String searchFor) {

        ((Label) blockRow.lookup("#wc")).setText(finalLastBlock.getWc().toString());
        ((Label) blockRow.lookup("#shard")).setText(finalLastBlock.getShard());
        ((Label) blockRow.lookup("#seqno")).setText(finalLastBlock.getSeqno().toString());
        if (((Label) blockRow.lookup("#seqno")).getText().equals(searchFor)) {
            ((Label) blockRow.lookup("#seqno")).setTextFill(Color.GREEN);
        }
        ((Label) blockRow.lookup("#filehash")).setText(finalLastBlock.getFileHash());
        ((Label) blockRow.lookup("#roothash")).setText(finalLastBlock.getRootHash());
        if (((Label) blockRow.lookup("#filehash")).getText().equals(searchFor)) {
            ((Label) blockRow.lookup("#filehash")).setTextFill(Color.GREEN);
        }
        if (((Label) blockRow.lookup("#roothash")).getText().equals(searchFor)) {
            ((Label) blockRow.lookup("#roothash")).setTextFill(Color.GREEN);
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ((Label) blockRow.lookup("#createdat")).setText(formatter.format(new Date(new Timestamp(finalLastBlock.getCreatedAt() * 1000).getTime())));
    }

    public void insertBlockEntity(ResultLastBlock lastBlock) {
        log.debug("inserting block into db {} ", lastBlock);
        BlockEntity block = BlockEntity.builder()
                .createdAt(lastBlock.getCreatedAt())
                .wc(lastBlock.getWc())
                .shard(lastBlock.getShard())
                .seqno(lastBlock.getSeqno())
                .filehash(lastBlock.getFileHash())
                .roothash(lastBlock.getRootHash())
                .build();

        //DB.insertBlock(block);
        App.dbPool.insertBlock(block);
    }

    public void runAccountsMonitor() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            Thread.currentThread().setName("MyLocalTon - Accounts Monitor");
            try {
                for (WalletEntity wallet : App.dbPool.getAllWallets()) {
                    if (Main.appActive.get()) {
                        AccountState accountState = LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(getInstance().getSettings().getGenesisNode(), wallet.getWc() + ":" + wallet.getHexAddress()));
                        if (nonNull(accountState.getBalance())) {

                            Pair<AccountState, Long> stateAndSeqno = getAccountStateAndSeqno(getInstance().getSettings().getGenesisNode(), wallet.getWc() + ":" + wallet.getHexAddress());
                            App.dbPool.updateWalletStateAndSeqno(wallet, stateAndSeqno.getLeft(), stateAndSeqno.getRight());

                            wallet.setAccountState(stateAndSeqno.getLeft());
                            wallet.setSeqno(stateAndSeqno.getRight());
                            updateAccountsTabGui(wallet);
                        }

                    }
                }
            } catch (Exception e) {
                log.error("Error in runAccountsMonitor(), " + e.getMessage());
            }
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    Pair<AccountState, Long> getAccountStateAndSeqno(Node node, String address) {
        AccountState accountState = LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(node, address));
        if (nonNull(accountState.getBalance())) {
            long seqno = new LiteClientExecutor().executeGetSeqno(getInstance().getSettings().getGenesisNode(), address);
            return Pair.of(accountState, seqno);
        }
        return Pair.of(null, -1L);
    }


    /*
        public void participate(Node node, long electionId) throws Exception {

    //        WalletAddress walletAddress = createControllingSmartContract(genesisNode, node, -1L);
    //        settings.getNode(node).setWalletAddress(walletAddress);

            String stdout = new LiteClientExecutor().executeGetElections(node);
            log.debug(stdout);
            ResultConfig15 config15 = LiteClientParser.parseConfig15(stdout);
            log.info("elections {}", config15);

            convertFullNodeToValidator(node, electionId, config15.getValidatorsElectedFor());
            String signature = new FiftExecutor().createValidatorElectionRequest(node, electionId, new BigDecimal("2.7"));
            Pair<String, String> validatorPublicKey = new FiftExecutor().signValidatorElectionRequest(node, electionId, new BigDecimal("2.7"), signature);
            settings.getNode(node).setValidatorMonitoringPubKeyHex(validatorPublicKey.getLeft());
            settings.getNode(node).setValidatorMonitoringPubKeyInteger(validatorPublicKey.getRight());

            saveSettingsToGson();

    //        new Wallet().sendGrams(node,
    //                node.getWalletAddress(),
    //                settings.getElectorSmcAddrHex(),
    //                BigDecimal.valueOf(10001L),
    //                node.getTonBinDir() + "validator-query.boc");
        }

        public void convertFullNodeToValidator(Node node, long electionId, long electedForDuration) throws Exception {
            if (Files.exists(Paths.get(node.getTonDbDir() + VALIDATOR))) {
                log.info("Found non-empty state; Skip conversion to validator!");
            } else {

                String newValKey = generateNewNodeKey(node);
                log.info("newValKey {}", newValKey);
                String newValPubKey = exportPubKey(node, newValKey);
                log.info("newValPubKey {}", newValPubKey);
                settings.getNode(node).setValidatorIdHex(newValKey);
                settings.getNode(node).setValidatorIdBase64(newValPubKey);

                String newValAdnl = generateNewNodeKey(node);
                settings.getNode(node).setValidatorAdnlAddrHex(newValAdnl);
                log.info("newValAdnl {}", newValAdnl);

                long startWorkTime = Instant.now().getEpochSecond();
                if (electionId == 0) {
                    electedForDuration = startWorkTime + electedForDuration;
                }
                addPermKey(node, newValKey, electionId, electedForDuration);
                addTempKey(node, newValKey, electionId, electedForDuration);
                addAdnl(node, newValAdnl);
                addValidatorAddr(node, newValKey, newValAdnl, electionId, electedForDuration);

                getStats(node);
                settings.setCurrentValidatorSetSince(startWorkTime);

                saveSettingsToGson();

            }
        }

        public void elections(Node genesisNode, Node node2, Node node3, Node node4, Node node5, Node node6) throws Exception {
            if (settings.getInitiallyElected()) {
                log.info("All four full nodes are validators, skipping elections.");
            } else {
                log.info("ELECTIONS STARTED");
                String stdout = new LiteClientExecutor().executeGetElections(genesisNode);
                log.debug(stdout);
                ResultConfig15 config15 = LiteClientParser.parseConfig15(stdout);
                log.info("elections {}", config15);
                //(validatorsElectedFor=3600, electionsStartBefore=180, electionsEndBefore=600, stakeHeldFor=900)

                stdout = new LiteClientExecutor().executeGetCurrentValidators(genesisNode);
                log.debug(stdout);
                // cur_validators:(validators_ext utime_since:1618912225 utime_until:1618914225 total:1 main:1 total_weight:10002

                settings.setCurrentValidatorSetSince(Long.parseLong(StringUtils.substringBetween(stdout, "utime_since:", " ").trim()));
                settings.setCurrentValidatorSetUntil(Long.parseLong(StringUtils.substringBetween(stdout, "utime_until:", " ").trim()));
                saveSettingsToGson();
                log.info("start work time: {}", toUTC(settings.getCurrentValidatorSetSince()));

                while (true) {
                    long electionId = new LiteClientExecutor().executeGetActiveElectionId(genesisNode, settings.getElectorSmcAddrHex());
                    log.info(toUTC(electionId));
                    if (electionId != 0) break;
                    Thread.sleep(10 * 1000L);
                }

                long electionId = new LiteClientExecutor().executeGetActiveElectionId(genesisNode, settings.getElectorSmcAddrHex());
                long valEndSet = settings.getCurrentValidatorSetSince() + settings.getBlockchainSettings().getOriginalValidatorSetValidFor();

                log.info("work time   : {}", toUTC(settings.getCurrentValidatorSetSince()));
                log.info("el start    : {}", toUTC(valEndSet - config15.getElectionsStartBefore()));
                log.info("el end      : {}", toUTC(valEndSet - config15.getElectionsEndBefore()));
                log.info("valSetSince : {}", toUTC(settings.getCurrentValidatorSetSince()));
                log.info("valSetUntil : {}", toUTC(settings.getCurrentValidatorSetUntil()));
                log.info("electionId  : {}", toUTC(electionId));
                log.info("el nextStart: {}", toUTC(electionId + settings.getBlockchainSettings().getElectedFor() - config15.getElectionsStartBefore()));

    //            log.info("work time   : {}", toUTC(settings.getCurrentValidatorSetSince()));
    //            log.info("el start    : {}", toUTC(settings.getCurrentValidatorSetSince() - config15.getElectionsStartBefore()));
    //            log.info("el end      : {}", toUTC(settings.getCurrentValidatorSetSince() - config15.getElectionsEndBefore()));
    //            log.info("startValidation : {}", toUTC(settings.getCurrentValidatorSetSince()));
    //            log.info("endValidation : {}", toUTC(settings.getCurrentValidatorSetSince() + settings.getElectedFor()));
    //            log.info("electionId  : {}", toUTC(electionId));
    //            log.info("el nextStart: {}", toUTC(electionId + settings.getElectedFor() - config15.getElectionsStartBefore()));

                WalletEntity walletEntity = createWalletWithFundsAndSmartContract(genesisNode, genesisNode, -1L, -1L, 30003L);
                settings.getNode(genesisNode).setWalletAddress(walletEntity.getWallet());
                walletEntity = createWalletWithFundsAndSmartContract(genesisNode, node2, -1L, -1L, 30003L);
                settings.getNode(node2).setWalletAddress(walletEntity.getWallet());
                walletEntity = createWalletWithFundsAndSmartContract(genesisNode, node3, -1L, -1L, 30003L);
                settings.getNode(node3).setWalletAddress(walletEntity.getWallet());
                walletEntity = createWalletWithFundsAndSmartContract(genesisNode, node4, -1L, -1L, 30003L);
                settings.getNode(node4).setWalletAddress(walletEntity.getWallet());
    //            walletAddress = createControllingSmartContract(genesisNode, node5, -1L);
    //            settings.getNode(node5).setWalletAddress(walletAddress);
    //            walletAddress = createControllingSmartContract(genesisNode, node6, -1L);
    //            settings.getNode(node6).setWalletAddress(walletAddress);

                Thread.sleep(6000);

                log.info("{} balance: {}", genesisNode.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(genesisNode, genesisNode.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());
                log.info("{} balance: {}", node2.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(node2, node2.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());
                log.info("{} balance: {}", node3.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(node3, node3.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());
                log.info("{} balance: {}", node4.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(node4, node4.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());
    //            log.info("{} balance: {}", node5.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor2().executeGetAccount(node5, node5.getWalletAddress().getFullWalletAddress())).getAccountBalance());
    //            log.info("{} balance: {}", node6.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor2().executeGetAccount(node6, node6.getWalletAddress().getFullWalletAddress())).getAccountBalance());

                participate(genesisNode, electionId);
                participate(node2, electionId);
                participate(node3, electionId);
                participate(node4, electionId);
    //            participate(node5, electionId);
    //            participate(node6, electionId);

                SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                        .executionNode(genesisNode)
                        .fromWallet(genesisNode.getWalletAddress())
                        .fromWalletVersion(WalletVersion.V1)
                        .fromSubWalletId(-1L)
                        .destAddr(settings.getElectorSmcAddrHex())
                        .amount(BigDecimal.valueOf(10002L))
                        .bocLocation(genesisNode.getTonBinDir() + "wallets" + File.separator + "validator-query.boc")
                        //add bounce flags
                        .build();

                new Wallet().sendTonCoins(sendToncoinsParam); // TODO

                SendToncoinsParam sendToncoinsParam2 = SendToncoinsParam.builder()
                        .executionNode(node2)
                        .fromWallet(node2.getWalletAddress())
                        .fromWalletVersion(WalletVersion.V1)
                        .fromSubWalletId(-1L)
                        .destAddr(settings.getElectorSmcAddrHex())
                        .amount(BigDecimal.valueOf(10002L))
                        .bocLocation(node2.getTonBinDir() + "validator-query.boc")
                        .build();

                new Wallet().sendTonCoins(sendToncoinsParam2);

                SendToncoinsParam sendToncoinsParam3 = SendToncoinsParam.builder()
                        .executionNode(node3)
                        .fromWallet(node3.getWalletAddress())
                        .fromWalletVersion(WalletVersion.V1)
                        .fromSubWalletId(-1L)
                        .destAddr(settings.getElectorSmcAddrHex())
                        .amount(BigDecimal.valueOf(10002L))
                        .bocLocation(node3.getTonBinDir() + "validator-query.boc")
                        .build();

                new Wallet().sendTonCoins(sendToncoinsParam3);

                SendToncoinsParam sendToncoinsParam4 = SendToncoinsParam.builder()
                        .executionNode(node4)
                        .fromWallet(node4.getWalletAddress())
                        .fromWalletVersion(WalletVersion.V1)
                        .fromSubWalletId(-1L)
                        .destAddr(settings.getElectorSmcAddrHex())
                        .amount(BigDecimal.valueOf(10002L))
                        .bocLocation(node4.getTonBinDir() + "validator-query.boc")
                        .build();
                new Wallet().sendTonCoins(sendToncoinsParam4);
    //            new Wallet().sendGrams(node5, node5.getWalletAddress(), settings.getElectorSmcAddrHex(), BigDecimal.valueOf(10002L), node5.getTonBinDir() + "validator-query.boc");
    //            new Wallet().sendGrams(node6, node6.getWalletAddress(), settings.getElectorSmcAddrHex(), BigDecimal.valueOf(10002L), node6.getTonBinDir() + "validator-query.boc");
                Thread.sleep(7000);

                stdout = new LiteClientExecutor().executeGetParticipantList(genesisNode, settings.getElectorSmcAddrHex());
                log.debug(stdout);

                Thread.sleep(2000);

                while (!LiteClientParser.parseRunMethodParticipantList(stdout).isEmpty()) {
                    stdout = new LiteClientExecutor().executeGetParticipantList(genesisNode, settings.getElectorSmcAddrHex());
                    List<ResultListParticipants> participants = LiteClientParser.parseRunMethodParticipantList(stdout);
                    log.info("PARTICIPANTS: {}", participants.size());

                    log.info("{} balance: {}", genesisNode.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(genesisNode, genesisNode.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());
                    log.info("{} balance: {}", node2.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(node2, node2.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());
                    log.info("{} balance: {}", node3.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(node3, node3.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());
                    log.info("{} balance: {}", node4.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(node4, node4.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());

                    log.info("sleep 15sec");
                    Thread.sleep(15 * 1000L);
                }

                stdout = new LiteClientExecutor().executeGetCurrentValidators(genesisNode);
                log.debug(stdout);
                while (Long.parseLong(StringUtils.substringBetween(stdout, "total:", " ").trim()) != 4) {
                    stdout = new LiteClientExecutor().executeGetCurrentValidators(genesisNode);
                    log.info("sleep 10sec");
                    Thread.sleep(10 * 1000L);
                }
                settings.setInitiallyElected(true);
                saveSettingsToGson();
                log.info("Up and running 4 validators");
            }
        }

        public void createHardFork(Node forkedNode, Node toNode) throws Exception {
            //public void createHardFork(Node node, ResultLastBlock lastBlock, String externalMsgLocation) throws Exception {

            getStats(forkedNode);

            log.info("run create-hardfork");
            //String externalMsgLocation = createExternalMessage(node, toNode);
            //log.info("sleep 7sec");
            //Thread.sleep(7000);
            //get last block id
            //ResultLastBlock lastBlock = getLastBlock(node);

            WalletAddress fromWalletAddress = settings.getNode(forkedNode).getWalletAddress();
            SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                    .executionNode(forkedNode)
                    .fromWallet(fromWalletAddress)
                    .fromWalletVersion(WalletVersion.V1)
                    .fromSubWalletId(-1L)
                    .destAddr(toNode.getWalletAddress().getBounceableAddressBase64url())
                    .amount(new BigDecimal(123L))
                    .build();
            String externalMsgLocation = new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
            //String externalMsgLocation = new Wallet().getSeqNoAndPrepareBoc(node, fromWalletAddress, toNode.getWalletAddress().getBounceableAddress(), new BigDecimal(123L), null);
            settings.setExternalMsgLocation(externalMsgLocation);
            saveSettingsToGson();

            log.info("sleep 5sec");
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam); // 10 toncoins was
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
            Thread.sleep(3 * 1000L);

            log.info("**************** {} balance before: {}", forkedNode.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(forkedNode, toNode.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());

            //get last block id
            ResultLastBlock forkFromBlock = getLastBlock(forkedNode);

            Thread.sleep(60 * 1000L);

            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam); //20toncoins
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);

            getStats(forkedNode);

            Thread.sleep(60 * 1000L);

            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam); // 30 toncoins was
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);

            getStats(forkedNode);

            Thread.sleep(60 * 1000L);

            getStats(forkedNode);

            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam); // 40 toncoins was
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
            Thread.sleep(3 * 1000L);
            new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);

    //        settings.getGenesisNode().getNodeProcess().destroy();
    //        settings.getNode2().getNodeProcess().destroy();
    //        settings.getNode3().getNodeProcess().destroy();
    //        settings.getNode4().getNodeProcess().destroy();
    //
    //        settings.getNode5().getNodeProcess().destroy();
    //        settings.getNode6().getNodeProcess().destroy();
    //        settings.getNode7().getNodeProcess().destroy();

            log.info(new LiteClientExecutor().executeGetCurrentValidators(forkedNode)); // TODO PARSING
            //stop instance
            forkedNode.getNodeProcess().destroy();
            //newNode1.getNodeProcess().destroy();
            //newNode2.getNodeProcess().destroy();

            Thread.sleep(1000L);
            ResultLastBlock newBlock = generateNewBlock(forkedNode, forkFromBlock, externalMsgLocation);

            // reuse dht-server and use lite-server of node4
            addHardForkEntryIntoMyGlobalConfig(forkedNode, forkedNode.getNodeGlobalConfigLocation(), newBlock);

            startValidator(forkedNode, forkedNode.getNodeForkedGlobalConfigLocation());

            FileUtils.copyFile(new File(forkedNode.getNodeForkedGlobalConfigLocation()), new File(settings.getNode2().getNodeForkedGlobalConfigLocation()));
            FileUtils.copyFile(new File(forkedNode.getNodeForkedGlobalConfigLocation()), new File(settings.getNode3().getNodeForkedGlobalConfigLocation()));
            FileUtils.copyDirectory(new File(forkedNode.getTonDbStaticDir()), new File(settings.getNode2().getTonDbStaticDir()));
            FileUtils.copyDirectory(new File(forkedNode.getTonDbStaticDir()), new File(settings.getNode3().getTonDbStaticDir()));

            settings.getNode2().getNodeProcess().destroy();
            settings.getNode3().getNodeProcess().destroy();

            startValidator(settings.getNode2(), settings.getNode2().getNodeForkedGlobalConfigLocation());
            startValidator(settings.getNode3(), forkedNode.getNodeForkedGlobalConfigLocation());

            log.info("All forked nodes are started. Sleep 60 sec");
            Thread.sleep(60 * 1000L);
            // but with forked config


    /*
            //recreateDhtServer(genesisForkedNode, genesisForkedNode.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON); 1
            //idea, update global config with new liteserver
            //recreateLiteServer(node);
            //init

            //recreateLocalConfigJsonAndValidatorAccess(genesisForkedNode, genesisForkedNode.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON); 2
            //recreateLiteServer(genesisForkedNode); 3
            // error after above line with lite-server recreated
            //[adnl-ext-server.cpp:34][!manager]	failed ext query: [Error : 651 : node not synced]
            // error from lite-client
            // cannot get masterchain info from server
            //0. delete keyring directory. Execute generateValidatorKeys then

            // here lite-client can't connect, need to recreate adnl and val keys
    //        log.info("recreating new node keys and adnl address addresses");
    //        Files.deleteIfExists(Paths.get(forkedNode.getTonDbDir() + File.separator + "temporary"));
    //        log.info("Starting temporary full-node...");

            // re-elect this node
            //Process forkedValidator = createGenesisValidator(node, node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON);
            // node4 valik,
            // completed sync. Validating 3 groups, reused 1 times and fails with:
            //  AdnlPeerTableImpl::subscribe - [adnl-peer-table.cpp:234][!PeerTable][&it != local_ids_.end()]
            //  smth like current validators cannot be found in dht server
            // if node is not validator - Validating 0 groups and not failing

            //start first forked full node OK (he is not validator if we compare with genesis validator)
            Process forkedValidator = startValidator(forkedNode, forkedNode.getNodeForkedGlobalConfigLocation());
            log.info("sleep 8sec");
            Thread.sleep(8 * 1000);
            // wait election id
            // replacing original global config with forked one
            FileUtils.copyFile(new File(forkedNode.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON), new File(forkedNode.getNodeGlobalConfigLocation()));
            //long electionId = new LiteClientExecutor2().executeGetActiveElectionId(genesisForkedNode, settings.getElectorSmcAddrHex());

            convertFullNodeToValidator(forkedNode, 0L, settings.getElectedFor());
            // completed sync. Validating 0 groups
            // error - "too big masterchain seqno"
            // if patch  with "if (!force) {" then error - "too small read"

            // a eto chto bi validator bil initial
    //        log.info("killing temp validator");
    //        if (forkedValidator != null) {
    //            forkedValidator.destroy();
    //        }
    //        startValidator(node, node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON);

            // create neighbors

            log.info("Adding new node6");
    //  cant join other full nodes: Check `block_id_.seqno() >= opts_->get_last_fork_masterchain_seqno()` failed
    //  add static/ forked files to the node and above disappears

            // node6
            newNode1.getNodeProcess().destroy();
            FileUtils.copyDirectory(new File(forkedNode.getTonDbStaticDir()), new File(newNode1.getTonDbStaticDir()));
            FileUtils.copyFile(new File(forkedNode.getNodeForkedGlobalConfigLocation()), new File(newNode1.getNodeForkedGlobalConfigLocation()));
            recreateLocalConfigJsonAndValidatorAccess(newNode1, newNode1.getNodeForkedGlobalConfigLocation());
            recreateLiteServer(newNode1);
            startValidator(newNode1, forkedNode.getNodeForkedGlobalConfigLocation());
            convertFullNodeToValidator(newNode1, 0L, settings.getElectedFor()); // completed sync. Validating 0 groups

            log.info("Adding new node7");
            // node7
            newNode2.getNodeProcess().destroy();
            FileUtils.copyDirectory(new File(forkedNode.getTonDbStaticDir()), new File(newNode2.getTonDbStaticDir()));
            FileUtils.copyFile(new File(forkedNode.getNodeForkedGlobalConfigLocation()), new File(newNode2.getNodeForkedGlobalConfigLocation()));
            recreateLocalConfigJsonAndValidatorAccess(newNode2, newNode2.getNodeForkedGlobalConfigLocation());
            recreateLiteServer(newNode2);
            startValidator(newNode2, forkedNode.getNodeForkedGlobalConfigLocation());
            convertFullNodeToValidator(newNode2, 0L, settings.getElectedFor()); // completed sync. Validating 0 groups

            new Utils().showThreads();

            while (true) {
                ResultLastBlock block = getLastBlockFromForked(forkedNode);
                log.info("last from {}          {}", settings.getNode2().getNodeName(), getLastBlockFromForked(settings.getNode2()));
                log.info("last from {}          {}", settings.getNode3().getNodeName(), getLastBlockFromForked(settings.getNode3()));
                log.info("last from {}          {}", forkedNode.getNodeName(), getLastBlockFromForked(forkedNode));
                log.info("**************** {} balance after: {}", forkedNode.getNodeName(), LiteClientParser.parseGetAccount(new LiteClientExecutor(true).executeGetAccount(forkedNode, toNode.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());

                log.info("hashes on {} {}", settings.getNode2(), new LiteClientExecutor(true).executeBySeqno(settings.getNode2(), block.getWc(), block.getShard(), block.getSeqno()));
                log.info("hashes on {} {}", settings.getNode3(), new LiteClientExecutor(true).executeBySeqno(settings.getNode3(), block.getWc(), block.getShard(), block.getSeqno()));
                log.info("hashes on {} {}", forkedNode.getNodeName(), new LiteClientExecutor(true).executeBySeqno(forkedNode, block.getWc(), block.getShard(), block.getSeqno()));

                Thread.sleep(10 * 1000L);
            }


    //        Thread.sleep(80 * 1000);
    //
    //        new LiteClientExecutor2().executeGetActiveElectionId(forkedNode, settings.getElectorSmcAddrHex());
    //
    //        String stdout = new LiteClientExecutor2().executeGetCurrentValidators(forkedNode);
    //        log.info(stdout);
    //
    //        stdout = new LiteClientExecutor2().executeGetPreviousValidators(forkedNode);
    //        log.info(stdout);
    //
    //        stdout = new LiteClientExecutor2().executeGetParticipantList(forkedNode, settings.getElectorSmcAddrHex());
    //        log.info(stdout);
    //
    //        Thread.sleep(80 * 1000);
    //
    //        new LiteClientExecutor2().executeGetActiveElectionId(forkedNode, settings.getElectorSmcAddrHex());
    //        // wait election id
    //        // send partipation request


            //startValidator(newNode, node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON);
            // fails with Check `block_id_.seqno() >= opts_->get_last_fork_masterchain_seqno()` failed
            // better to take synced node
        }


    //    public String createExternalMessage(Node node, Node toNode) throws Exception {
    //        WalletAddress fromWalletAddress = settings.getNode(node).getWalletAddress();
    //        String externalMsgLocation = new Wallet().getSeqNoAndPrepareBoc(node, fromWalletAddress, toNode.getWalletAddress().getBounceableAddress(), new BigDecimal(123L), null);
    //        settings.setExternalMsgLocation(externalMsgLocation);
    //        saveSettingsToGson();
    //        return externalMsgLocation;
    //    }

        private void recreateLocalConfigJsonAndValidatorAccess(Node node, String myGlobalConfig) throws Exception {
            //1. just delete config.json
            FileUtils.deleteQuietly(new File(node.getTonDbDir() + "config.json"));
            FileUtils.deleteQuietly(new File(node.getTonDbDir() + "server"));
            FileUtils.deleteQuietly(new File(node.getTonDbDir() + "server.pub"));
            FileUtils.deleteQuietly(new File(node.getTonDbDir() + "client"));
            FileUtils.deleteQuietly(new File(node.getTonDbDir() + "client.pub"));

            //recreate initial local configuration config.json
            startValidator(node, myGlobalConfig);

            Thread.sleep(1000);
            //1. full node should not be started - passed ok
            replaceOutPortInConfigJson(node.getTonDbDir(), node.getOutPort());
            //enable access to full node from validator-engine-console - required if you want to become validator later
            String serverIdBase64 = generateServerCertificate(node);
            generateClientCertificate(node, serverIdBase64);
        }

        private void recreateDhtServer(Node node, String myGlobalConfig) throws Exception {
            log.info("recreate dht-server");
            //re-create dht-server - maybe leave it?
            FileUtils.deleteDirectory(new File(node.getDhtServerDir()));
            initDhtServer(node, EXAMPLE_GLOBAL_CONFIG, myGlobalConfig);
            startDhtServer(node, myGlobalConfig);
        }

        private void recreateLiteServer(Node node) throws Exception {
            log.info("recreate lite-server");
            Files.deleteIfExists(Paths.get(node.getTonDbKeyringDir() + LITESERVER));
            Files.deleteIfExists(Paths.get(node.getTonDbKeyringDir() + "liteserver.pub"));
            installLiteServer(node, node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON, true); //not needed actually?

        }

        public void addHardForkEntryIntoMyGlobalConfig(Node node, String globalConfig, ResultLastBlock newBlock) throws IOException, DecoderException {
            //add forks 1. put "hardforks": [ into global config, filehash and roothash taken from new block
            String myLocalTonConfig = FileUtils.readFileToString(new File(globalConfig), StandardCharsets.UTF_8);
            String replacedMyLocalTonConfig = StringUtils.replace(myLocalTonConfig, "\"validator\": {", "\"validator\": {\n    \"hardforks\": [\n" +
                    "\t    {\n" +
                    "\t\t    \"file_hash\": \"" + Base64.encodeBase64String(Hex.decodeHex(newBlock.getFileHash())) + "\",\n" +
                    "\t\t    \"seqno\": " + newBlock.getSeqno() + ",\n" +
                    "\t\t    \"root_hash\": \"" + Base64.encodeBase64String(Hex.decodeHex(newBlock.getRootHash())) + "\",\n" +
                    "\t\t    \"workchain\": " + newBlock.getWc() + ",\n" +
                    "\t\t    \"shard\": " + new BigInteger(newBlock.getShard(), 16).longValue() + "\n" +
                    "\t    }\n" +
                    "    ],");

            FileUtils.writeStringToFile(new File(node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON), replacedMyLocalTonConfig, StandardCharsets.UTF_8);
            log.debug("added hardforks to {}: {}", node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON, replacedMyLocalTonConfig);
        }

        public ResultLastBlock getLastBlock(Node node) throws InterruptedException, java.util.concurrent.ExecutionException {
            Pair<Process, Future<String>> liteClientOutput = new LiteClientExecutor().execute(node, "last");
            String stdout = liteClientOutput.getRight().get();
            ResultLastBlock lastBlock = LiteClientParser.parseLast(stdout);
            log.debug("parsed last block {}", lastBlock);
            return lastBlock;
        }

        public ResultLastBlock getLastBlockFromForked(Node node) throws InterruptedException, java.util.concurrent.ExecutionException {
            Pair<Process, Future<String>> liteClientOutput = new LiteClientExecutor(true).execute(node, "last");
            String stdout = liteClientOutput.getRight().get();
            ResultLastBlock lastBlock = LiteClientParser.parseLast(stdout);
            log.debug("parsed last block {}", lastBlock);
            return lastBlock;
        }

        public ResultLastBlock generateNewBlock(Node node, ResultLastBlock lastBlock, String externalMsgLocation) throws Exception {

            //run create-hardfork, creates new block and put state in static directory
            Pair<Process, Future<String>> hardForkOutput = new HardforkExecutor().execute(node,
                    "-D", node.getTonDbDir(),
                    "-T", lastBlock.getFullBlockSeqno(),
                    "-w", lastBlock.getWc() + ":" + lastBlock.getShard()
                    //        ,"-m", externalMsgLocation
            );

            String newBlockOutput = hardForkOutput.getRight().get().toString();
            log.info("create-hardfork output {}", newBlockOutput);

            ResultLastBlock newBlock;
            if (newBlockOutput.contains("created block") && newBlockOutput.contains("success, block")) {
                newBlock = LiteClientParser.parseCreateHardFork(newBlockOutput);
                log.info("parsed new block {}", newBlock);

            } else {
                throw new Exception("Can't create block using create-hardfork utility.");
            }
            return newBlock;
        }
    */

}