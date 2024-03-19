package org.ton.actions;

import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXListView;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.util.Strings;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.blockchainexplorer.BlockchainExplorer;
import org.ton.executors.fift.Fift;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.ResultComputeReturnStake;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.executors.liteclient.api.ResultListBlockTransactions;
import org.ton.executors.liteclient.api.block.LiteClientAddress;
import org.ton.executors.liteclient.api.block.Message;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.executors.tonhttpapi.TonHttpApi;
import org.ton.executors.validatorengine.ValidatorEngine;
import org.ton.executors.validatorengineconsole.ValidatorEngineConsole;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.RawAccountState;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.tonlib.types.TvmStackEntryNumber;
import org.ton.java.utils.Utils;
import org.ton.main.App;
import org.ton.main.Main;
import org.ton.parameters.SendToncoinsParam;
import org.ton.parameters.ValidationParam;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.ui.controllers.MainController;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomNotificationEvent;
import org.ton.ui.custom.events.event.CustomSearchEvent;
import org.ton.utils.MyLocalTonUtils;
import org.ton.wallet.MyWallet;
import org.ton.wallet.WalletAddress;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.ton.main.App.fxmlLoader;
import static org.ton.main.App.mainController;
import static org.ton.ui.custom.events.CustomEventBus.emit;

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
    public static final long ONE_BLN = 1000000000L;
    public static final String FOUND_COLOR_HIHGLIGHT = "-fx-text-fill: #0088CC;";
    private static MyLocalTon singleInstance = null;
    public static ScheduledExecutorService validatorsMonitor = null;
    public static Tonlib tonlib;
    public static Tonlib tonlibBlockMonitor;

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

    AtomicBoolean insertingNewAccount = new AtomicBoolean(false);
    Boolean autoScroll;
    AtomicLong blocksScrollBarHighWaterMark = new AtomicLong(30);
    AtomicLong txsScrollBarHighWaterMark = new AtomicLong(30);
    AtomicLong accountsScrollBarHighWaterMark = new AtomicLong(30);
    public static final int SCROLL_BAR_DELTA = 30;
    public static final Long MAX_ROWS_IN_GUI = 1000L;
    public static final int YEAR_1971 = 34131600;
    public static final int VALIDATION_GUI_REFRESH_SECONDS = 60;

    ScheduledExecutorService monitorExecutorService;
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

    public void runBlockchainExplorer() {
        if (!GraphicsEnvironment.isHeadless()) {
            Platform.runLater(() -> mainController.startNativeBlockchainExplorer());
        } else {
            if (settings.getUiSettings().isEnableBlockchainExplorer()) {
                log.info("Starting native blockchain-explorer on port {}", settings.getUiSettings().getBlockchainExplorerPort());
                BlockchainExplorer blockchainExplorer = new BlockchainExplorer();
                blockchainExplorer.startBlockchainExplorer(settings.getGenesisNode(), settings.getGenesisNode().getNodeGlobalConfigLocation(), settings.getUiSettings().getBlockchainExplorerPort());
                Utils.sleep(2);
            }
        }
    }

    public void runTonHttpApi() {
        if (!GraphicsEnvironment.isHeadless()) {
            Platform.runLater(() -> mainController.startTonHttpApi());
        } else {
            if (settings.getUiSettings().isEnableTonHttpApi()) {
                log.info("Starting ton-http-api on port {}", settings.getUiSettings().getTonHttpApiPort());
                Utils.sleep(3);
                TonHttpApi tonHttpApi = new TonHttpApi();
                tonHttpApi.startTonHttpApi(settings.getGenesisNode(), settings.getGenesisNode().getNodeGlobalConfigLocation(), settings.getUiSettings().getTonHttpApiPort());
            }
        }
    }

    public void runNodesStatusMonitor() {

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Thread.currentThread().setName("MyLocalTon - Nodes Monitor");

            for (String nodeName : settings.getActiveNodes()) {

                Executors.newSingleThreadExecutor().execute(() -> {
                    Thread.currentThread().setName("MyLocalTon - " + nodeName + " Status Monitor");
                    try {

                        Node node = settings.getNodeByName(nodeName);
                        ResultLastBlock lastBlock = LiteClientParser.parseLast(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeLast(node));
                        if (isNull(lastBlock)) {
                            node.setStatus("not ready");
                            log.info("{} is not ready", nodeName);

                        } else if (lastBlock.getSyncedSecondsAgo() > 15) {
                            node.setStatus("out of sync by " + lastBlock.getSyncedSecondsAgo() + " seconds");
                            log.info("{} out of sync by {} seconds", nodeName, lastBlock.getSyncedSecondsAgo());
                        } else {
                            node.setStatus("ready");
                            node.setFlag("cloned");
                            log.info("{} is ready", nodeName);
                        }

                        if (!GraphicsEnvironment.isHeadless()) {
                            Platform.runLater(() -> MyLocalTonUtils.showNodeStatus(settings.getNodeByName(nodeName), MyLocalTonUtils.getNodeStatusLabelByName(nodeName), MyLocalTonUtils.getNodeTabByName(nodeName)));
                        }

                    } catch (Exception e) {
                        log.error("Error in runNodesMonitor(), " + e.getMessage());
                    }
                });
            }
        }, 0L, 15L, TimeUnit.SECONDS);
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

    public void initGenesis(Node node) throws Exception {

        if (!Files.exists(Paths.get(node.getTonDbDir() + "state"), LinkOption.NOFOLLOW_LINKS)) {
            log.debug("Initializing genesis network");

            ValidatorEngine validatorEngine = new ValidatorEngine();

            validatorEngine.generateValidatorKeys(node, true);
            validatorEngine.configureGenesisZeroState();
            validatorEngine.createZeroState(node);
            //result: created tonDbDir + File.separator + MY_TON_GLOBAL_CONFIG_JSON with replace FILE_HASH and ROOT_HASH, still to fill [NODES]

            //run validator very first time
            validatorEngine.initFullnode(node, node.getNodeGlobalConfigLocation());

            createGenesisValidator(node, node.getNodeGlobalConfigLocation());

            validatorEngine.enableLiteServer(node, node.getNodeGlobalConfigLocation(), false);

            settings.getActiveNodes().add(node.getNodeName());
        } else {
            log.debug("Found non-empty state; Skip genesis initialization.");
        }
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
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * start validator for a short time in order to execute commands via validator-engine-console
     *
     * @param node           Node
     * @param myGlobalConfig String
     */
    public void createGenesisValidator(Node node, String myGlobalConfig) throws Exception {

        String validatorPrvKeyHex = node.getValidatorPrvKeyHex();
        log.debug("{} validatorIdHex {}", node.getNodeName(), node.getValidatorPrvKeyHex());

        log.debug("Starting temporary full-node...");
        Process validatorProcess = new ValidatorEngine().startValidatorWithoutParams(node, myGlobalConfig).getLeft();

        log.debug("sleep 5sec");
        Thread.sleep(5000);
        ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();

        String newNodeKey = validatorEngineConsole.generateNewNodeKey(node);
        String newNodePubKey = validatorEngineConsole.exportPubKey(node, newNodeKey);
        String newValAdnl = validatorEngineConsole.generateNewNodeKey(node);

        log.debug("newNodeKey {}, newNodePubKey {}, newValAdnl {}", newNodeKey, newNodePubKey, newValAdnl);

        node.setValidatorPubKeyBase64(newNodePubKey);
        node.setValidatorAdnlAddrHex(newValAdnl);

        long startWorkTime = Instant.now().getEpochSecond();
        long electionId = 0L;
        long electionEnd = startWorkTime + YEAR;

        validatorEngineConsole.addPermKey(node, validatorPrvKeyHex, electionId, electionEnd);
        validatorEngineConsole.addTempKey(node, validatorPrvKeyHex, electionEnd);
        validatorEngineConsole.addAdnl(node, newValAdnl);
        validatorEngineConsole.addAdnl(node, validatorPrvKeyHex);
        validatorEngineConsole.addValidatorAddr(node, validatorPrvKeyHex, newValAdnl, electionEnd);

        validatorEngineConsole.addAdnl(node, newNodeKey);
        validatorEngineConsole.changeFullNodeAddr(node, newNodeKey);
        validatorEngineConsole.importF(node, validatorPrvKeyHex);

        saveSettingsToGson();

        validatorProcess.destroy();
    }

    public WalletEntity createWalletWithFundsAndSmartContract(Node fromNode, WalletVersion walletVersion, long workchain, long subWalletId, BigInteger amount) throws Exception {
        MyWallet myWallet = new MyWallet();


        WalletAddress walletAddress = myWallet.createWalletByVersion(walletVersion, workchain, subWalletId);

        WalletEntity walletEntity = WalletEntity.builder()
                .wc(walletAddress.getWc())
                .hexAddress(walletAddress.getHexWalletAddress().toUpperCase())
                .walletVersion(walletVersion)
                .wallet(walletAddress)
                .accountState(RawAccountState.builder().build())
                .createdAt(Instant.now().getEpochSecond())
                .build();

        App.dbPool.insertWallet(walletEntity);

        // TOP UP NEW WALLET

        WalletAddress fromMasterWalletAddress = WalletAddress.builder()
                .fullWalletAddress(settings.getMainWalletAddrFull())
                .privateKeyHex(settings.getMainWalletPrvKey())
                .bounceableAddressBase64url(settings.getMainWalletAddrBase64())
                .filenameBase("main-wallet")
                .filenameBaseLocation(settings.getMainWalletFilenameBaseLocation())
                .build();

        SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                .executionNode(settings.getGenesisNode())
                .fromWallet(fromMasterWalletAddress)
                .fromWalletVersion(WalletVersion.master) // master
                .fromSubWalletId(-1L)
                .destAddr(walletAddress.getNonBounceableAddressBase64Url())
                .amount(amount)
                .build();

        boolean sentOK = myWallet.sendTonCoins(sendToncoinsParam);

        if (sentOK) {
            Thread.sleep(2000);
            myWallet.installWalletSmartContract(fromNode, walletAddress);
        } else {
            App.mainController.showErrorMsg(String.format("Failed to send %s Toncoins to %s", amount, walletAddress.getNonBounceableAddressBase64Url()), 5);
        }

        return walletEntity;
    }

    public void createInitialWallets(Node genesisNode) throws Exception {

        long installed = App.dbPool.getNumberOfWalletsFromAllDBsAsync();

        if (installed < 3) {
            Thread.sleep(1000);
            if (!GraphicsEnvironment.isHeadless()) {
                mainController.showInfoMsg("Creating initial wallets...", 6);
            } else {
                log.info("Creating initial wallets...");
            }
        }

        if (App.dbPool.existsMainWallet() == 0) {
            createWalletSynchronously(genesisNode, getSettings().getGenesisNode().getTonBinDir() + ZEROSTATE + File.separator + "main-wallet", WalletVersion.master, -1L, -1L, settings.getWalletSettings().getInitialAmount(), false); //WC -1
        }

        if (App.dbPool.existsConfigWallet() == 0) {
            createWalletSynchronously(genesisNode, getSettings().getGenesisNode().getTonBinDir() + ZEROSTATE + File.separator + "config-master", WalletVersion.config, -1L, -1L, settings.getWalletSettings().getInitialAmount(), false); //WC -1
        }

        if (isNull(genesisNode.getWalletAddress())) {
            log.info("Creating validator controlling smart-contract for node {}", genesisNode.getNodeName());
            createWalletSynchronously(genesisNode, null, WalletVersion.V3R2, -1L, settings.getWalletSettings().getDefaultSubWalletId(), genesisNode.getInitialValidatorWalletAmount(), true);
        }

        while (installed < 3) {
            installed = App.dbPool.getNumberOfWalletsFromAllDBsAsync();
            Thread.sleep(200);
            log.info("creating main validator wallet, total wallets {}", installed);
        }
    }

    public void createWalletAsynchronously(Node node, String fileBaseName, WalletVersion walletVersion, long workchain, long subWalletid, BigInteger amount, boolean validatorWallet) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {
            log.info("CreateWalletEntity thread started!");
            Thread.currentThread().setName("MyLocalTon - Creating wallet - " + walletVersion.getValue() + ",wc=" + workchain + ",walledId=" + subWalletid);

            createWalletEntity(node, fileBaseName, walletVersion, workchain, subWalletid, amount, validatorWallet);
        });

        executorService.shutdown();
    }

    public void createWalletSynchronously(Node node, String fileBaseName, WalletVersion walletVersion, long workchain, long subWalletid, BigInteger amount, boolean validatorWallet) {
        createWalletEntity(node, fileBaseName, walletVersion, workchain, subWalletid, amount, validatorWallet);
    }

    public void createWalletEntity(Node node, String fileBaseName, WalletVersion walletVersion, long workchain, long subWalletid, BigInteger amount, boolean validatorWallet) {
        try {
            WalletEntity wallet;
            if (isNull(fileBaseName)) {
                wallet = createWalletWithFundsAndSmartContract(settings.getGenesisNode(), walletVersion, workchain, subWalletid, amount);
                log.debug("created wallet address {}", wallet.getHexAddress());
            } else { //read address of initially created wallet (main-wallet and config-master)
                wallet = new Fift().getWalletByBasename(node, fileBaseName);
                log.debug("read wallet address: {}", wallet.getHexAddress());
            }

            if (validatorWallet) {
                node.setWalletAddress(wallet.getWallet());
            }
        } catch (Exception e) {
            log.error("Error creating wallet! Error {} ", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    public void runBlockchainSizeMonitor() {
        monitorExecutorService = Executors.newSingleThreadScheduledExecutor();
        monitorExecutorService.scheduleWithFixedDelay(() -> {
            Thread.currentThread().setName("MyLocalTon - Blockchain Size Monitor");

            String size = MyLocalTonUtils.getDirectorySizeUsingDu(CURRENT_DIR + File.separator + MY_LOCAL_TON);
            log.debug("size {}", size);
            if (!GraphicsEnvironment.isHeadless()) {
                MainController c = fxmlLoader.getController();
                Platform.runLater(() -> c.dbSizeId.setSecondaryText(size));
            }
        }, 0L, 60L, TimeUnit.SECONDS);
    }

    public void runValidationMonitor() {
        log.info("Starting validation monitor");

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            Thread.currentThread().setName("MyLocalTon - Validation Monitor");

            if (Main.appActive.get()) {
                try {
                    long currentTime = MyLocalTonUtils.getCurrentTimeSeconds();

                    ValidationParam v = MyLocalTonUtils.getConfig(settings.getGenesisNode());
                    log.debug("validation parameters {}", v);
                    //save election ID
                    if (v.getStartValidationCycle() > YEAR_1971) {
                        settings.elections.put(v.getStartValidationCycle(), v);
                        saveSettingsToGson();
                    }

                    long electionsDelta = v.getNextElections() - v.getStartElections();

                    if (!GraphicsEnvironment.isHeadless()) {
                        mainController.drawElections();
                    }

                    log.debug("[start-end] elections [{} - {}], currentTime {}", MyLocalTonUtils.toLocal(v.getStartElections()), MyLocalTonUtils.toLocal(v.getEndElections()), MyLocalTonUtils.toLocal(currentTime));
                    log.debug("currTime > delta2, {} {}", (currentTime - v.getStartElections()), electionsDelta * 2);

                    if (((v.getStartValidationCycle() > YEAR_1971) && ((currentTime > v.getStartElections()) && (currentTime < v.getEndElections() - 10)))  // 10 sec to process
                            || ((v.getStartValidationCycle() > YEAR_1971) && ((currentTime - v.getStartElections()) > (electionsDelta * 2)))) {

                        log.info("ELECTIONS OPENED");

                        Main.inElections.set(true);

                        participateAll(v);

                        Main.inElections.set(false);

                    } else {
                        log.info("ELECTIONS CLOSED, REAPING...");
                        reapAll();
                    }

                } catch (Exception e) {
                    log.error("Error getting blockchain configuration! Error {}", e.getMessage());
                    log.error(ExceptionUtils.getStackTrace(e));
                } finally {
                    Main.inElections.set(false);
                }
                if (!GraphicsEnvironment.isHeadless()) {
                    Platform.runLater(() -> {
                        ProgressBar progress = mainController.progressValidationUpdate;
                        Timeline timeline = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(progress.progressProperty(), 0)),
                                new KeyFrame(Duration.seconds(VALIDATION_GUI_REFRESH_SECONDS), new KeyValue(progress.progressProperty(), 1))
                        );
                        timeline.setCycleCount(1);
                        timeline.play();
                    });
                }

            }
        }, 0L, VALIDATION_GUI_REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    private void participateAll(ValidationParam v) {
        for (String nodeName : settings.getActiveNodes()) {
            Node node = settings.getNodeByName(nodeName);

            if (node.getStatus().equals("ready")) {
                log.info("participates in elections {}", nodeName);
                ExecutorService nodeParticipationExecutorService = Executors.newSingleThreadExecutor();
                nodeParticipationExecutorService.execute(() -> {
                    Thread.currentThread().setName("MyLocalTon - Participation in elections by " + nodeName);
                    MyLocalTonUtils.participate(node, v);
                });
                nodeParticipationExecutorService.shutdown();
            }
        }
    }

    private void reapAll() {
        for (String nodeName : settings.getActiveNodes()) {
            Node node = settings.getNodeByName(nodeName);

            if (node.getStatus().equals("ready")) {

                ExecutorService nodeReapRewardsExecutorService = Executors.newSingleThreadExecutor();
                nodeReapRewardsExecutorService.execute(() -> {
                    Thread.currentThread().setName("MyLocalTon - Reaping rewards by " + nodeName);
                    reap(settings.getNodeByName(nodeName));

                    Platform.runLater(() -> updateReapedValuesTab(node));
                });
                nodeReapRewardsExecutorService.shutdown();
            }
        }
    }

    public void initTonlib(Node node) {
        String tonlibName;
        switch (Utils.getOS()) {
            case LINUX:
                tonlibName = "tonlibjson.so";
                break;
            case LINUX_ARM:
                tonlibName = "tonlibjson-arm.so";
                break;
            case WINDOWS:
                tonlibName = "tonlibjson.dll";
                break;
            case WINDOWS_ARM:
                tonlibName = "tonlibjson-arm.dll";
                break;
            case MAC:
                tonlibName = "tonlibjson";
                break;
            case MAC_ARM64:
                tonlibName = "tonlibjson-arm";
                break;
            case UNKNOWN:
                throw new Error("Operating system is not supported!");
            default:
                throw new IllegalArgumentException();
        }
        tonlib = Tonlib.builder()
                .pathToGlobalConfig(node.getNodeGlobalConfigLocation())
                .keystorePath(node.getTonlibKeystore().replace("\\", "/"))
                .pathToTonlibSharedLib(tonlibName)
//                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        tonlibBlockMonitor = Tonlib.builder()
                .pathToGlobalConfig(node.getNodeGlobalConfigLocation())
                .keystorePath(node.getTonlibKeystore().replace("\\", "/"))
                .pathToTonlibSharedLib(tonlibName)
//                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();
    }

    public void runBlockchainMonitor(Node node) {
        log.info("Starting node monitor");

        ExecutorService blockchainMonitorExecutorService = Executors.newSingleThreadExecutor();

        blockchainMonitorExecutorService.execute(() -> {
            Thread.currentThread().setName("MyLocalTon - Blockchain Monitor");
            ExecutorService executorService;
            while (Main.appActive.get()) {
                try {
                    executorService = Executors.newSingleThreadExecutor();
                    LiteClient liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);

                    executorService.execute(() -> {
                        Thread.currentThread().setName("MyLocalTon - Dump Block " + prevBlockSeqno.get());
                        log.debug("Get last block");
                        ResultLastBlock lastBlock = LiteClientParser.parseLast(liteClient.executeLast(node));
//                        MasterChainInfo lastBlockM = tonlib.getLast(); // todo next release

                        if (nonNull(lastBlock)) {

                            if ((!Objects.equals(prevBlockSeqno.get(), lastBlock.getSeqno())) && (lastBlock.getSeqno().compareTo(BigInteger.ZERO) != 0)) {

                                prevBlockSeqno.set(lastBlock.getSeqno());
                                log.info(lastBlock.getShortBlockSeqno());

                                if (!GraphicsEnvironment.isHeadless()) {
                                    List<ResultLastBlock> shardsInBlock = insertBlocksAndTransactions(node, lastBlock, true);
                                    updateTopInfoBarGui(shardsInBlock.size());
                                }
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
            blockchainMonitorExecutorService.shutdown();
            log.info("Blockchain Monitor has stopped working");
        });
    }

    public List<ResultLastBlock> insertBlocksAndTransactions(Node node, ResultLastBlock lastBlock, boolean updateGuiNow) {

        insertBlockEntity(lastBlock);

        if (updateGuiNow) {
            updateBlocksTabGui(lastBlock);
        }

        List<ResultLastBlock> shardsInBlock = LiteClient.getInstance(LiteClientEnum.GLOBAL).getShardsFromBlock(node, lastBlock); // txs from basechain shards

        for (ResultLastBlock shard : shardsInBlock) {
            log.info(shard.getShortBlockSeqno());
            if (shard.getSeqno().compareTo(BigInteger.ZERO) != 0) {
                insertBlockEntity(shard);
                if (updateGuiNow) {
                    updateBlocksTabGui(shard);
                }
            }
        }

        dumpBlockTransactions(node, lastBlock, updateGuiNow); // txs from master-chain

        shardsInBlock.stream().filter(b -> (b.getSeqno().compareTo(BigInteger.ZERO) != 0)).forEach(shard -> dumpBlockTransactions(node, shard, updateGuiNow)); // txs from shards

        return shardsInBlock;
    }

    public void dumpBlockTransactions(Node node, ResultLastBlock lastBlock, boolean updateGuiNow) {

        LiteClient liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);
        List<ResultListBlockTransactions> txs = LiteClientParser.parseListBlockTrans(liteClient.executeListblocktrans(node, lastBlock, 0));
        log.debug("found {} transactions in block {}", txs.size(), lastBlock.getShortBlockSeqno());

        for (ResultListBlockTransactions tx : txs) {
            Transaction txDetails = LiteClientParser.parseDumpTrans(liteClient.executeDumptrans(node, lastBlock, tx), settings.getUiSettings().isShowBodyInMessage());
            if (nonNull(txDetails)) {

                List<TxEntity> txEntities = extractTxsAndMsgs(lastBlock, tx, txDetails);

                txEntities.forEach(App.dbPool::insertTx);

                detectNewAccount(lastBlock, tx, txDetails);

                if (updateGuiNow) {
                    updateTxTabGui(lastBlock, tx, txDetails, txEntities);
                }
            }
        }
    }

    private void detectNewAccount(ResultLastBlock lastBlock, ResultListBlockTransactions tx, Transaction txDetails) {
        if (
                (txDetails.getOrigStatus().equals(NONEXISTENT) && txDetails.getEndStatus().equals(UNINITIALIZED)) ||
                        (txDetails.getOrigStatus().equals(UNINITIALIZED) && txDetails.getEndStatus().equals(UNINITIALIZED)) ||
                        (txDetails.getOrigStatus().equals(UNINITIALIZED) && txDetails.getEndStatus().equals(ACTIVE))
        ) {
            log.info("New account detected! origStatus {}, endStatus {}, address {}, txSeqno {}, txLt {}, block {}",
                    txDetails.getOrigStatus(), txDetails.getEndStatus(), txDetails.getAccountAddr(), tx.getTxSeqno(), tx.getLt(), lastBlock.getShortBlockSeqno());

            try {

                WalletEntity foundWallet = App.dbPool.findWallet(WalletPk.builder()
                        .wc(lastBlock.getWc())
                        .hexAddress(txDetails.getAccountAddr().toUpperCase())
                        .build());

                if (isNull(foundWallet)) {
                    log.info("insertNewAccountEntity new inserting {}", txDetails.getAccountAddr().toUpperCase());
                    Address address = Address.of(lastBlock.getWc() + ":" + txDetails.getAccountAddr().toUpperCase());
//
                    WalletAddress walletAddress = WalletAddress.builder()
                            .wc(lastBlock.getWc())
                            .hexWalletAddress(txDetails.getAccountAddr().toUpperCase())
                            .fullWalletAddress(lastBlock.getWc() + ":" + txDetails.getAccountAddr().toUpperCase())
                            .bounceableAddressBase64url(address.toString(true, true, true))
                            .nonBounceableAddressBase64Url(address.toString(true, true, false))
                            .bounceableAddressBase64(address.toString(true, false, true))
                            .nonBounceableAddressBase64(address.toString(true, false, true))
                            .build();

                    WalletEntity walletEntity = WalletEntity.builder()
                            .wc(walletAddress.getWc())
                            .hexAddress(walletAddress.getHexWalletAddress().toUpperCase())
                            .wallet(walletAddress)
                            .createdAt(txDetails.getNow())
                            .build();

                    App.dbPool.insertWallet(walletEntity);

                    log.info("insertNewAccountEntity new inserted {}", txDetails.getAccountAddr().toUpperCase());
                }
            } catch (Throwable e) {
                log.error("Error executing insertNewAccountEntity: {}", e.getMessage());
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    private void updateAccountsTabGui(WalletEntity walletEntity) {
        if (isNull(walletEntity) || isNull(walletEntity.getAccountState())) {
            return;
        }
        log.debug("updateAccountsTabGui, wallet account addr {}, state {}", walletEntity.getHexAddress(), walletEntity.getAccountState());

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
                    (accountRow.lookup("#accRowBorderPane")).getStyleClass().add("row-pane-gray");
                    (accountRow.lookup("#hBoxDeletetn")).getStyleClass().add("background-acc-delete-button-gray");
                    (accountRow.lookup("#hBoxSendBtn")).getStyleClass().add("background-acc-send-button-gray");
                }

                showInGuiOnlyUniqueAccounts(walletEntity, c, accountRow);
            });
        }
    }

    private void emitResultMessage(WalletEntity walletEntity) {
        if (nonNull(walletEntity)) {
            if (WalletVersion.V1R1.equals(walletEntity.getWalletVersion())) {
                Platform.runLater(() -> {
                    emit(new CustomNotificationEvent(CustomEvent.Type.SUCCESS, "Wallet " + walletEntity.getFullAddress() + " created", 3));
                });
            } else if (walletEntity.getSeqno() != -1L) {
                Platform.runLater(() -> {
                    emit(new CustomNotificationEvent(CustomEvent.Type.SUCCESS, "Wallet " + walletEntity.getFullAddress() + " created", 3));
                });
            }
        } else {
            Platform.runLater(() -> {
                emit(new CustomNotificationEvent(CustomEvent.Type.ERROR, "Error creating wallet. See logs for details.", 4));
            });
        }
    }

    private void showInGuiOnlyUniqueAccounts(WalletEntity walletEntity, MainController c, javafx.scene.Node accountRow) {
        if (isNull(concurrentAccountsHashMap.get(walletEntity.getWallet().getFullWalletAddress().toUpperCase()))) {
            log.debug("add to gui list 1 new account {}", walletEntity.getWallet().getFullWalletAddress());

            //show in gui only unique accounts. some might come from: scroll event, blockchain monitor or manual creation
            if (concurrentAccountsHashMap.size() > 100) {
                concurrentAccountsHashMap.keySet().removeAll(Arrays.asList(concurrentAccountsHashMap.keySet().toArray()).subList(concurrentAccountsHashMap.size() / 2, concurrentAccountsHashMap.size())); // truncate
            }

            concurrentAccountsHashMap.put(walletEntity.getWallet().getFullWalletAddress().toUpperCase(), 1L);

            ((Label) accountRow.lookup("#hexAddrLabel")).setText(MyLocalTonUtils.getNodeNameByWalletAddress(walletEntity.getWallet().getFullWalletAddress().toUpperCase()) + "Hex:");

            populateAccountRowWithData(walletEntity, accountRow, "--------------------");

            log.debug("add to gui list 2 new account {}", walletEntity.getWallet().getFullWalletAddress());

            int size = c.accountsvboxid.getItems().size();

            if (size > accountsScrollBarHighWaterMark.get()) {
                c.accountsvboxid.getItems().remove(size - 1);
            }

            c.accountsvboxid.getItems().add(0, accountRow);

            log.debug("accounts gui size {}", c.accountsvboxid.getItems().size());

        } else {
            log.debug("update gui list with account {}", walletEntity.getWallet().getFullWalletAddress());
            if (nonNull(walletEntity.getAccountState())) {
                log.debug("update account details in gui {}, {}", walletEntity.getFullAddress(), walletEntity.getAccountState());
                for (javafx.scene.Node node : c.accountsvboxid.getItems()) {
                    if (((Label) node.lookup("#hexAddr")).getText().equals(walletEntity.getFullAddress().toUpperCase())) {
                        populateAccountRowWithData(walletEntity, node, "--------------------");
                    }
                }
            }
        }
    }

    public void populateAccountRowWithData(WalletEntity walletEntity, javafx.scene.Node accountRow, String
            searchFor) {
        try {
            if (nonNull(walletEntity.getWallet())) {
                ((Label) accountRow.lookup("#hexAddrLabel")).setText(MyLocalTonUtils.getNodeNameByWalletAddress(walletEntity.getWallet().getFullWalletAddress().toUpperCase()) + "Hex:");
            } else {
                ((Label) accountRow.lookup("#hexAddrLabel")).setText("Hex:");
            }

            ((Label) accountRow.lookup("#hexAddr")).setText(walletEntity.getWallet().getFullWalletAddress().toUpperCase());
            if (((Label) accountRow.lookup("#hexAddr")).getText().contains(searchFor)) {
                accountRow.lookup("#hexAddr").setStyle(accountRow.lookup("#hexAddr").getStyle() + FOUND_COLOR_HIHGLIGHT);
            }

            ((Label) accountRow.lookup("#b64Addr")).setText(walletEntity.getWallet().getBounceableAddressBase64());
            ((Label) accountRow.lookup("#b64urlAddr")).setText(walletEntity.getWallet().getBounceableAddressBase64url());
            ((Label) accountRow.lookup("#nb64Addr")).setText(walletEntity.getWallet().getNonBounceableAddressBase64());
            ((Label) accountRow.lookup("#nb64urlAddr")).setText(walletEntity.getWallet().getNonBounceableAddressBase64Url());

            ((Label) accountRow.lookup("#createdat")).setText(MyLocalTonUtils.toLocal((walletEntity.getCreatedAt())));

            if (isNull(walletEntity.getWalletVersion())) {
                ((Label) accountRow.lookup("#type")).setText("Unknown");
            } else {
                ((Label) accountRow.lookup("#type")).setText(walletEntity.getWalletVersion().getValue());
            }

            if (nonNull(walletEntity.getWalletVersion()) && (walletEntity.getWalletVersion().getValue().contains("V3") || walletEntity.getWalletVersion().getValue().contains("V4"))) {
                accountRow.lookup("#walledId").setVisible(true);
                ((Label) accountRow.lookup("#walledId")).setText("Wallet ID " + walletEntity.getWallet().getSubWalletId());
            } else { //wallets V1 and V2
                accountRow.lookup("#walledId").setVisible(false);
                ((Label) accountRow.lookup("#walledId")).setText("-1");
            }

            if (walletEntity.getSeqno() >= 0 && (nonNull(walletEntity.getWalletVersion()) && !walletEntity.getWalletVersion().equals(WalletVersion.config))) {
                accountRow.lookup("#seqno").setVisible(true);
                ((Label) accountRow.lookup("#seqno")).setText("Seqno " + walletEntity.getSeqno());
            } else {
                accountRow.lookup("#seqno").setVisible(false);
                ((Label) accountRow.lookup("#seqno")).setText("Seqno -1");
            }

//            String value = walletEntity.getAccountState().getBalance();
//            String formattedBalance2 = String.format("%,.9f", value);
            BigDecimal balance = new BigDecimal(walletEntity.getAccountState().getBalance());
            String formattedBalance = String.format("%,.9f", balance.divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING));

            ((Label) accountRow.lookup("#balance")).setText(formattedBalance);

            String status = StringUtils.isEmpty(walletEntity.getAccountStatus()) ? "" : StringUtils.capitalize(walletEntity.getAccountStatus());
            ((Label) accountRow.lookup("#status")).setText(status);

            if (settings.getConfigSmcAddrHex().contains(walletEntity.getHexAddress())
                    || settings.getMainWalletAddrFull().contains(walletEntity.getHexAddress())
                    || isNull(walletEntity.getWallet().getPrivateKeyHex())
            ) {
                accountRow.lookup("#walletDeleteBtn").setDisable(true);
                (accountRow.lookup("#hBoxDeletetn")).getStyleClass().clear();
            }
            if (status.equals(UNINITIALIZED)
                    || status.equals(FROZEN)
                    || settings.getConfigSmcAddrHex().contains(walletEntity.getHexAddress().toUpperCase())
                    || isNull(walletEntity.getWallet().getPrivateKeyHex())
                //|| isNull(walletEntity.getWallet().getPrivateKeyLocation())
            ) {
                accountRow.lookup("#accSendBtn").setDisable(true);
                (accountRow.lookup("#hBoxSendBtn")).getStyleClass().clear();
            } else {
                accountRow.lookup("#accSendBtn").setDisable(false);
                (accountRow.lookup("#hBoxSendBtn")).getStyleClass().clear();
            }
        } catch (Exception e) {
            log.error("error updating accounts GUI: " + e.getMessage());
        }
    }

    private void updateTxTabGui(ResultLastBlock lastBlock, ResultListBlockTransactions tx, Transaction
            txDetails, List<TxEntity> txEntities) {

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
                        (txRow.lookup("#txRowBorderPane")).getStyleClass().add("row-pane-gray");
                    }

                    showInGuiOnlyUniqueTxs(lastBlock, tx, txDetails, c, txE, txRow);  //show in gui only unique values. some might come from scroll event
                }
            });
        }
    }

    private void showInGuiOnlyUniqueTxs(ResultLastBlock lastBlock, ResultListBlockTransactions tx, Transaction
            txDetails, MainController c, TxEntity txE, javafx.scene.Node
                                                txRow) {
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
            txRow.lookup("#block").setStyle(txRow.lookup("#block").getStyle() + FOUND_COLOR_HIHGLIGHT);
        }
        ((Label) txRow.lookup("#typeTx")).setText(txEntity.getTypeTx());
        ((Label) txRow.lookup("#typeMsg")).setText(txEntity.getTypeMsg());

        ((Label) txRow.lookup("#status")).setText(txEntity.getStatus());
        ((Label) txRow.lookup("#txidHidden")).setText(txEntity.getTxHash());
        ((Label) txRow.lookup("#txAccAddrHidden")).setText(txEntity.getAccountAddress());
        ((Label) txRow.lookup("#txLt")).setText(String.valueOf(txEntity.getTxLt()));

        ((Label) txRow.lookup("#txid")).setText(txEntity.getTxHash().substring(0, 8) + "..." + txEntity.getTxHash().substring(56, 64));
        if (((Label) txRow.lookup("#txidHidden")).getText().equals(searchFor)) {
            txRow.lookup("#txid").setStyle(txRow.lookup("#txid").getStyle() + FOUND_COLOR_HIHGLIGHT);
        }
        ((Label) txRow.lookup("#from")).setText(txEntity.getFrom().getAddr());
        if (searchFor.length() >= 64) {
            if (((Label) txRow.lookup("#from")).getText().contains(StringUtils.substring(searchFor, 4, -2))) {
                txRow.lookup("#from").setStyle(txRow.lookup("#from").getStyle() + FOUND_COLOR_HIHGLIGHT);
            }
        }
        if (((Label) txRow.lookup("#from")).getText().equals(searchFor)) {
            txRow.lookup("#from").setStyle(txRow.lookup("#from").getStyle() + FOUND_COLOR_HIHGLIGHT);
        }

        ((Label) txRow.lookup("#to")).setText(txEntity.getTo().getAddr());
        if (searchFor.length() >= 64) {
            if (((Label) txRow.lookup("#to")).getText().contains(StringUtils.substring(searchFor, 4, -2))) {
                txRow.lookup("#to").setStyle(txRow.lookup("#to").getStyle() + FOUND_COLOR_HIHGLIGHT);
            }
        }
        if (((Label) txRow.lookup("#to")).getText().equals(searchFor)) {
            txRow.lookup("#to").setStyle(txRow.lookup("#to").getStyle() + FOUND_COLOR_HIHGLIGHT);
        }
        ((Label) txRow.lookup("#amount")).setText(txEntity.getAmount().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING).toPlainString());

        ((Label) txRow.lookup("#fees")).setText(txEntity.getTx().getTotalFees().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING).toPlainString());
        ((Label) txRow.lookup("#time")).setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(new Timestamp(txEntity.getTx().getNow() * 1000).getTime())));

        showButtonWithMessage(txRow, txEntity);
    }

    public void populateTxRowWithData(String shortBlockSeqno, ResultListBlockTransactions tx, Transaction
            txDetails, javafx.scene.Node txRow, TxEntity txEntity) {

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
        ((Label) txRow.lookup("#amount")).setText(txEntity.getAmount().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING).toPlainString());

        ((Label) txRow.lookup("#fees")).setText(txDetails.getTotalFees().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING).toPlainString());
        ((Label) txRow.lookup("#time")).setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(new Timestamp(txDetails.getNow() * 1000).getTime())));

        showButtonWithMessage(txRow, txEntity);
    }

    private void showButtonWithMessage(javafx.scene.Node txRow, TxEntity txEntity) {

        String msg = null;
        if (txEntity.getTypeTx().contains("Message") && !txEntity.getTypeMsg().contains("External In")) {
            if (!(txEntity.getTx().getInMsg().getBody().getCells().isEmpty()) && (txEntity.getTx().getInMsg().getValue().getToncoins().compareTo(BigDecimal.ZERO) > 0) &&
                    !txEntity.getTx().getInMsg().getBody().getCells().get(0).equals("FFFFFFFF")) {

                msg = txEntity.getTx().getInMsg().getBody().getCells().stream().map(c -> {
                    try {
                        return new String(Hex.decodeHex(c.toCharArray()));
                    } catch (DecoderException e) {
                        log.debug("Cannot convert hex msg to string");
                        return "      Cannot convert hex msg to string";
                    }
                }).collect(Collectors.joining());

            } else if ((!txEntity.getTx().getOutMsgs().isEmpty() && !txEntity.getTx().getOutMsgs().get(0).getBody().getCells().isEmpty())) {
                if (txEntity.getTx().getOutMsgs().get(0).getValue().getToncoins().compareTo(BigDecimal.ZERO) > 0 && !txEntity.getTx().getOutMsgs().get(0).getBody().getCells().get(0).equals("FFFFFFFF")) {

                    msg = txEntity.getTx().getOutMsgs().get(0).getBody().getCells().stream().map(c -> {
                        try {
                            return new String(Hex.decodeHex(c.toCharArray()));
                        } catch (DecoderException e) {
                            log.debug("Cannot convert hex msg to string");
                            return "     Cannot convert hex msg to string";
                        }
                    }).collect(Collectors.joining());
                }
            }
        }
        if (StringUtils.isNotEmpty(msg) && msg.length() > 4) {
            msg = msg.substring(4);

            if (StringUtils.isAlphanumericSpace(msg) || StringUtils.isAsciiPrintable(msg)) {
                txRow.lookup("#txMsgBtn").setVisible(true);
                txRow.lookup("#txMsgBtn").setUserData(msg);

                txRow.lookup("#txMsgBtn").setOnMouseClicked(mouseEvent -> {
                    log.info("in msg btn clicked on block {}, {}", ((Label) txRow.lookup("#block")).getText(), txRow.lookup("#txMsgBtn").getUserData());
                    mainController.showMessage(String.valueOf(txRow.lookup("#txMsgBtn").getUserData()));
                });
            }
        }
    }

    public List<TxEntity> extractTxsAndMsgs(ResultLastBlock lastBlock, ResultListBlockTransactions tx, Transaction
            txDetails) {
        List<TxEntity> txEntity = new ArrayList<>();
        LiteClientAddress to;
        LiteClientAddress from;
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
                from = LiteClientAddress.builder().wc(lastBlock.getWc()).addr(txDetails.getAccountAddr()).build();

                to = LiteClientAddress.builder().addr("").build();
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
                    from = LiteClientAddress.builder().wc(from.getWc()).addr(EXTERNAL).build();
                } else if (Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {
                    to = LiteClientAddress.builder().wc(to.getWc()).addr(EXTERNAL).build();
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
                        from = LiteClientAddress.builder().wc(from.getWc()).addr(EXTERNAL).build();
                        ok = true;
                    } else if (Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {
                        to = LiteClientAddress.builder().wc(to.getWc()).addr(EXTERNAL).build();
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
                    from = LiteClientAddress.builder().wc(from.getWc()).addr(EXTERNAL).build();
                    ok = true;
                } else if (Strings.isEmpty(to.getAddr()) && !Strings.isEmpty(from.getAddr())) {
                    to = LiteClientAddress.builder().wc(to.getWc()).addr(EXTERNAL).build();
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

    private void updateTopInfoBarGui(int shardsNum) {

        MainController c = fxmlLoader.getController();
        Platform.runLater(() -> {
            try {
                // update GUI
                c.shardsNum.setSecondaryText(String.valueOf(shardsNum));
                c.liteClientInfo.setSecondaryText(String.format("%s:%s", settings.getGenesisNode().getPublicIp(), settings.getGenesisNode().getLiteServerPort()));
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
                    c.currentBlockNum.setSecondaryText(finalLastBlock.getSeqno().toString());
                }

                if (Boolean.TRUE.equals(autoScroll)) {

                    // update blocks row
                    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("blockrow.fxml"));
                    javafx.scene.Node blockRow;
                    try {
                        blockRow = fxmlLoader.load();
                    } catch (IOException e) {
                        log.error("error loading blockrow.fxml file, {}", e.getMessage());
                        return;
                    }
                    if (finalLastBlock.getWc() == -1L) {
                        (blockRow.lookup("#blockRowBorderPane")).getStyleClass().add("row-pane-gray");
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
            emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_BLOCKS, 0));
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
                    blockRow.getStyleClass().add("row-pane-gray");
                }
                log.debug("adding block to found gui {} roothash {}", block.getSeqno(), block.getRoothash());

                blockRows.add(blockRow);

            } catch (IOException e) {
                log.error("error loading blockrow.fxml file, {}", e.getMessage());
                return;
            }
        }

        log.debug("blockRows.size  {}", blockRows.size());

        c.foundBlockslistviewid.getItems().addAll(blockRows);
        emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_BLOCKS, blockRows.size()));

    }

    public void showFoundTxsInGui(JFXListView<javafx.scene.Node> listView, List<TxEntity> foundTxs, String
            searchFor, String accountAddr) {

        MainController c = fxmlLoader.getController();

        if (foundTxs.isEmpty()) {

            if (StringUtils.isNotEmpty(accountAddr)) {
                emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_ACCOUNTS_TXS, 0, accountAddr));
            } else {
                emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_TXS, 0));
            }
            return;
        }

        ObservableList<javafx.scene.Node> txRows = FXCollections.observableArrayList();

        for (TxEntity tx : foundTxs) {
            try {
                FXMLLoader fxmlLoaderRow = new FXMLLoader(App.class.getResource("txrow.fxml"));
                javafx.scene.Node txRow;

                txRow = fxmlLoaderRow.load();

                populateTxRowWithData(txRow, tx, searchFor);

                if (tx.getWc() == -1L) {
                    txRow.getStyleClass().add("row-pane-gray");
                }
                log.debug("adding tx to found gui {} roothash {}", tx.getShortBlock(), tx.getTxHash());

                txRows.add(txRow);

            } catch (IOException e) {
                log.error("error loading txrow.fxml file, {}", e.getMessage());
                return;
            }
        }

        log.debug("txRows.size {}", txRows.size());

        if (StringUtils.isNotEmpty(accountAddr)) {
            emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_ACCOUNTS_TXS, txRows.size(), accountAddr));
            listView.getItems().addAll(txRows);
        } else {
            emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_TXS, txRows.size()));
            c.foundTxsvboxid.getItems().addAll(txRows);
        }

    }

    public void showFoundAccountsInGui(List<WalletEntity> foundAccounts, String searchFor) {

        MainController c = fxmlLoader.getController();

        if (foundAccounts.isEmpty()) {
            emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_ACCOUNTS, 0));
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
                    accountRow.getStyleClass().add("row-pane-gray");

                }
                log.debug("adding account to found gui {}", account.getFullAddress());

                accountRows.add(accountRow);

            } catch (IOException e) {
                log.error("error loading accountrow.fxml file, {}", e.getMessage());
                return;
            }
        }

        log.debug("accRows.size  {}", accountRows.size());

        c.foundAccountsvboxid.getItems().addAll(accountRows);
        emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_ACCOUNTS, accountRows.size()));
    }

    public void populateBlockRowWithData(ResultLastBlock finalLastBlock, javafx.scene.Node blockRow, String
            searchFor) {

        ((Label) blockRow.lookup("#wc")).setText(finalLastBlock.getWc().toString());
        ((Label) blockRow.lookup("#shard")).setText(finalLastBlock.getShard());
        ((Label) blockRow.lookup("#seqno")).setText(finalLastBlock.getSeqno().toString());
        if (((Label) blockRow.lookup("#seqno")).getText().equals(searchFor)) {
            blockRow.lookup("#seqno").setStyle(blockRow.lookup("#seqno").getStyle() + FOUND_COLOR_HIHGLIGHT);
        }
        ((Label) blockRow.lookup("#filehash")).setText(finalLastBlock.getFileHash());
        ((Label) blockRow.lookup("#roothash")).setText(finalLastBlock.getRootHash());
        if (((Label) blockRow.lookup("#filehash")).getText().equals(searchFor)) {
            blockRow.lookup("#filehash").setStyle(blockRow.lookup("#filehash").getStyle() + FOUND_COLOR_HIHGLIGHT);
        }
        if (((Label) blockRow.lookup("#roothash")).getText().equals(searchFor)) {
            blockRow.lookup("#roothash").setStyle(blockRow.lookup("#roothash").getStyle() + FOUND_COLOR_HIHGLIGHT);
        }
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");

        ((Label) blockRow.lookup("#createdatDate")).setText(formatterDate.format(new Date(new Timestamp(finalLastBlock.getCreatedAt() * 1000).getTime())));
        ((Label) blockRow.lookup("#createdatTime")).setText(formatterTime.format(new Date(new Timestamp(finalLastBlock.getCreatedAt() * 1000).getTime())));

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

        App.dbPool.insertBlock(block);
    }

    public void runAccountsMonitor() {
//        Tonlib tonlib = Tonlib.builder()
//                .pathToGlobalConfig(MyLocalTon.getInstance().getSettings().getGenesisNode().getNodeGlobalConfigLocation())
//                .keystorePath(MyLocalTon.getInstance().getSettings().getGenesisNode().getTonlibKeystore().replace("\\", "/"))
//                .build();
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            Thread.currentThread().setName("MyLocalTon - Accounts Monitor");
            try {
                for (WalletEntity wallet : App.dbPool.getAllWallets()) {
                    if (Main.appActive.get()) {
                        Address address = Address.of(wallet.getWc() + ":" + wallet.getHexAddress());
                        RawAccountState accountState = tonlib.getRawAccountState(address);
                        WalletVersion walletVersion = MyLocalTonUtils.detectWalletVersion(accountState.getCode(), address);

                        long subWalletId = -1;
                        long seqno = -1;

                        try {
                            seqno = tonlib.getSeqno(address);
                            subWalletId = tonlib.getAccountState(address).getAccount_state().getWallet_id();
                            if (subWalletId == 0) {
                                RunResult resultSubWalletId = tonlib.runMethod(address, "get_subwallet_id");
                                if (resultSubWalletId.getExit_code() == 0) {
                                    TvmStackEntryNumber resultSubWalletIdNum = (TvmStackEntryNumber) resultSubWalletId.getStack().get(0);
                                    subWalletId = resultSubWalletIdNum.getNumber().longValue();
                                } else {
                                    subWalletId = -1;
                                }
                            }
                        } catch (Throwable ignored) {
                            subWalletId = -1;
                        }

                        log.debug("runAccountsMonitor: {}, {}, {}, {}", walletVersion, seqno, subWalletId, accountState.getBalance());

                        App.dbPool.updateWalletStateAndSeqno(wallet, accountState, seqno, walletVersion);

                        wallet.setAccountState(accountState);
                        wallet.setSeqno(seqno);
                        wallet.getWallet().setSubWalletId(subWalletId);
                        wallet.setWalletVersion(walletVersion);

                        if (!GraphicsEnvironment.isHeadless()) {
                            updateAccountsTabGui(wallet);
                        }
                    }
                }
            } catch (Throwable e) {
                log.error("Error in runAccountsMonitor(), " + e.getMessage());
            }
        }, 0L, 6L, TimeUnit.SECONDS);
    }

    public void reap(Node node) {
        try {

            if (isNull(node.getWalletAddress())) {
                log.error("Reaping rewards. {} wallet is not present.", node.getNodeName());
                return;
            }

            ResultComputeReturnStake result = LiteClientParser.parseRunMethodComputeReturnStake(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeComputeReturnedStake(node, settings.getElectorSmcAddrHex(), node.getWalletAddress().getHexWalletAddress()));

            if (result.getStake().compareTo(BigDecimal.ZERO) > 0) {
                log.info("Reaping rewards. {} reward size is {}, send request for stake recovery", node.getNodeName(), result.getStake());

                // create recover-query.boc
                new Fift().createRecoverStake(node);

                // send stake and validator-query.boc to elector
                SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                        .executionNode(node)
                        .fromWallet(node.getWalletAddress())
                        .fromWalletVersion(WalletVersion.V3R2) // default validator wallet type
                        .fromSubWalletId(settings.getWalletSettings().getDefaultSubWalletId())
                        .destAddr(settings.getElectorSmcAddrHex())
                        .amount(BigInteger.valueOf(ONE_BLN)) //(BigDecimal.valueOf(1L))
                        .bocLocation(node.getTonBinDir() + "recover-query.boc")
                        .build();

                new MyWallet().sendTonCoins(sendToncoinsParam);

                // Basic rewards statistics. Better to fetch from the DB actual values, since recover stake may fail e.g.

                node.setLastRewardCollected(result.getStake());
                node.setTotalRewardsCollected(node.getTotalRewardsCollected().add(result.getStake()));

                //returned stake - 10001 + 1
                // big time todo review
                node.setLastPureRewardCollected(result.getStake().multiply(BigDecimal.valueOf(ONE_BLN)).subtract(new BigDecimal(node.getDefaultValidatorStake()).subtract(BigDecimal.valueOf(ONE_BLN)).multiply(BigDecimal.valueOf(ONE_BLN))));

                node.setTotalPureRewardsCollected(node.getTotalPureRewardsCollected().add(node.getLastPureRewardCollected()));
                node.setElectionsRipped(node.getElectionsRipped().add(BigDecimal.ONE));
                node.setAvgPureRewardCollected(node.getTotalPureRewardsCollected().divide(node.getElectionsRipped(), 9, RoundingMode.CEILING));

                saveSettingsToGson();
            } else {
                log.info("Reaping rewards. {} reward size is {}, nothing to reap.", node.getNodeName(), result.getStake());
            }
        } catch (Exception e) {
            log.error("Error reaping rewards. Error {}", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private void updateReapedValuesTab(Node node) {
        try {
            log.debug("{} updating reaped values {}", node.getNodeName(), String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));

            switch (node.getNodeName()) {
                case "genesis":
                    mainController.validator1totalCollected.setText(String.format("%,.9f", node.getTotalRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator1LastCollected.setText(String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator1TotalRewardsPure.setText(String.format("%,.9f", node.getTotalPureRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator1LastRewardPure.setText(String.format("%,.9f", node.getLastPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator1AvgPureReward.setText(String.format("%,.9f", node.getAvgPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.participatedInElections1.setText(node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
                    break;
                case "node2":
                    mainController.validator2totalCollected.setText(String.format("%,.9f", node.getTotalRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator2LastCollected.setText(String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator2TotalRewardsPure.setText(String.format("%,.9f", node.getTotalPureRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator2LastRewardPure.setText(String.format("%,.9f", node.getLastPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator2AvgPureReward.setText(String.format("%,.9f", node.getAvgPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.participatedInElections2.setText(node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
                    break;
                case "node3":
                    mainController.validator3totalCollected.setText(String.format("%,.9f", node.getTotalRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator3LastCollected.setText(String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator3TotalRewardsPure.setText(String.format("%,.9f", node.getTotalPureRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator3LastRewardPure.setText(String.format("%,.9f", node.getLastPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator3AvgPureReward.setText(String.format("%,.9f", node.getAvgPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.participatedInElections3.setText(node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
                    break;
                case "node4":
                    mainController.validator4totalCollected.setText(String.format("%,.9f", node.getTotalRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator4LastCollected.setText(String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator4TotalRewardsPure.setText(String.format("%,.9f", node.getTotalPureRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator4LastRewardPure.setText(String.format("%,.9f", node.getLastPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator4AvgPureReward.setText(String.format("%,.9f", node.getAvgPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.participatedInElections4.setText(node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
                    break;
                case "node5":
                    mainController.validator5totalCollected.setText(String.format("%,.9f", node.getTotalRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator5LastCollected.setText(String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator5TotalRewardsPure.setText(String.format("%,.9f", node.getTotalPureRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator5LastRewardPure.setText(String.format("%,.9f", node.getLastPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator5AvgPureReward.setText(String.format("%,.9f", node.getAvgPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.participatedInElections5.setText(node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
                    break;
                case "node6":
                    mainController.validator6totalCollected.setText(String.format("%,.9f", node.getTotalRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator6LastCollected.setText(String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator6TotalRewardsPure.setText(String.format("%,.9f", node.getTotalPureRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator6LastRewardPure.setText(String.format("%,.9f", node.getLastPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator6AvgPureReward.setText(String.format("%,.9f", node.getAvgPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.participatedInElections6.setText(node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
                    break;
                case "node7":
                    mainController.validator7totalCollected.setText(String.format("%,.9f", node.getTotalRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator7LastCollected.setText(String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator7TotalRewardsPure.setText(String.format("%,.9f", node.getTotalPureRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator7LastRewardPure.setText(String.format("%,.9f", node.getLastPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.validator7AvgPureReward.setText(String.format("%,.9f", node.getAvgPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                    mainController.participatedInElections7.setText(node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
                    break;
                default:
                    throw new Error("Unknown node name");
            }
        } catch (Throwable te) {
            log.error("Error updating validation rewards: " + te.getMessage());
        }
    }

    public void createValidatorPubKeyAndAdnlAddress(Node node, long electionId) throws Exception {
        log.debug("{} creating validator PubKey and ADNL address", node.getNodeName());
        long electionEnd = electionId + 600; // was YEAR

        createSigningKeyForValidation(node, electionId, electionEnd);
        createAdnlKeyForValidation(node, node.getValidationSigningKey(), electionEnd);

        node.setValidationPubKeyAndAdnlCreated(true); // not used

        saveSettingsToGson();
    }

    void createSigningKeyForValidation(Node node, long electionId, long electionEnd) throws
            ExecutionException, InterruptedException {
        ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();

        String signingKey = validatorEngineConsole.generateNewNodeKey(node);
        String signingPubKey = validatorEngineConsole.exportPubKey(node, signingKey);

        log.debug("{} signingKey {}, new signingPubKey {} for current elections", node.getNodeName(), signingKey, signingPubKey);

        node.setValidationSigningKey(signingKey);
        node.setValidationSigningPubKey(signingPubKey);
        validatorEngineConsole.addPermKey(node, signingKey, electionId, electionEnd);
        validatorEngineConsole.addTempKey(node, signingKey, electionEnd);
    }

    void createAdnlKeyForValidation(Node node, String signingKey, long electionEnd) throws
            ExecutionException, InterruptedException {
        ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();
        String adnlKey = validatorEngineConsole.generateNewNodeKey(node);
        log.debug("{} new adnlKey {} for current elections", node.getNodeName(), adnlKey);

        node.setPrevValidationAndlKey(node.getValidationAndlKey());
        node.setValidationAndlKey(adnlKey); // shown on validator tab as ADNL address

        validatorEngineConsole.addAdnl(node, adnlKey);
        validatorEngineConsole.addValidatorAddr(node, signingKey, adnlKey, electionEnd);
        //validatorEngineConsole.getStats(node);
    }

    public void createFullnode(Node node, boolean enableLiteServer, boolean start) throws Exception {
        if (Files.exists(Paths.get(node.getTonDbArchiveDir()))) {
            log.info("{} already created, just start it", node.getNodeName());
            if (start) {
                new ValidatorEngine().startValidator(node, node.getNodeGlobalConfigLocation());
            }
            return;
        }
        log.info("creating new fullnode {}", node.getNodeName());
        node.extractBinaries();

        ValidatorEngine validatorEngine = new ValidatorEngine();
        validatorEngine.generateValidatorKeys(node, false);
        validatorEngine.initFullnode(node, settings.getGenesisNode().getNodeGlobalConfigLocation());

        if (!node.getNodeName().contains("genesis")) {
            node.setFlag("cloning");
            if (SystemUtils.IS_OS_WINDOWS) {
//               on Windows locked files cannot be copied. As an option, we can shut down genesis node, copy the files and start it again.
//               but there is a connection issue with this on Windows.
//               As an alternative, we can exclude locked files (LOCK), that's why Utils.copyDirectory is used.

                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbStaticDir()), new File(node.getTonDbStaticDir()));
//              copy ignoring locked files
                MyLocalTonUtils.copyDirectory(settings.getGenesisNode().getTonDbArchiveDir(), node.getTonDbArchiveDir()); // if only this dir, then fails with "Check `ptr && "deferencing null Ref"` failed"
                MyLocalTonUtils.copyDirectory(settings.getGenesisNode().getTonDbCellDbDir(), node.getTonDbCellDbDir()); // with archive and celldb only - fails with - [!shardclient][&masterchain_block_handle_->inited_next_left()]
                MyLocalTonUtils.copyDirectory(settings.getGenesisNode().getTonDbFilesDir(), node.getTonDbFilesDir());
                MyLocalTonUtils.copyDirectory(settings.getGenesisNode().getTonDbCatchainsDir(), node.getTonDbCatchainsDir()); // [!shardclient][&masterchain_block_handle_->inited_next_left()]
            } else {
                // speed up synchronization - copy archive, catchains, files, state and celldb directories
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbStaticDir()), new File(node.getTonDbStaticDir()));
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbArchiveDir()), new File(node.getTonDbArchiveDir()));
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbCatchainsDir()), new File(node.getTonDbCatchainsDir()));
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbCellDbDir()), new File(node.getTonDbCellDbDir()));
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbFilesDir()), new File(node.getTonDbFilesDir()));
            }
        }

        if (enableLiteServer) {
            validatorEngine.enableLiteServer(node, node.getNodeGlobalConfigLocation(), false);
        }

        if (start) {
            validatorEngine.startValidator(node, node.getNodeGlobalConfigLocation());
        }
    }
}