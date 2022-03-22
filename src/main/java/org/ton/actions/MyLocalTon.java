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
import javafx.scene.control.Tab;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.fift.Fift;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.executors.liteclient.api.ResultComputeReturnStake;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.executors.liteclient.api.ResultListBlockTransactions;
import org.ton.executors.liteclient.api.block.Address;
import org.ton.executors.liteclient.api.block.Message;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.executors.liteclient.api.block.Value;
import org.ton.executors.validatorengine.ValidatorEngine;
import org.ton.executors.validatorengineconsole.ValidatorEngineConsole;
import org.ton.main.App;
import org.ton.main.Main;
import org.ton.parameters.SendToncoinsParam;
import org.ton.parameters.ValidationParam;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.ui.controllers.MainController;
import org.ton.utils.Utils;
import org.ton.wallet.Wallet;
import org.ton.wallet.WalletAddress;
import org.ton.wallet.WalletVersion;

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
import static org.ton.main.App.*;

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
    public static final int YEAR_1971 = 34131600;
    public static final int VALIDATION_GUI_REFRESH_SECONDS = 30;

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
        Platform.runLater(() -> mainController.startWeb());
    }

    public void runNodesMonitor() {

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Thread.currentThread().setName("MyLocalTon - Nodes Monitor");

            for (String nodeName : settings.getActiveNodes()) {

                Executors.newSingleThreadExecutor().execute(() -> {
                    Thread.currentThread().setName("MyLocalTon - " + nodeName + " Monitor");
                    try {

                        Node node = settings.getNodeByName(nodeName);
                        ResultLastBlock lastBlock = LiteClientParser.parseLast(LiteClient.getInstance(LiteClientEnum.LOCAL).executeLast(node));
                        if (isNull(lastBlock)) {
                            node.setStatus("not ready");
                            log.info("{} is not ready", nodeName);
                        } else {
                            node.setStatus("out of sync by " + lastBlock.getSyncedSecondsAgo() + " seconds");
                            log.info("{} is out of sync by {} seconds", nodeName, lastBlock.getSyncedSecondsAgo());
                        }

                        Platform.runLater(() -> {
                            Utils.showNodeStatus(settings.getNodeByName(nodeName), Utils.getNodeStatusLabelByName(nodeName), Utils.getNodeTabByName(nodeName));
                        });

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
            log.info("Initializing genesis network");
            Thread.sleep(1000);
            Platform.runLater(() -> mainController.showWarningMsg("Initializing TON blockchain very first time. It can take up to 2 minutes, please wait.", 60 * 5L));

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
            log.info("Found non-empty state; Skip genesis initialization.");
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
            e.printStackTrace();
        }
    }

    /**
     * start validator for a short time in order to execute commands via validator-engine-console
     *
     * @param node
     * @param myGlobalConfig
     * @throws Exception
     */
    public void createGenesisValidator(Node node, String myGlobalConfig) throws Exception {

        String validatorPrvKeyHex = node.getValidatorPrvKeyHex();
        log.info("{} validatorIdHex {}", node.getNodeName(), node.getValidatorPrvKeyHex());

        log.info("Starting temporary full-node...");
        Pair<Process, Future<String>> validatorProcess = new ValidatorEngine().startValidatorWithoutParams(node, myGlobalConfig);

        log.debug("sleep 5sec");
        Thread.sleep(5000);
        ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();

        String newNodeKey = validatorEngineConsole.generateNewNodeKey(node);
        String newNodePubKey = validatorEngineConsole.exportPubKey(node, newNodeKey);
        String newValAdnl = validatorEngineConsole.generateNewNodeKey(node);

        log.info("newNodeKey {}, newNodePubKey {}, newValAdnl {}", newNodeKey, newNodePubKey, newValAdnl);

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

        validatorProcess.getLeft().destroy();
    }

    private static int parseIp(String address) {
        int result = 0;
        for (String part : address.split(Pattern.quote("."))) {
            result = result << 8;
            result |= Integer.parseInt(part);
        }
        return result;
    }

    public WalletEntity createWalletWithFundsAndSmartContract(Node fromNode, Node toNode, long workchain, long subWalletId, BigDecimal amount) throws Exception {
        Wallet wallet = new Wallet();
        WalletVersion walletVersion = settings.getWalletSettings().getWalletVersion();
        log.debug("createWalletWithFundsAndSmartContract, default wallet version {}", walletVersion.getValue());

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
                .fromWalletVersion(WalletVersion.V1)
                .fromSubWalletId(-1L)
                .destAddr(walletAddress.getNonBounceableAddressBase64Url())
                .amount(amount)
                .build();

        boolean sentOK = wallet.sendTonCoins(sendToncoinsParam);

        if (sentOK) {
            Thread.sleep(2000);

            // install smart-contract into a new wallet
            log.debug("installing wallet smc from node {}, boc {}", fromNode.getNodeName(), walletAddress.getWalletQueryFileBocLocation());
            wallet.installWalletSmartContract(fromNode, walletAddress);
        } else {
            App.mainController.showErrorMsg(String.format("Failed to send %s Toncoins to %s", amount, walletAddress.getNonBounceableAddressBase64Url()), 3);
        }

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
                createWalletEntity(genesisNode, getSettings().getGenesisNode().getTonBinDir() + ZEROSTATE + File.separator + "main-wallet", -1L, -1L, settings.getWalletSettings().getInitialAmount()); //WC -1
                Thread.sleep(500);
            }
            if (App.dbPool.existsConfigWallet() == 0) {
                createWalletEntity(genesisNode, getSettings().getGenesisNode().getTonBinDir() + ZEROSTATE + File.separator + "config-master", -1L, -1L, settings.getWalletSettings().getInitialAmount()); //WC -1
                Thread.sleep(500);
            }

            createWalletEntity(genesisNode, null, getSettings().getWalletSettings().getDefaultWorkChain(), getSettings().getWalletSettings().getDefaultSubWalletId(), settings.getWalletSettings().getInitialAmount());

            installed = App.dbPool.getNumberOfPreinstalledWallets();
            log.info("created {}", installed);
        }

        List<WalletEntity> wallets = App.dbPool.getAllWallets();

        for (WalletEntity wallet : wallets) {
            log.info("preinstalled wallet {}", wallet.getFullAddress());

            // always update account state on start
            //AccountState accountState = LiteClientParser.parseGetAccount(LiteClientExecutor.getInstance().executeGetAccount(genesisNode, wallet.getWc() + ":" + wallet.getHexAddress()));
            Pair<AccountState, Long> stateAndSeqno = getAccountStateAndSeqno(genesisNode, wallet.getWc() + ":" + wallet.getHexAddress());
            App.dbPool.updateWalletStateAndSeqno(wallet, stateAndSeqno.getLeft(), stateAndSeqno.getRight());

            wallet.setAccountState(stateAndSeqno.getLeft());
            wallet.setSeqno(stateAndSeqno.getRight());
            updateAccountsTabGui(wallet);
        }
    }

    public WalletEntity createWalletEntity(Node node, String fileBaseName, long workchain, long subWalletid, BigDecimal amount) {

        try {
            WalletEntity wallet;
            if (isNull(fileBaseName)) { //generate and read address of just generated wallet
                wallet = createWalletWithFundsAndSmartContract(settings.getGenesisNode(), node, workchain, subWalletid, amount);
                log.debug("create wallet address {}", wallet.getHexAddress());
            } else { //read address of initially created wallet (main-wallet and config-master)
                wallet = new Fift().getWalletByBasename(node, fileBaseName);
                log.debug("read wallet address: {}", wallet.getHexAddress());
            }

            Pair<AccountState, Long> stateAndSeqno = getAccountStateAndSeqno(node, wallet.getWc() + ":" + wallet.getHexAddress());
            log.info("on node {}, created wallet {} with balance {}", node.getNodeName(), wallet.getWc() + ":" + wallet.getHexAddress(), stateAndSeqno.getLeft().getBalance().getToncoins());
            App.dbPool.updateWalletStateAndSeqno(wallet, stateAndSeqno.getLeft(), stateAndSeqno.getRight());

            wallet.setAccountState(stateAndSeqno.getLeft());
            wallet.setSeqno(stateAndSeqno.getRight());
            updateAccountsTabGui(wallet);
            return wallet;
        } catch (Exception e) {
            log.error("Error creating wallet! Error {} ", e.getMessage());
            return null;
        }
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

    public void runValidationMonitor() {
        log.info("Starting validation monitor");

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            Thread.currentThread().setName("MyLocalTon - Validation Monitor");

            if (Main.appActive.get()) {
                try {

                    ValidationParam v = Utils.getConfig(settings.getGenesisNode());
                    log.debug("validation parameters {}", v);
                    long currentTime = Utils.getCurrentTimeSeconds();
                    long electionsDelta = v.getNextElections() - v.getStartElections();
                    log.info("currTime - getStartElections = {} > {}", currentTime - v.getStartElections(), electionsDelta * 3);

                    if (((v.getStartValidationCycle() > YEAR_1971) && ((currentTime > v.getStartElections()) && (currentTime < v.getEndElections() - 10))) // 10 sec to process
                            || ((v.getStartValidationCycle() > YEAR_1971) && ((currentTime - v.getStartElections()) > (electionsDelta * 3)))) {

                        log.debug("current active election id {} {}", v.getStartValidationCycle(), Utils.toLocal(v.getStartValidationCycle()));

                        log.info("ELECTIONS OPENED");

                        if (firstAppLaunch) {
                            firstAppLaunch = false;
                            if (!settings.getVeryFirstElections()) {
                                log.debug("A. First app launch and not the first elections");
                                //always draw elections bars from the beginning
                                settings.electionsCounter.clear();
                                settings.electionsCounter.put(1L, null);
                                settings.electionsCounter.put(2L, null);
                                settings.electionsCounter.put(3L, null);
                            } else {
                                if (nonNull(settings.getLastValidationParamEvery3Cycles())) {
                                    log.info("Y. currTime - getLastValidationParamEvery3Cycles().getStartElections = {} > {}", currentTime - settings.getLastValidationParamEvery3Cycles().getStartElections(), electionsDelta * 3);
                                    if ((currentTime - settings.getLastValidationParamEvery3Cycles().getStartElections()) > (electionsDelta * 3)) {
                                        log.debug("too old previous start date of elections");
                                        settings.electionsCounter.clear();
                                    }
                                }
                            }
                        }

                        settings.electionsCounter.put(v.getStartValidationCycle(), v.getStartValidationCycle());

                        settings.setLastValidationParam(v);

                        saveSettingsToGson();

                        Main.inElections.set(true);

                        for (String nodeName : settings.getActiveNodes()) {
                            Node node = settings.getNodeByName(nodeName);
                            log.info("participating in elections {}", node.getNodeName());
                            Utils.participate(node, v); // TODO parallel
                        }
                    } else { // active election id is not available

                        v = settings.getLastValidationParam();
                        if (isNull(v)) {
                            log.info("neither active nor previous election ids are available, waiting for the elections to be opened");
                            return;
                        }

                        log.info("ELECTIONS CLOSED");

                        log.info("using old election id {} {}", settings.getLastValidationParam().getStartValidationCycle(), Utils.toLocal(settings.getLastValidationParam().getStartValidationCycle()));

                        if (firstAppLaunch) {
                            firstAppLaunch = false;
                            if (!settings.getVeryFirstElections()) {
                                log.info("B. First app launch and not the first elections");
                                firstAppLaunch = false;
                                //always draw elections bars from the beginning
                                settings.electionsCounter.clear();
                                settings.electionsCounter.put(1L, null);
                                settings.electionsCounter.put(2L, null);
                                settings.electionsCounter.put(3L, null);
                                settings.electionsCounter.put(v.getStartValidationCycle(), v.getStartValidationCycle());
                            } else {
                                if (nonNull(settings.getLastValidationParamEvery3Cycles())) {
                                    log.info("X. currTime - getLastValidationParamEvery3Cycles().getStartElections = {} > {}", currentTime - settings.getLastValidationParamEvery3Cycles().getStartElections(), electionsDelta * 3);
                                    if ((currentTime - settings.getLastValidationParamEvery3Cycles().getStartElections()) > (electionsDelta * 3)) {
                                        log.info("X. too old previous start date of elections");
                                        settings.electionsCounter.clear();
                                        settings.electionsCounter.put(1L, null);
                                        settings.electionsCounter.put(2L, null);
                                        //settings.electionsCounter.put(3L, null);
                                        settings.electionsCounter.put(settings.getLastValidationParamEvery3Cycles().getStartValidationCycle(), settings.getLastValidationParamEvery3Cycles().getStartValidationCycle());
                                    }
                                }
                            }
                        }

                        saveSettingsToGson();
                    }

                    ValidationParam finalV = v;

                    mainController.drawElections(finalV);

                    for (String nodeName : settings.getActiveNodes()) {
                        reap(settings.getNodeByName(nodeName)); // TODO parallel
                    }

                    Main.inElections.set(false);

                } catch (Exception e) {
                    log.error("Error getting blockchain configuration! Error {}", e.getMessage());
                    log.error(ExceptionUtils.getStackTrace(e));
                }

                log.debug("refresh GUI, sleep 30 sec ");

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
        }, 0L, VALIDATION_GUI_REFRESH_SECONDS, TimeUnit.SECONDS);
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
                    LiteClient liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);

                    executorService.execute(() -> {
                        Thread.currentThread().setName("MyLocalTon - Dump Block " + prevBlockSeqno.get());
                        log.debug("Get last block");
                        //Main.parsingBlocks.set(true);
                        ResultLastBlock lastBlock = LiteClientParser.parseLast(liteClient.executeLast(node));

                        if (nonNull(lastBlock)) {

                            if ((!Objects.equals(prevBlockSeqno.get(), lastBlock.getSeqno())) && (lastBlock.getSeqno().compareTo(BigInteger.ZERO) != 0)) {

                                prevBlockSeqno.set(lastBlock.getSeqno());
                                log.info(lastBlock.getShortBlockSeqno());

                                List<ResultLastBlock> shardsInBlock = insertBlocksAndTransactions(node, lastBlock, true);

                                updateTopInfoBarGui(shardsInBlock.size());
                            }
                        } else {
                            log.debug("last block is null");
                        }
                        //Main.parsingBlocks.set(false);
                        log.debug("Thread is done {}", Thread.currentThread().getName());
                    });

                    executorService.shutdown();

                    Thread.sleep(1000);

                } catch (Exception e) {
                    //Main.parsingBlocks.set(false);
                    e.printStackTrace();
                    log.error(e.getMessage());
                }
            }
            blockchainMonitorExecutorService.shutdownNow();
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
        String formattedBalance = String.format("%,.9f", balance.divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING));
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

    private void showInGuiOnlyUniqueTxs(ResultLastBlock lastBlock, ResultListBlockTransactions tx, Transaction txDetails, MainController c, TxEntity txE, javafx.scene.Node
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
        ((Label) txRow.lookup("#amount")).setText(txEntity.getAmount().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING).toPlainString());

        ((Label) txRow.lookup("#fees")).setText(txEntity.getTx().getTotalFees().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING).toPlainString());
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
        ((Label) txRow.lookup("#amount")).setText(txEntity.getAmount().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING).toPlainString());

        ((Label) txRow.lookup("#fees")).setText(txDetails.getTotalFees().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING).toPlainString());
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

        //AccountState accountState = LiteClientParser.parseGetAccount(LiteClientExecutor.getInstance().executeGetAccount(settings.getGenesisNode(), lastBlock.getWc() + ":" + txDetails.getAccountAddr()));
        Pair<AccountState, Long> stateAndSeqno = getAccountStateAndSeqno(settings.getGenesisNode(), lastBlock.getWc() + ":" + txDetails.getAccountAddr());
        log.debug("insertAccountEntity, wallet {}:{}, balance {}, state {}", stateAndSeqno.getLeft().getWc(), stateAndSeqno.getLeft().getAddress(), stateAndSeqno.getLeft().getBalance(), stateAndSeqno.getLeft().getStatus());

        Pair<WalletVersion, Long> walletVersionAndId = Utils.detectWalledVersionAndId(stateAndSeqno.getLeft());

        WalletEntity foundWallet = App.dbPool.findWallet(WalletPk.builder()
                .wc(lastBlock.getWc())
                .hexAddress(txDetails.getAccountAddr())
                .build());

        if (isNull(foundWallet)) {

            WalletAddress wa = new Fift().convertAddr(settings.getGenesisNode(), lastBlock.getWc() + ":" + txDetails.getAccountAddr());

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
                        AccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(getInstance().getSettings().getGenesisNode(), wallet.getWc() + ":" + wallet.getHexAddress()));
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
        AccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(node, address));
        if (nonNull(accountState.getBalance())) {
            long seqno = LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetSeqno(getInstance().getSettings().getGenesisNode(), address);
            return Pair.of(accountState, seqno);
        }
        return Pair.of(null, -1L);
    }

    public void reap(Node node) throws Exception {

        log.debug("reaping rewards by {}", node.getNodeName());

        if (isNull(node.getWalletAddress())) {
            log.error("{} wallet is not present", node.getNodeName());
            return;
        }

        ResultComputeReturnStake result = LiteClientParser.parseRunMethodComputeReturnStake(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeComputeReturnedStake(node, settings.getElectorSmcAddrHex(), node.getWalletAddress().getHexWalletAddress()));

        log.debug("reap amount {}", result.getStake());

        if (result.getStake().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Reaping rewards. {} reward size is greater than 0, send request for stake recovery", node.getNodeName());

            // create recover-query.boc
            new Fift().createRecoverStake(node);

            // send stake and validator-query.boc to elector
            SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                    .executionNode(node)
                    .fromWallet(node.getWalletAddress())
                    .fromWalletVersion(WalletVersion.V3)
                    .fromSubWalletId(settings.getWalletSettings().getDefaultSubWalletId())
                    .destAddr(settings.getElectorSmcAddrHex())
                    .amount(BigDecimal.valueOf(1L))
                    .comment("stake-recover-request") // TODO check if comment is visible
                    .bocLocation(node.getTonBinDir() + "recover-query.boc")
                    .build();

            new Wallet().sendTonCoins(sendToncoinsParam);

            // Basic rewards statistics. Better to fetch from the DB actual values, since recover stake may fail e.g.

            node.setLastRewardCollected(result.getStake());
            node.setTotalRewardsCollected(node.getTotalRewardsCollected().add(result.getStake()));
            //returned stake - 10001 + 1
            node.setLastPureRewardCollected(result.getStake().subtract(node.getDefaultValidatorStake().subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(ONE_BLN))));
            node.setTotalPureRewardsCollected(node.getTotalPureRewardsCollected().add(node.getLastPureRewardCollected()));
            node.setElectionsRipped(node.getElectionsRipped().add(BigDecimal.ONE));
            node.setAvgPureRewardCollected(node.getTotalPureRewardsCollected().divide(node.getElectionsRipped(), 9, RoundingMode.CEILING));

            saveSettingsToGson();

            Platform.runLater(() -> {
                updateReapedValuesTab(node);
            });
        } else {
            log.info("Reaping rewards. {} reward size is {}, nothing to reap.", node.getNodeName(), result.getStake());
        }
    }

    private void updateReapedValuesTab(Node node) {
        log.info("{} updating reaped values {}", node.getNodeName(), String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));

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
                mainController.validator1totalCollected.setText(String.format("%,.9f", node.getTotalRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                mainController.validator1LastCollected.setText(String.format("%,.9f", node.getLastRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                mainController.validator1TotalRewardsPure.setText(String.format("%,.9f", node.getTotalPureRewardsCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                mainController.validator1LastRewardPure.setText(String.format("%,.9f", node.getLastPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                mainController.validator1AvgPureReward.setText(String.format("%,.9f", node.getAvgPureRewardCollected().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
                mainController.participatedInElections1.setText(node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
        }
    }

    public void createValidatorPubKeyAndAdnlAddress(Node node, long electionId) throws Exception {

        long electionEnd = electionId + 600; // was YEAR

        createSigningKeyForValidation(node, electionId, electionEnd);
        createAdnlKeyForValidation(node, node.getValidationSigningKey(), electionEnd);

        node.setValidationPubKeyAndAdnlCreated(true); // not used

        saveSettingsToGson();
    }

    void createSigningKeyForValidation(Node node, long electionId, long electionEnd) {
        ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();

        String signingKey = validatorEngineConsole.generateNewNodeKey(node);
        String signingPubKey = validatorEngineConsole.exportPubKey(node, signingKey);

        log.info("{} signingKey {}, new signingPubKey {} for current elections", node.getNodeName(), signingKey, signingPubKey);

        node.setValidationSigningKey(signingKey);
        node.setValidationSigningPubKey(signingPubKey);

        validatorEngineConsole.addPermKey(node, signingKey, electionId, electionEnd);
        validatorEngineConsole.addTempKey(node, signingKey, electionEnd);
    }

    void createAdnlKeyForValidation(Node node, String signingKey, long electionEnd) {
        ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();
        String adnlKey = validatorEngineConsole.generateNewNodeKey(node);
        log.info("{} new adnlKey {} for current elections", node.getNodeName(), adnlKey);

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
                new ValidatorEngine().startValidatorWithoutParams(node, node.getNodeGlobalConfigLocation());
            }
            return;
        }
        log.info("creating new fullnode {}", node.getNodeName());
        node.extractBinaries();

        ValidatorEngine validatorEngine = new ValidatorEngine();
        validatorEngine.generateValidatorKeys(node, false);
        validatorEngine.initFullnode(node, settings.getGenesisNode().getNodeGlobalConfigLocation());

        if (!node.getNodeName().contains("genesis")) {
            if (isWindows()) {
//               on Windows locked files cannot be copied. As an option, we can shut down genesis node, copy the files and start it again.
//               but there is a connection issue with this on Windows
//               log.info("shutting down genesis node...");
//               settings.getGenesisNode().nodeShutdown();

//               if we copy only db/static dir, it works, but requires full synchronization that takes long time,
//               in comparison to linux systems where copy of all directories makes almost instant synchronization
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbStaticDir()), new File(node.getTonDbStaticDir()));
//              copy ignoring locked files
                Utils.copyDirectory(settings.getGenesisNode().getTonDbArchiveDir(), node.getTonDbArchiveDir()); // if only this dir, then fails with "Check `ptr && "deferencing null Ref"` failed"
                Utils.copyDirectory(settings.getGenesisNode().getTonDbCellDbDir(), node.getTonDbCellDbDir()); // with archive and celldb only - fails with - [!shardclient][&masterchain_block_handle_->inited_next_left()]
                Utils.copyDirectory(settings.getGenesisNode().getTonDbFilesDir(), node.getTonDbFilesDir());
                Utils.copyDirectory(settings.getGenesisNode().getTonDbCatchainsDir(), node.getTonDbCatchainsDir()); // [!shardclient][&masterchain_block_handle_->inited_next_left()]

//                Utils.copyDirectory(settings.getGenesisNode().getTonDbStateDir(), node.getTonDbStateDir());
//                log.info("launching genesis node...");
//                validatorEngine.startValidator(settings.getGenesisNode(), settings.getGenesisNode().getNodeGlobalConfigLocation());
//                Utils.waitForBlockchainReady(settings.getGenesisNode());
//                Utils.waitForNodeSynchronized(settings.getGenesisNode());
            } else {
                // speed up synchronization - copy archive, catchains, files, state and celldb directories
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbStaticDir()), new File(node.getTonDbStaticDir()));
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbArchiveDir()), new File(node.getTonDbArchiveDir()));
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbCatchainsDir()), new File(node.getTonDbCatchainsDir()));
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbCellDbDir()), new File(node.getTonDbCellDbDir()));
                FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbFilesDir()), new File(node.getTonDbFilesDir()));
                //FileUtils.copyDirectory(new File(settings.getGenesisNode().getTonDbStateDir()), new File(node.getTonDbStateDir()));
            }
        }

        if (enableLiteServer) {
            validatorEngine.enableLiteServer(node, node.getNodeGlobalConfigLocation(), false);
        }

        if (start) {
            validatorEngine.startValidatorWithoutParams(node, node.getNodeGlobalConfigLocation());
        }
    }
}