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
import org.ton.executors.dhtserver.DhtServer;
import org.ton.executors.fift.Fift;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.*;
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

    public void runBlockchainExplorer() {
        Platform.runLater(() -> mainController.startWeb());
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

            ValidatorEngine validatorEngine = new ValidatorEngine();

            validatorEngine.generateValidatorKeys(node, true);
            validatorEngine.configureGenesisZeroState();
            validatorEngine.createZeroState(node);
            //result: created tonDbDir + File.separator + MY_TON_GLOBAL_CONFIG_JSON with replace FILE_HASH and ROOT_HASH, still to fill [NODES]

            DhtServer dhtServer = new DhtServer();
            dhtServer.initDhtServer(node, EXAMPLE_GLOBAL_CONFIG, node.getNodeGlobalConfigLocation());  // result: generated dht-server/config.json
            dhtServerProcess = dhtServer.startDhtServer(node, node.getNodeGlobalConfigLocation());

            //run nodeInit.sh, run validator very first time
            validatorEngine.initFullnode(node, node.getNodeGlobalConfigLocation());

            Process validatorGenesisProcess = createGenesisValidator(node, node.getNodeGlobalConfigLocation());

            validatorEngine.enableLiteServer(node, node.getNodeGlobalConfigLocation(), false);

            return validatorGenesisProcess;
        } else {
            log.info("Found non-empty state; Skip genesis initialization.");
            dhtServerProcess = new DhtServer().startDhtServer(node, node.getNodeGlobalConfigLocation());
            Thread.sleep(100);
            return null;
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

    public Process createGenesisValidator(Node node, String myGlobalConfig) throws Exception {

        if (Files.exists(Paths.get(node.getTonDbDir() + File.separator + "validatorGenesis"))) {
            log.info("Found non-empty state of genesis validator.");
            return null;
        } else {
            String validatorPrvKeyHex = node.getValidatorPrvKeyHex();
            log.info("{} validatorIdHex {}", node.getNodeName(), node.getValidatorPrvKeyHex());
            //shortly start validator
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

            return isNull(validatorProcess) ? null : validatorProcess.getLeft();
        }
    }

    private static int parseIp(String address) {
        int result = 0;
        for (String part : address.split(Pattern.quote("."))) {
            result = result << 8;
            result |= Integer.parseInt(part);
        }
        return result;
    }

    public WalletEntity createWalletWithFundsAndSmartContract(Node fromNode, Node toNode, long workchain, long subWalletId, long amount) throws Exception {
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
                .amount(BigDecimal.valueOf(amount))
                .build();

        wallet.sendTonCoins(sendToncoinsParam);

        Thread.sleep(2000);

        // install smart-contract into a new wallet
        log.debug("installing wallet smc from node {}, boc {}", fromNode.getNodeName(), walletAddress.getWalletQueryFileBocLocation());
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
            //AccountState accountState = LiteClientParser.parseGetAccount(new LiteClientExecutor().executeGetAccount(genesisNode, wallet.getWc() + ":" + wallet.getHexAddress()));
            Pair<AccountState, Long> stateAndSeqno = getAccountStateAndSeqno(genesisNode, wallet.getWc() + ":" + wallet.getHexAddress());
            App.dbPool.updateWalletStateAndSeqno(wallet, stateAndSeqno.getLeft(), stateAndSeqno.getRight());

            wallet.setAccountState(stateAndSeqno.getLeft());
            wallet.setSeqno(stateAndSeqno.getRight());
            updateAccountsTabGui(wallet);
        }
    }

    public WalletEntity createWalletEntity(Node node, String fileBaseName, long workchain, long subWalletid, long amount) {

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

    public void runValidationMonitor() throws Exception {
        log.info("Starting validation monitor");

        log.info("starting node 2");
//        org.ton.settings.Node node2 = settings.getNode2();
//        MyLocalTon.getInstance().createFullnode(node2, true, true); //     add true to create wallet
//        Utils.waitForBlockchainReady(node2);
//        Utils.waitForNodeSynchronized(node2);
//        saveSettingsToGson();

        ExecutorService blockchainValidationExecutorService = Executors.newSingleThreadExecutor();

        blockchainValidationExecutorService.execute(() -> {
            Thread.currentThread().setName("MyLocalTon - Validation Monitor");

            while (Main.appActive.get()) {
                try {

                    ValidationParam v = Utils.getConfig(settings.getGenesisNode());
                    log.debug("validation parameters {}", v);

                    long currentTime = Utils.getCurrentTimeSeconds();

                    if ((currentTime > v.getStartElections()) && (currentTime < v.getEndElections())) {
                        log.info("ELECTIONS OPENED");
                        Utils.participate(settings.getGenesisNode(), v); // loop for other nodes IN PARALLEL
                        // Utils.participate(node2, v); // loop for other nodes
                    } else {
                        log.info("ELECTIONS CLOSED");
                        settings.getGenesisNode().setValidationParticipated(false);
                        //node2.setValidationParticipated(false);
                    }

                    Utils.updateValidationTabGUI(v);

                    Platform.runLater(() -> {
                        try {
                            mainController.drawElections();

                            mainController.electionsChartPane.setVisible(true);

                        } catch (Exception e) {
                            e.printStackTrace();
                            log.error(ExceptionUtils.getStackTrace(e));
                        }
                    });

                    log.info("PREVIOUS VALIDATORS {}", LiteClientParser.parseConfig32(new LiteClient().executeGetPreviousValidators(settings.getGenesisNode())).getValidators().getTotal());
                    log.info("CURRENT VALIDATORS {}", LiteClientParser.parseConfig34(new LiteClient().executeGetCurrentValidators(settings.getGenesisNode())).getValidators().getTotal());
                    log.info("NEXT VALIDATORS {}", LiteClientParser.parseConfig36(new LiteClient().executeGetNextValidators(settings.getGenesisNode())).getValidators().getTotal());

                    reap(settings.getGenesisNode());
                    //reap(node2);

                } catch (Exception e) {
                    log.error("Error getting blockchain configuration! Error {}", e.getMessage());
                }

                log.info("participate, sleep 30 sec ");

                try {
                    Thread.sleep(30 * 1000); // todo scheduled
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            blockchainValidationExecutorService.shutdownNow();
        });
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
                    LiteClient liteClient = new LiteClient();

                    executorService.execute(() -> {
                        Thread.currentThread().setName("MyLocalTon - Dump Block " + prevBlockSeqno.get());
                        log.debug("Get last block");
                        ResultLastBlock lastBlock;

                        lastBlock = LiteClientParser.parseLast(liteClient.executeLast(node));

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

    public List<ResultLastBlock> insertBlocksAndTransactions(Node node, ResultLastBlock lastBlock, boolean updateGuiNow) {

        insertBlockEntity(lastBlock);

        if (updateGuiNow) {
            updateBlocksTabGui(lastBlock);
        }

        List<ResultLastBlock> shardsInBlock = new LiteClient().getShardsFromBlock(node, lastBlock); // txs from basechain shards

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

        LiteClient liteClient = new LiteClient();
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
        String formattedBalance = String.format("%,.9f", balance.divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));
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

    public void monitorParticipants(Node node) {

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {

            Thread.currentThread().setName("MyLocalTon - Accounts Monitor");
            if (nonNull(node.getWalletAddress())) {
                try {
                    String stdout = new LiteClient().executeGetParticipantList(node, settings.getElectorSmcAddrHex());
                    List<ResultListParticipants> participants = LiteClientParser.parseRunMethodParticipantList(stdout);
                    log.info("AMOUNT OF PARTICIPANTS: {}", participants.size());

                    AccountState accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(settings.getGenesisNode(), settings.getGenesisNode().getWalletAddress().getFullWalletAddress()));
                    log.info("{} ({}) balance: {}", settings.getGenesisNode().getNodeName(), settings.getGenesisNode().getWalletAddress().getFullWalletAddress(), accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));

                    accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(settings.getGenesisNode(), settings.getMainWalletAddrFull()));
                    log.info("{} ({}) balance: {}", "main-wallet", settings.getMainWalletAddrFull(), accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));

                    accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(settings.getGenesisNode(), settings.getConfigSmcAddrHex()));
                    log.info("{} ({}) balance: {}", "config-wallet", settings.getConfigSmcAddrHex(), accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));

                    accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(settings.getGenesisNode(), settings.getElectorSmcAddrHex()));
                    log.info("{} ({}) balance: {}", "elector-smc", settings.getElectorSmcAddrHex(), accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));

//                    accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(settings.getNode2(), settings.getNode2().getWalletAddress().getFullWalletAddress()));
//                    log.info("{} ({}) balance: {}", settings.getNode2().getNodeName(), settings.getNode2().getWalletAddress().getFullWalletAddress(), accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));

//                    accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(settings.getNode3(), settings.getNode3().getWalletAddress().getFullWalletAddress()));
//                    log.info("{} ({}) balance: {}", settings.getNode3().getNodeName(), settings.getNode3().getWalletAddress().getFullWalletAddress(), accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));
//
//                    accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(settings.getNode4(), settings.getNode4().getWalletAddress().getFullWalletAddress()));
//                    log.info("{} ({}) balance: {}", settings.getNode4().getNodeName(), settings.getNode4().getWalletAddress().getFullWalletAddress(), accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(1000000000L), 9, RoundingMode.CEILING));

                } catch (Exception e) {
                    log.error("Error in monitorParticipants(), " + e.getMessage());
                }
            }
        }, 60L, 30L, TimeUnit.SECONDS);
    }

    public void runAccountsMonitor() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            Thread.currentThread().setName("MyLocalTon - Accounts Monitor");
            try {
                for (WalletEntity wallet : App.dbPool.getAllWallets()) {
                    if (Main.appActive.get()) {
                        AccountState accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(getInstance().getSettings().getGenesisNode(), wallet.getWc() + ":" + wallet.getHexAddress()));
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
        AccountState accountState = LiteClientParser.parseGetAccount(new LiteClient().executeGetAccount(node, address));
        if (nonNull(accountState.getBalance())) {
            long seqno = new LiteClient().executeGetSeqno(getInstance().getSettings().getGenesisNode(), address);
            return Pair.of(accountState, seqno);
        }
        return Pair.of(null, -1L);
    }

    public void reap(Node node) throws Exception {

        log.info("reap");

        ResultComputeReturnStake result = LiteClientParser.parseRunMethodComputeReturnStake(new LiteClient().executeComputeReturnedStake(node, settings.getElectorSmcAddrHex(), settings.getGenesisNode().getWalletAddress().getHexWalletAddress()));

        log.info("reap amount {}", result.getStake());

        if (result.getStake().compareTo(BigInteger.ZERO) > 0) {
            log.info("{} stake is greater than 0, send request for stake recovery", node.getNodeName());

            //create recover-query.boc
            new Fift().createRecoverStake(node);

            // send stake and validator-query.boc to elector
            SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                    .executionNode(node)
                    .fromWallet(node.getWalletAddress())
                    .fromWalletVersion(WalletVersion.V3)
                    .fromSubWalletId(settings.getWalletSettings().getDefaultSubWalletId())
                    .destAddr(settings.getElectorSmcAddrHex())
                    .amount(BigDecimal.valueOf(2L)) // amount to recover
                    .comment("stake-recover-request")
                    .bocLocation(node.getTonBinDir() + "recover-query.boc")
                    .build();

            new Wallet().sendTonCoins(sendToncoinsParam);
        } else {
            log.info("{} stake is {}, nothing to reap", node.getNodeName(), result.getStake());
        }
    }

    public void createValidatorPubKeyAndAdnlAddress(Node node, long electionId) throws Exception {

        long electionEnd = electionId + 600; // was YEAR

        createSigningKeyForValidation(node, electionId, electionEnd);
        createAdnlKeyForValidation(node, node.getValidationSigningKey(), electionEnd);

        node.setValidationPubKeyAndAdnlCreated(true);

        saveSettingsToGson();
    }

    void createSigningKeyForValidation(Node node, long electionId, long electionEnd) {
        ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();

        String signingKey = validatorEngineConsole.generateNewNodeKey(node);
        String signingPubKey = validatorEngineConsole.exportPubKey(node, signingKey);

        log.info("{} signingKey {}, signingPubKey {}", node.getNodeName(), signingKey, signingPubKey);

        node.setValidationSigningKey(signingKey); // setValidatorIdHex(newValKey);
        node.setValidationSigningPubKey(signingPubKey); // setValidatorIdBase64(newValPubKey);

        validatorEngineConsole.addPermKey(node, signingKey, electionId, electionEnd);
        validatorEngineConsole.addTempKey(node, signingKey, electionEnd);
    }

    void createAdnlKeyForValidation(Node node, String signingKey, long electionEnd) {
        ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();
        String adnlKey = validatorEngineConsole.generateNewNodeKey(node);
        log.info("{} adnlKey {}", node.getNodeName(), adnlKey);
        node.setValidationAndlKey(adnlKey); // .setValidatorAdnlAddrHex(newValAdnl);

        validatorEngineConsole.addAdnl(node, adnlKey);
        validatorEngineConsole.addValidatorAddr(node, signingKey, adnlKey, electionEnd);
        //validatorEngineConsole.getStats(node);
    }

    public void createFullnode(Node node, boolean enableLiteServer, boolean start) throws Exception {
        if (Files.exists(Paths.get(node.getTonBinDir()))) {
            log.info("{} already created, just start it", node.getNodeName());
            if (start) {
                new ValidatorEngine().startValidatorWithoutParams(node, node.getNodeGlobalConfigLocation());
            }
            return;
        }
        log.info("creating new fullnode {}", node.getNodeName());
        node.extractBinaries();

        ValidatorEngine validatorEngine = new ValidatorEngine();
        validatorEngine.initFullnode(node, settings.getGenesisNode().getNodeGlobalConfigLocation());

        Thread.sleep(2000);

        if (enableLiteServer) {
            validatorEngine.enableLiteServer(node, node.getNodeGlobalConfigLocation(), false);
        }

        if (start) {
            validatorEngine.startValidatorWithoutParams(node, node.getNodeGlobalConfigLocation());
        }
    }
/*
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


        private void recreateLiteServer(Node node) throws Exception {
            log.info("recreate lite-server");
            Files.deleteIfExists(Paths.get(node.getTonDbKeyringDir() + LITESERVER));
            Files.deleteIfExists(Paths.get(node.getTonDbKeyringDir() + "liteserver.pub"));
            installLiteServer(node, node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON, true); //not needed actually?
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
    */

}