package org.ton.mylocalton.actions;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.mylocalton.main.App.*;
import static org.ton.mylocalton.ui.custom.events.CustomEventBus.emit;

import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXListView;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;
import org.ton.mylocalton.data.Runner;
import org.ton.mylocalton.db.entities.BlockEntity;
import org.ton.mylocalton.db.entities.TxEntity;
import org.ton.mylocalton.db.entities.WalletEntity;
import org.ton.mylocalton.db.entities.WalletPk;
import org.ton.mylocalton.enums.LiteClientEnum;
import org.ton.mylocalton.executors.blockchainexplorer.BlockchainExplorer;
import org.ton.mylocalton.executors.fift.Fift;
import org.ton.mylocalton.executors.liteclient.LiteClient;
import org.ton.mylocalton.executors.liteclient.LiteClientParser;
import org.ton.mylocalton.executors.liteclient.api.ResultComputeReturnStake;
import org.ton.mylocalton.executors.liteclient.api.ResultLastBlock;
import org.ton.mylocalton.executors.tonhttpapi.TonHttpApi;
import org.ton.mylocalton.executors.validatorengine.ValidatorEngine;
import org.ton.mylocalton.executors.validatorengineconsole.ValidatorEngineConsole;
import org.ton.mylocalton.main.App;
import org.ton.mylocalton.main.Main;
import org.ton.mylocalton.parameters.SendToncoinsParam;
import org.ton.mylocalton.parameters.ValidationParam;
import org.ton.mylocalton.settings.MyLocalTonSettings;
import org.ton.mylocalton.settings.Node;
import org.ton.mylocalton.ui.controllers.MainController;
import org.ton.mylocalton.ui.custom.events.CustomEvent;
import org.ton.mylocalton.ui.custom.events.event.CustomNotificationEvent;
import org.ton.mylocalton.ui.custom.events.event.CustomSearchEvent;
import org.ton.mylocalton.utils.MyLocalTonUtils;
import org.ton.mylocalton.wallet.MyWallet;
import org.ton.mylocalton.wallet.WalletAddress;

@Slf4j
@Getter
@Setter
public class MyLocalTon {

  public static final String ZEROSTATE = "zerostate";
  public static final String UNINITIALIZED = "Uninitialized";
  public static final String ACTIVE = "Active";
  public static final String NONEXISTENT = "Nonexistent";
  public static final String EXTERNAL = "external";
  public static final String FROZEN = "Frozen";
  public static final long ONE_BLN = 1000000000L;
  public static final String FOUND_COLOR_HIHGLIGHT = "-fx-text-fill: #0088CC;";
  public static final String MY_LOCAL_TON = "myLocalTon";

  public static final int YEAR = 31556952;
  public static final int SCROLL_BAR_DELTA = 30;
  public static final Long MAX_ROWS_IN_GUI = 1000L;
  public static final int YEAR_1971 = 34131600;
  public static final int VALIDATION_GUI_REFRESH_SECONDS = 60;
  private static final String CURRENT_DIR = System.getProperty("user.dir");

  private static final String SETTINGS_JSON = "settings.json";
  public static final String SETTINGS_FILE =
      CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + SETTINGS_JSON;
  public static ScheduledExecutorService validatorsMonitor = null;
  public static Tonlib tonlib;
  //  public static Tonlib tonlibBlockMonitor;
  //  public static Tonlib tonlibDataGenerator;
  private static MyLocalTon singleInstance = null;
  public AtomicLong prevBlockSeqno = new AtomicLong(0);
  //  public AtomicLong nextBlockSeqno = new AtomicLong(0);
  ConcurrentHashMap<String, Long> concurrentBlocksHashMap = new ConcurrentHashMap<>();
  ConcurrentHashMap<String, Long> concurrentTxsHashMap = new ConcurrentHashMap<>();
  ConcurrentHashMap<String, Long> concurrentAccountsHashMap = new ConcurrentHashMap<>();
  AtomicBoolean insertingNewAccount = new AtomicBoolean(false);
  Boolean autoScroll;
  AtomicLong blocksScrollBarHighWaterMark = new AtomicLong(30);
  AtomicLong txsScrollBarHighWaterMark = new AtomicLong(30);
  AtomicLong accountsScrollBarHighWaterMark = new AtomicLong(30);
  ScheduledExecutorService monitorExecutorService;
  private Runnable fetchTask;
  @Setter @Getter private MyLocalTonSettings settings;

  private MyLocalTon() {
    //    prevBlockSeqno = new AtomicBigInteger(BigInteger.ZERO);
    autoScroll = true;
  }

  public static MyLocalTon getInstance() {
    if (singleInstance == null) singleInstance = new MyLocalTon();

    return singleInstance;
  }

  public void runBlockchainExplorer() {
    if (!GraphicsEnvironment.isHeadless()) {
      Platform.runLater(() -> mainController.startNativeBlockchainExplorer());
    } else {
      if (settings.getUiSettings().isEnableBlockchainExplorer()) {
        log.info(
            "Starting native blockchain-explorer on port {}",
            settings.getUiSettings().getBlockchainExplorerPort());
        BlockchainExplorer blockchainExplorer = new BlockchainExplorer();
        blockchainExplorer.startBlockchainExplorer(
            settings.getGenesisNode(),
            settings.getGenesisNode().getNodeGlobalConfigLocation(),
            settings.getUiSettings().getBlockchainExplorerPort());
        Utils.sleep(2);
      }
    }
  }

  public void runTonHttpApi() {
    if (!GraphicsEnvironment.isHeadless()) {
      Platform.runLater(() -> mainController.startTonHttpApi());
    } else {
      if (settings.getUiSettings().isEnableTonHttpApi()) {
        log.info(
            "Starting ton-http-api on port headless {}",
            settings.getUiSettings().getTonHttpApiPort());
        Utils.sleep(3);
        TonHttpApi tonHttpApi = new TonHttpApi();
        tonHttpApi.startTonHttpApi(
            settings.getGenesisNode(),
            settings.getGenesisNode().getNodeGlobalConfigLocation(),
            settings.getUiSettings().getTonHttpApiPort());
      }
    }
  }

  public void runNodesStatusMonitor() {

    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(
            () -> {
              Thread.currentThread().setName("MyLocalTon - Nodes Monitor");

              for (String nodeName : settings.getActiveNodes()) {

                Executors.newSingleThreadExecutor()
                    .execute(
                        () -> {
                          Thread.currentThread()
                              .setName("MyLocalTon - " + nodeName + " Status Monitor");
                          try {

                            if (App.testBinaries) {
                              if (tonlib.getLast().getLast().getSeqno() > 40) {
                                log.info("force exiting since in testing mode");
                                System.exit(0);
                              }
                            }

                            Node node = settings.getNodeByName(nodeName);
                            MasterChainInfo lastBlock = tonlib.getLast();
                            //                                LiteClientParser.parseLast(
                            //
                            // LiteClient.getInstance(LiteClientEnum.LOCAL).executeLast(node));
                            if (isNull(lastBlock)) {
                              node.setStatus("not ready");
                              log.info("{} is not ready", nodeName);

                              //                            }
                              //                            else if (lastBlock.getSyncedSecondsAgo()
                              // > 15) {
                              //                              node.setStatus(
                              //                                  "out of sync by " +
                              // lastBlock.getSyncedSecondsAgo() + " seconds");
                              //                              log.info(
                              //                                  "{} out of sync by {} seconds",
                              //                                  nodeName,
                              //                                  lastBlock.getSyncedSecondsAgo());
                            } else {
                              node.setStatus("ready");
                              node.setFlag("cloned");
                              log.info("{} is ready", nodeName);
                            }

                            if (!GraphicsEnvironment.isHeadless()) {
                              Platform.runLater(
                                  () ->
                                      MyLocalTonUtils.showNodeStatus(
                                          settings.getNodeByName(nodeName),
                                          MyLocalTonUtils.getNodeStatusLabelByName(nodeName),
                                          MyLocalTonUtils.getNodeTabByName(nodeName)));
                            }

                          } catch (Exception e) {
                            log.error("Error in runNodesMonitor(), " + e.getMessage());
                            if (App.testBinaries) {
                              if (testBinariesCounter.incrementAndGet() > 20) {
                                log.info(
                                    "force exiting since in test mode, blockchain was not able to start");
                                System.exit(333);
                              }
                            }
                          }
                        });
              }
            },
            30L,
            10L,
            TimeUnit.SECONDS);
  }

  public void initGenesis(Node node) throws Exception {

    if (!Files.exists(Paths.get(node.getTonDbDir() + "state"), LinkOption.NOFOLLOW_LINKS)) {
      log.debug("Initializing genesis network");

      ValidatorEngine validatorEngine = new ValidatorEngine();

      validatorEngine.generateValidatorKeys(node, true);
      validatorEngine.configureGenesisZeroState();
      validatorEngine.createZeroState(node);
      // result: created tonDbDir + File.separator + MY_TON_GLOBAL_CONFIG_JSON with replace
      // FILE_HASH and ROOT_HASH, still to fill [NODES]

      // run validator very first time
      validatorEngine.initFullnode(node, node.getNodeGlobalConfigLocation());

      createGenesisValidator(node, node.getNodeGlobalConfigLocation());

      validatorEngine.enableLiteServer(node, node.getNodeGlobalConfigLocation(), false);

      settings.getActiveNodes().add(node.getNodeName());
    } else {
      log.debug("Found non-empty state; Skip genesis initialization.");
    }
  }

  public void saveSettingsToGson() throws InterruptedException {
    ForkJoinPool.commonPool().submit(this::saveSettingsToGsonSynchronized);
    Thread.sleep(30);
  }

  private synchronized void saveSettingsToGsonSynchronized() {
    try {
      String abJson =
          new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(settings);
      FileUtils.writeStringToFile(new File(SETTINGS_FILE), abJson, StandardCharsets.UTF_8);
    } catch (Throwable e) {
      log.error("Error saving {} file: {}", SETTINGS_JSON, e.getMessage());
      log.error(ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * start validator for a short time in order to execute commands via validator-engine-console
   *
   * @param node Node
   * @param myGlobalConfig String
   */
  public void createGenesisValidator(Node node, String myGlobalConfig) throws Exception {

    String validatorPrvKeyHex = node.getValidatorPrvKeyHex();
    log.debug("{} validatorIdHex {}", node.getNodeName(), node.getValidatorPrvKeyHex());

    log.debug("Starting temporary full-node...");
    Process validatorProcess =
        new ValidatorEngine().startValidatorWithoutParams(node, myGlobalConfig).getLeft();

    log.debug("sleep 5s");
    Thread.sleep(5000);
    ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();

    String newNodeKey = validatorEngineConsole.generateNewNodeKey(node);
    String newNodePubKey = validatorEngineConsole.exportPubKey(node, newNodeKey);
    String newValAdnl = validatorEngineConsole.generateNewNodeKey(node);

    log.debug(
        "newNodeKey {}, newNodePubKey {}, newValAdnl {}", newNodeKey, newNodePubKey, newValAdnl);

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

  public WalletEntity createWalletWithFundsAndSmartContract(
      Node fromNode,
      WalletVersion walletVersion,
      long workchain,
      long subWalletId,
      BigInteger amount)
      throws Exception {
    MyWallet myWallet = new MyWallet();

    WalletAddress walletAddress =
        myWallet.createWalletByVersion(walletVersion, workchain, subWalletId);

    WalletEntity walletEntity =
        WalletEntity.builder()
            .wc(walletAddress.getWc())
            .hexAddress(walletAddress.getHexWalletAddress().toUpperCase())
            .walletVersion(walletVersion)
            .wallet(walletAddress)
            .accountState(RawAccountState.builder().build())
            .createdAt(Instant.now().getEpochSecond())
            .build();

    log.debug("inserted walletEntity into DB {}", walletEntity);
    App.dbPool.insertWallet(walletEntity);

    // TOP UP NEW WALLET

    WalletAddress fromMasterWalletAddress =
        WalletAddress.builder()
            .fullWalletAddress(settings.getMainWalletAddrFull())
            .privateKeyHex(settings.getMainWalletPrvKey())
            .bounceableAddressBase64url(settings.getMainWalletAddrBase64())
            .filenameBase("main-wallet")
            .filenameBaseLocation(settings.getMainWalletFilenameBaseLocation())
            .build();

    SendToncoinsParam sendToncoinsParam =
        SendToncoinsParam.builder()
            .executionNode(settings.getGenesisNode())
            .workchain(-1L)
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
      if (!GraphicsEnvironment.isHeadless()) {
        mainController.showErrorMsg(
            String.format(
                "Failed to send %s Toncoins to %s",
                amount, walletAddress.getNonBounceableAddressBase64Url()),
            5);
      } else {
        log.error(
            String.format(
                "Failed to send %s Toncoins to %s",
                amount, walletAddress.getNonBounceableAddressBase64Url()));
      }
    }

    return walletEntity;
  }

  public void createInitialWallets(Node genesisNode) throws Exception {

    long installed = App.dbPool.getNumberOfWalletsFromAllDBsAsync();

    //    if (installed < 3) {
    //      Thread.sleep(1000);
    //      if (!GraphicsEnvironment.isHeadless()) {
    //        mainController.showInfoMsg("Creating initial wallets...", 6);
    //      } else {
    //        log.info("Creating initial wallets...");
    //      }
    //    }

    if (App.dbPool.existsMainWallet() == 0) {
      createWalletEntity(
          genesisNode,
          getSettings().getGenesisNode().getTonBinDir()
              + ZEROSTATE
              + File.separator
              + "main-wallet",
          WalletVersion.master,
          -1L,
          -1L,
          settings.getWalletSettings().getInitialAmount(),
          false); // WC -1
    }

    if (App.dbPool.existsConfigWallet() == 0) {
      createWalletEntity(
          genesisNode,
          getSettings().getGenesisNode().getTonBinDir()
              + ZEROSTATE
              + File.separator
              + "config-master",
          WalletVersion.config,
          -1L,
          -1L,
          settings.getWalletSettings().getInitialAmount(),
          false); // WC -1
    }

    createValidatorControllingSmartContract(genesisNode);

    createWalletFromBaseFile(genesisNode, WalletVersion.V3R2, "faucet");
    createWalletFromBaseFile(genesisNode, WalletVersion.highload, "faucet-highload");
    createWalletFromBaseFile(genesisNode, WalletVersion.highload, "data-highload");

    while (installed < 3) {
      installed = App.dbPool.getNumberOfWalletsFromAllDBsAsync();
      Thread.sleep(200);
      log.info("creating main validator wallet, total wallets {}", installed);
    }
  }

  public void createValidatorControllingSmartContract(Node node) {
    if (isNull(node.getWalletAddress())) {
      log.info("Creating validator controlling smart-contract for node {}", node.getNodeName());
      createWalletEntity(
          node,
          node.getValidatorBaseFile(),
          WalletVersion.V3R2,
          -1L,
          settings.getWalletSettings().getDefaultSubWalletId(),
          node.getInitialValidatorWalletAmount(),
          true);
    }
  }

  private void createWalletFromBaseFile(Node node, WalletVersion walletVersion, String baseFile) {

    boolean created = false;
    if (baseFile.contains("faucet-highload")) {
      created = settings.getFaucetHighloadWalletSettings().isCreated();
    } else if (baseFile.contains("data-highload")) {
      created = settings.getFaucetDataWalletSettings().isCreated();
    } else if (baseFile.contains("faucet")) {
      created = settings.getFaucetWalletSettings().isCreated();
    }
    if (!created) {
      log.info("Creating faucet wallet {} {} {}", node.getNodeName(), walletVersion, baseFile);
      createWalletEntity(
          node,
          getSettings().getGenesisNode().getTonBinDir() + ZEROSTATE + File.separator + baseFile,
          walletVersion,
          -1L,
          settings.getWalletSettings().getDefaultSubWalletId(),
          node.getInitialValidatorWalletAmount(),
          false);

      if (baseFile.contains("faucet-highload")) {
        settings.getFaucetHighloadWalletSettings().setCreated(true);
      } else if (baseFile.contains("data-highload")) {
        settings.getFaucetDataWalletSettings().setCreated(true);
      } else if (baseFile.contains("faucet")) {
        settings.getFaucetWalletSettings().setCreated(true);
      }
    }
  }

  //  private void createValidator2ControllingSmartContract(Node node) {
  //    if (isNull(node.getWalletAddress())) {
  //      log.info("Creating validator controlling smart-contract for node {}", node.getNodeName());
  //      createWalletSynchronously(
  //          node,
  //          getSettings().getGenesisNode().getTonBinDir()
  //              + ZEROSTATE
  //              + File.separator
  //              + "validator-1",
  //          WalletVersion.V3R2,
  //          -1L,
  //          settings.getWalletSettings().getDefaultSubWalletId(),
  //          node.getInitialValidatorWalletAmount(),
  //          true);
  //    }
  //  }
  //
  //  private void createValidator3ControllingSmartContract(Node node) {
  //    if (isNull(node.getWalletAddress())) {
  //      log.info("Creating validator controlling smart-contract for node {}", node.getNodeName());
  //      createWalletSynchronously(
  //          node,
  //          getSettings().getGenesisNode().getTonBinDir()
  //              + ZEROSTATE
  //              + File.separator
  //              + "validator-1",
  //          WalletVersion.V3R2,
  //          -1L,
  //          settings.getWalletSettings().getDefaultSubWalletId(),
  //          node.getInitialValidatorWalletAmount(),
  //          true);
  //    }
  //  }

  public void createWalletEntity(
      Node node,
      String fileBaseName,
      WalletVersion walletVersion,
      long workchain,
      long subWalletid,
      BigInteger amount,
      boolean validatorWallet) {
    try {
      WalletEntity wallet;
      if (isNull(fileBaseName)) {
        wallet =
            createWalletWithFundsAndSmartContract(
                settings.getGenesisNode(), walletVersion, workchain, subWalletid, amount);
        log.debug("created wallet address {}", wallet.getHexAddress());
      } else { // read address of initially created wallet (main-wallet and config-master)
        wallet = new Fift().getWalletByBasename(node, fileBaseName);
        log.debug(
            "created wallet from baseFile {}, address: {}", fileBaseName, wallet.getHexAddress());
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
    monitorExecutorService.scheduleWithFixedDelay(
        () -> {
          Thread.currentThread().setName("MyLocalTon - Blockchain Size Monitor");

          String size =
              MyLocalTonUtils.getDirectorySizeUsingDu(CURRENT_DIR + File.separator + MY_LOCAL_TON);
          log.debug("size {}", size);
          if (!GraphicsEnvironment.isHeadless()) {
            MainController c = fxmlLoader.getController();
            Platform.runLater(() -> c.dbSizeId.setSecondaryText(size));
          }
        },
        0L,
        60L,
        TimeUnit.SECONDS);
  }

  public void runValidationMonitor() {
    log.info("Starting validation monitor");

    Executors.newSingleThreadScheduledExecutor()
        .scheduleWithFixedDelay(
            () -> {
              Thread.currentThread().setName("MyLocalTon - Validation Monitor");

              if (Main.appActive.get()) {
                try {
                  long currentTime = MyLocalTonUtils.getCurrentTimeSeconds();

                  ValidationParam v = MyLocalTonUtils.getConfig(settings.getGenesisNode());
                  log.debug("validation parameters {}", v);
                  // save election ID
                  if (v.getStartValidationCycle() > YEAR_1971) {
                    settings.elections.put(v.getStartValidationCycle(), v);
                    saveSettingsToGson();
                  }

                  long electionsDelta = v.getNextElections() - v.getStartElections();

                  if (!GraphicsEnvironment.isHeadless()) {
                    try {
                      mainController.drawElections();
                    } catch (Throwable t) {
                      log.error("Error drawing elections {}", t.getMessage());
                      log.error(ExceptionUtils.getStackTrace(t));
                    }
                  }

                  log.debug(
                      "[start-end] elections [{} - {}], currentTime {}",
                      MyLocalTonUtils.toLocal(v.getStartElections()),
                      MyLocalTonUtils.toLocal(v.getEndElections()),
                      MyLocalTonUtils.toLocal(currentTime));
                  log.debug(
                      "currTime > delta2, {} {}",
                      (currentTime - v.getStartElections()),
                      electionsDelta * 2);

                  if (((v.getStartValidationCycle() > YEAR_1971)
                          && ((currentTime > v.getStartElections())
                              && (currentTime < v.getEndElections() - 10))) // 10 sec to process
                      || ((v.getStartValidationCycle() > YEAR_1971)
                          && ((currentTime - v.getStartElections()) > (electionsDelta * 2)))) {

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
                  Platform.runLater(
                      () -> {
                        ProgressBar progress = mainController.progressValidationUpdate;
                        Timeline timeline =
                            new Timeline(
                                new KeyFrame(
                                    Duration.ZERO, new KeyValue(progress.progressProperty(), 0)),
                                new KeyFrame(
                                    Duration.seconds(VALIDATION_GUI_REFRESH_SECONDS),
                                    new KeyValue(progress.progressProperty(), 1)));
                        timeline.setCycleCount(1);
                        timeline.play();
                      });
                }
              }
            },
            0L,
            VALIDATION_GUI_REFRESH_SECONDS,
            TimeUnit.SECONDS);
  }

  public void runDataGenerator() {
    if (settings.getUiSettings().isEnableDataGenerator()) {
      log.info("Starting data generator");
      ForkJoinPool.commonPool()
          .execute(
              () -> {
                Thread.currentThread().setName("MyLocalTon - data-generator");
                Runner runner = Runner.builder().tonlib(tonlib).period(1).build();
                runner.run();
              });
    }
  }

  private void participateAll(ValidationParam v) {
    for (String nodeName : settings.getActiveNodes()) {
      Node node = settings.getNodeByName(nodeName);

      if (node.getStatus().equals("ready")) {
        log.info("participates in elections {}", nodeName);
        ForkJoinPool.commonPool()
            .execute(
                () -> {
                  Thread.currentThread()
                      .setName("MyLocalTon - Participation in elections by " + nodeName);
                  MyLocalTonUtils.participate(node, v);
                });
      }
    }
  }

  private void reapAll() {
    for (String nodeName : settings.getActiveNodes()) {
      Node node = settings.getNodeByName(nodeName);

      if (node.getStatus().equals("ready")) {

        ForkJoinPool.commonPool()
            .execute(
                () -> {
                  Thread.currentThread().setName("MyLocalTon - Reaping rewards by " + nodeName);
                  reap(settings.getNodeByName(nodeName));
                  if (!GraphicsEnvironment.isHeadless()) {
                    Platform.runLater(() -> updateReapedValuesTab(node));
                  }
                });
      }
    }
  }

  private String getTonlibName() {
    String tonlibName = null;
    switch (Utils.getOS()) {
      case LINUX:
        tonlibName = "libtonlibjson.so";
        break;
      case LINUX_ARM:
        tonlibName = "libtonlibjson.so";
        break;
      case WINDOWS:
        tonlibName = "tonlibjson.dll";
        break;
      case WINDOWS_ARM:
        tonlibName = "tonlibjson.dll";
        break;
      case MAC:
        tonlibName = "libtonlibjson.dylib";
        break;
      case MAC_ARM64:
        tonlibName = "libtonlibjson.dylib";
        break;
      case UNKNOWN:
        System.out.println("Unknown OS. Simple tonlib test failed.");
        System.exit(11);
      default:
        System.out.println("Unknown OS. Simple tonlib test failed.");
        System.exit(12);
    }
    return tonlibName;
  }

  public void initTonlib(Node node) {
    String tonlibName = settings.getGenesisNode().getTonBinDir() + getTonlibName();

    try {
      tonlib =
          Tonlib.builder()
              .pathToGlobalConfig(node.getNodeGlobalConfigLocation())
              .keystorePath(node.getTonlibKeystore().replace("\\", "/"))
              .pathToTonlibSharedLib(tonlibName)
              .receiveTimeout(5)
              .receiveRetryTimes(24)
              //              .verbosityLevel(VerbosityLevel.DEBUG)
              .build();
    } catch (Throwable e) {
      System.out.println(ExceptionUtils.getStackTrace(e));
      log.error("Cannot initialize tonlib!");
      System.exit(14);
    }
  }

  public void runBlockchainMonitor(Node node) {
    log.info("Starting node monitor");

    ExecutorService blockchainMonitorExecutorService = Executors.newSingleThreadExecutor();

    blockchainMonitorExecutorService.execute(
        () -> {
          Thread.currentThread().setName("MyLocalTon - Blockchain Monitor");
          ExecutorService executorService;
          while (Main.appActive.get()) {
            try {
              executorService = Executors.newSingleThreadExecutor();

              executorService.execute(
                  () -> {
                    Thread.currentThread()
                        .setName("MyLocalTon - Dump Block " + prevBlockSeqno.get());
                    log.debug("Getting last block");

                    ResultLastBlock lastBlock = MyLocalTonUtils.getLast();
                    log.debug("got last block {}", lastBlock);

                    if (nonNull(lastBlock)) {
                      if ((prevBlockSeqno.get() != lastBlock.getSeqno().longValue())
                          && (lastBlock.getSeqno().compareTo(BigInteger.ZERO) != 0)) {

                        prevBlockSeqno.set(lastBlock.getSeqno().longValue());
                        log.info(lastBlock.getShortBlockSeqno());

                        if (!GraphicsEnvironment.isHeadless()) {
                          List<ResultLastBlock> shardsInBlock =
                              insertBlocksAndTransactions(node, lastBlock, true);
                          updateTopInfoBarGui(shardsInBlock.size());
                        }
                      }
                    } else {
                      log.debug("last block is null");
                    }
                    log.debug("Thread is done {}", Thread.currentThread().getName());
                  });

              executorService.shutdown();

              Utils.sleep(1);

            } catch (Throwable e) {
              log.error(e.getMessage());
            }
          }
          blockchainMonitorExecutorService.shutdown();
          log.info("Blockchain Monitor has stopped working");
        });
  }

  public List<ResultLastBlock> insertBlocksAndTransactions(
      Node node, ResultLastBlock lastBlock, boolean updateGuiNow) {

    insertBlockEntity(lastBlock);

    if (updateGuiNow) {
      updateBlocksTabGui(lastBlock);
    }

    List<ResultLastBlock> shardsInBlock = MyLocalTonUtils.getShardsInBlock(lastBlock);

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

    shardsInBlock.stream()
        .filter(b -> (b.getSeqno().compareTo(BigInteger.ZERO) != 0))
        .forEach(shard -> dumpBlockTransactions(node, shard, updateGuiNow)); // txs from shards

    return shardsInBlock;
  }

  public void dumpBlockTransactions(Node node, ResultLastBlock lastBlock, boolean updateGuiNow) {

    BlockTransactionsExt blockTransactions =
        tonlib.getBlockTransactionsExt(lastBlock.getBlockIdExt(), 1000, null);

    log.debug(
        "found {} transactions in block {}",
        blockTransactions.getTransactions().size(),
        lastBlock.getShortBlockSeqno());

    for (RawTransaction rawTx : blockTransactions.getTransactions()) {
      org.ton.java.tlb.Transaction txDetails =
          org.ton.java.tlb.Transaction.deserialize(
              CellSlice.beginParse(Cell.fromBocBase64(rawTx.getData())));

      if (nonNull(txDetails)) {
        List<TxEntity> txEntities = extractTxsAndMsgs(lastBlock, txDetails); // todo
        txEntities.forEach(App.dbPool::insertTx);
        detectNewAccount(lastBlock, txDetails); // seems ok
        if (updateGuiNow) {
          updateTxTabGui(lastBlock, txDetails, txEntities);
        }
      }
    }

    //    for (ResultListBlockTransactions tx : txs) {
    //      Transaction txDetails =
    //          LiteClientParser.parseDumpTrans(
    //              liteClient.executeDumptrans(node, lastBlock, tx),
    //              settings.getUiSettings().isShowBodyInMessage());
    //      if (nonNull(txDetails)) {
    //
    //        List<TxEntity> txEntities = extractTxsAndMsgs(lastBlock, tx, txDetails);
    //
    //        txEntities.forEach(App.dbPool::insertTx);
    //
    //        detectNewAccount(lastBlock, tx, txDetails);
    //
    //        if (updateGuiNow) {
    //          updateTxTabGui(lastBlock, tx, txDetails, txEntities);
    //        }
    //      }
    //    }
  }

  private void detectNewAccount(ResultLastBlock lastBlock, org.ton.java.tlb.Transaction txDetails) {
    try {
      if ((txDetails.getOrigStatus().equals(AccountStates.NON_EXIST)
              && txDetails.getEndStatus().equals(AccountStates.UNINIT))
          || (txDetails.getOrigStatus().equals(AccountStates.UNINIT)
              && txDetails.getEndStatus().equals(AccountStates.UNINIT))
          || (txDetails.getOrigStatus().equals(AccountStates.UNINIT)
              && txDetails.getEndStatus().equals(AccountStates.ACTIVE))) {
        log.info(
            "New account detected! origStatus {}, endStatus {}, address {}," + " block {}",
            txDetails.getOrigStatus(),
            txDetails.getEndStatus(),
            txDetails.getAccountAddr(),
            lastBlock.getShortBlockSeqno());

        WalletEntity foundWallet =
            App.dbPool.findWallet(
                WalletPk.builder()
                    .wc(lastBlock.getWc())
                    .hexAddress(txDetails.getAccountAddr().toUpperCase())
                    .build());

        if (isNull(foundWallet)) {
          log.info(
              "insertNewAccountEntity new inserting {}", txDetails.getAccountAddr().toUpperCase());
          Address address =
              Address.of(lastBlock.getWc() + ":" + txDetails.getAccountAddr().toUpperCase());
          //
          WalletAddress walletAddress =
              WalletAddress.builder()
                  .wc(lastBlock.getWc())
                  .hexWalletAddress(txDetails.getAccountAddr().toUpperCase())
                  .fullWalletAddress(
                      lastBlock.getWc() + ":" + txDetails.getAccountAddr().toUpperCase())
                  .bounceableAddressBase64url(address.toString(true, true, true))
                  .nonBounceableAddressBase64Url(address.toString(true, true, false))
                  .bounceableAddressBase64(address.toString(true, false, true))
                  .nonBounceableAddressBase64(address.toString(true, false, true))
                  .build();

          WalletEntity walletEntity =
              WalletEntity.builder()
                  .wc(walletAddress.getWc())
                  .hexAddress(walletAddress.getHexWalletAddress().toUpperCase())
                  .wallet(walletAddress)
                  .createdAt(txDetails.getNow())
                  .build();

          App.dbPool.insertWallet(walletEntity);

          log.info(
              "insertNewAccountEntity new inserted {}", txDetails.getAccountAddr().toUpperCase());
        }
      }
    } catch (Throwable e) {
      log.error("Error executing insertNewAccountEntity: {}", e.getMessage());
      log.error(ExceptionUtils.getStackTrace(e));
    }
  }

  private void updateAccountsTabGui(WalletEntity walletEntity) {
    if (walletEntity == null || walletEntity.getAccountState() == null) {
      return;
    }

    //    log.debug(
    //        "updateAccountsTabGui, wallet account addr {}, state {}",
    //        walletEntity.getHexAddress(),
    //        walletEntity.getAccountState());

    if (!Boolean.TRUE.equals(autoScroll)) {
      return;
    }

    MainController c = fxmlLoader.getController();

    CompletableFuture.supplyAsync(
            () -> {
              FXMLLoader loader = new FXMLLoader(App.class.getResource("accountrow.fxml"));
              try {
                return (javafx.scene.Node) loader.load();
              } catch (IOException e) {
                log.error("Error loading accountrow.fxml file: {}", e.getMessage(), e);
                return null;
              }
            },
            ForkJoinPool.commonPool())
        .thenAccept(
            accountRow -> {
              if (accountRow == null) {
                return;
              }

              Platform.runLater(
                  () -> {
                    if (walletEntity.getWc() == -1L) {
                      accountRow.lookup("#accRowBorderPane").getStyleClass().add("row-pane-gray");
                      accountRow
                          .lookup("#hBoxDeletetn")
                          .getStyleClass()
                          .add("background-acc-delete-button-gray");
                      accountRow
                          .lookup("#hBoxSendBtn")
                          .getStyleClass()
                          .add("background-acc-send-button-gray");
                    }
                    //                    log.debug(
                    //                        "updateAccountsTabGui,showInGuiOnlyUniqueAccounts,
                    // account row {}",
                    //                        walletEntity);
                    showInGuiOnlyUniqueAccounts(walletEntity, c, accountRow);
                  });
            })
        .exceptionally(
            ex -> {
              log.error("Error updating accounts tab GUI", ex);
              Platform.runLater(
                  () -> App.mainController.showErrorMsg("Error updating accounts tab", 3));
              return null;
            });
  }

  private void emitResultMessage(WalletEntity walletEntity) {
    if (nonNull(walletEntity)) {
      if (WalletVersion.V1R1.equals(walletEntity.getWalletVersion())) {
        Platform.runLater(
            () ->
                emit(
                    new CustomNotificationEvent(
                        CustomEvent.Type.SUCCESS,
                        "Wallet " + walletEntity.getFullAddress() + " created",
                        3)));
      } else if (walletEntity.getSeqno() != -1L) {
        Platform.runLater(
            () ->
                emit(
                    new CustomNotificationEvent(
                        CustomEvent.Type.SUCCESS,
                        "Wallet " + walletEntity.getFullAddress() + " created",
                        3)));
      }
    } else {
      Platform.runLater(
          () ->
              emit(
                  new CustomNotificationEvent(
                      CustomEvent.Type.ERROR, "Error creating wallet. See logs for details.", 4)));
    }
  }

  private void showInGuiOnlyUniqueAccounts(
      WalletEntity walletEntity, MainController c, javafx.scene.Node accountRow) {
    String upperAddr = walletEntity.getWallet().getFullWalletAddress().toUpperCase();

    if (!concurrentAccountsHashMap.containsKey(upperAddr)) {
      log.debug("add to gui list 1 new account {}", upperAddr);

      if (concurrentAccountsHashMap.size() > 100) {
        int targetSize = 50;
        int toRemove = concurrentAccountsHashMap.size() - targetSize;
        Iterator<String> it = concurrentAccountsHashMap.keySet().iterator();
        while (it.hasNext() && toRemove > 0) {
          it.next();
          it.remove();
          toRemove--;
        }
      }

      concurrentAccountsHashMap.put(upperAddr, 1L);

      ((Label) accountRow.lookup("#hexAddrLabel"))
          .setText(MyLocalTonUtils.getNodeNameByWalletAddress(upperAddr) + "Hex:");

      populateAccountRowWithData(walletEntity, accountRow, "--------------------");

      log.debug("add to gui list 2 new account {}", upperAddr);

      ObservableList<javafx.scene.Node> items = c.accountsvboxid.getItems();
      int size = items.size();

      if (size > accountsScrollBarHighWaterMark.get()) {
        items.remove(size - 1);
      }

      items.add(0, accountRow);

      log.debug("accounts gui size {}", items.size());

    } else {
      log.debug("update gui list with account {}", upperAddr);
      if (walletEntity.getAccountState() != null) {
        //        log.debug(
        //            "update account details in gui {}, {}",
        //            walletEntity.getFullAddress(),
        //            walletEntity.getAccountState());
        for (javafx.scene.Node node : c.accountsvboxid.getItems()) {
          Label hexAddrLabel = (Label) node.lookup("#hexAddr");
          if (hexAddrLabel != null && hexAddrLabel.getText().equals(upperAddr)) {
            populateAccountRowWithData(walletEntity, node, "--------------------");
            break;
          }
        }
      }
    }
  }

  public void populateAccountRowWithData(
      WalletEntity walletEntity, javafx.scene.Node accountRow, String searchFor) {
    try {
      if (nonNull(walletEntity.getWallet())) {
        ((Label) accountRow.lookup("#hexAddrLabel"))
            .setText(
                MyLocalTonUtils.getNodeNameByWalletAddress(
                        walletEntity.getWallet().getFullWalletAddress().toUpperCase())
                    + "Hex:");
      } else {
        ((Label) accountRow.lookup("#hexAddrLabel")).setText("Hex:");
      }

      ((Label) accountRow.lookup("#hexAddr"))
          .setText(walletEntity.getWallet().getFullWalletAddress().toUpperCase());
      if (((Label) accountRow.lookup("#hexAddr")).getText().contains(searchFor)) {
        accountRow
            .lookup("#hexAddr")
            .setStyle(accountRow.lookup("#hexAddr").getStyle() + FOUND_COLOR_HIHGLIGHT);
      }

      ((Label) accountRow.lookup("#b64Addr"))
          .setText(walletEntity.getWallet().getBounceableAddressBase64());
      ((Label) accountRow.lookup("#b64urlAddr"))
          .setText(walletEntity.getWallet().getBounceableAddressBase64url());
      ((Label) accountRow.lookup("#nb64Addr"))
          .setText(walletEntity.getWallet().getNonBounceableAddressBase64());
      ((Label) accountRow.lookup("#nb64urlAddr"))
          .setText(walletEntity.getWallet().getNonBounceableAddressBase64Url());

      ((Label) accountRow.lookup("#createdat"))
          .setText(MyLocalTonUtils.toLocal((walletEntity.getCreatedAt())));

      if (isNull(walletEntity.getWalletVersion())) {
        ((Label) accountRow.lookup("#type")).setText("Unknown");
      } else {
        ((Label) accountRow.lookup("#type")).setText(walletEntity.getWalletVersion().getValue());
      }

      if (walletEntity.getWallet().getSubWalletId() != -1) {
        accountRow.lookup("#walledId").setVisible(true);
        ((Label) accountRow.lookup("#walledId"))
            .setText("Wallet ID " + walletEntity.getWallet().getSubWalletId());
      } else { // wallets V1 and V2
        accountRow.lookup("#walledId").setVisible(false);
        ((Label) accountRow.lookup("#walledId")).setText("-1");
      }

      if (walletEntity.getSeqno() >= 0
          && (nonNull(walletEntity.getWalletVersion())
              && !walletEntity.getWalletVersion().equals(WalletVersion.config))) {
        accountRow.lookup("#seqno").setVisible(true);
        ((Label) accountRow.lookup("#seqno")).setText("Seqno " + walletEntity.getSeqno());
      } else {
        accountRow.lookup("#seqno").setVisible(false);
        ((Label) accountRow.lookup("#seqno")).setText("Seqno -1");
      }

      //            String value = walletEntity.getAccountState().getBalance();
      //            String formattedBalance2 = String.format("%,.9f", value);
      //      BigDecimal balance = new BigDecimal(walletEntity.getAccountState().getBalance());
      String formattedBalance = Utils.formatNanoValue(walletEntity.getAccountState().getBalance());

      ((Label) accountRow.lookup("#balance")).setText(formattedBalance);

      String status =
          StringUtils.isEmpty(walletEntity.getAccountStatus())
              ? ""
              : StringUtils.capitalize(walletEntity.getAccountStatus());
      ((Label) accountRow.lookup("#status")).setText(status);

      if (settings.getConfigSmcAddrHex().contains(walletEntity.getHexAddress())
          || settings.getMainWalletAddrFull().contains(walletEntity.getHexAddress())
          || isNull(walletEntity.getWallet().getPrivateKeyHex())) {
        accountRow.lookup("#walletDeleteBtn").setDisable(true);
        (accountRow.lookup("#hBoxDeletetn")).getStyleClass().clear();
      }
      if (status.equals(UNINITIALIZED)
          || status.equals(FROZEN)
          || settings.getConfigSmcAddrHex().contains(walletEntity.getHexAddress().toUpperCase())
          || isNull(walletEntity.getWallet().getPrivateKeyHex())
      // || isNull(walletEntity.getWallet().getPrivateKeyLocation())
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

  private void updateTxTabGui(
      ResultLastBlock lastBlock,
      org.ton.java.tlb.Transaction txDetails,
      List<TxEntity> txEntities) {

    if (Boolean.TRUE.equals(autoScroll)) {

      MainController c = fxmlLoader.getController();

      Platform.runLater(
          () -> {
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

              showInGuiOnlyUniqueTxs(lastBlock, c, txE, txRow);
              // show in gui only unique values. some might come from scroll event
            }
          });
    }
  }

  private void showInGuiOnlyUniqueTxs(
      ResultLastBlock lastBlock, MainController c, TxEntity txE, javafx.scene.Node txRow) {
    String uniqueKey =
        lastBlock.getShortBlockSeqno() + txE.getTypeTx() + txE.getTypeMsg() + txE.getTxHash();
    log.debug("showInGuiOnlyUniqueTxs {}", uniqueKey);

    if (!concurrentTxsHashMap.containsKey(uniqueKey)) {

      if (concurrentTxsHashMap.size() > 800) {
        int targetSize = 400;
        int toRemove = concurrentTxsHashMap.size() - targetSize;
        Iterator<String> it = concurrentTxsHashMap.keySet().iterator();
        while (it.hasNext() && toRemove > 0) {
          it.next();
          it.remove();
          toRemove--;
        }
      }

      concurrentTxsHashMap.put(uniqueKey, lastBlock.getCreatedAt());

      populateTxRowWithData(lastBlock.getShortBlockSeqno(), txRow, txE);

      ObservableList<javafx.scene.Node> items = c.transactionsvboxid.getItems();
      int size = items.size();

      if (size > txsScrollBarHighWaterMark.get()) {
        items.remove(size - 1);
      }

      items.add(0, txRow);
    }
  }

  public void applyTxGuiFilters(List<TxEntity> txEntity) {
    if (!settings.getUiSettings().isShowMainConfigTransactions()) {
      txEntity.removeIf(
          t ->
              (settings.getMainWalletAddrFull().contains(t.getFrom())
                  || settings.getElectorSmcAddrHex().contains(t.getFrom())
                  || settings.getConfigSmcAddrHex().contains(t.getFrom())));
    }

    if (!settings.getUiSettings().isShowTickTockTransactions()) {
      txEntity.removeIf(t -> t.getTx().getDescription().getType().equals("tick"));
      txEntity.removeIf(t -> t.getTx().getDescription().getType().equals("tock"));
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

    ((Label) txRow.lookup("#txid"))
        .setText(
            txEntity.getTxHash().substring(0, 8) + "..." + txEntity.getTxHash().substring(56, 64));
    if (((Label) txRow.lookup("#txidHidden")).getText().equals(searchFor)) {
      txRow.lookup("#txid").setStyle(txRow.lookup("#txid").getStyle() + FOUND_COLOR_HIHGLIGHT);
    }
    ((Label) txRow.lookup("#from")).setText(txEntity.getFrom());
    if (searchFor.length() >= 64) {
      if (((Label) txRow.lookup("#from"))
          .getText()
          .contains(StringUtils.substring(searchFor, 4, -2))) {
        txRow.lookup("#from").setStyle(txRow.lookup("#from").getStyle() + FOUND_COLOR_HIHGLIGHT);
      }
    }
    if (((Label) txRow.lookup("#from")).getText().equals(searchFor)) {
      txRow.lookup("#from").setStyle(txRow.lookup("#from").getStyle() + FOUND_COLOR_HIHGLIGHT);
    }

    ((Label) txRow.lookup("#to")).setText(txEntity.getTo());
    if (searchFor.length() >= 64) {
      if (((Label) txRow.lookup("#to"))
          .getText()
          .contains(StringUtils.substring(searchFor, 4, -2))) {
        txRow.lookup("#to").setStyle(txRow.lookup("#to").getStyle() + FOUND_COLOR_HIHGLIGHT);
      }
    }
    if (((Label) txRow.lookup("#to")).getText().equals(searchFor)) {
      txRow.lookup("#to").setStyle(txRow.lookup("#to").getStyle() + FOUND_COLOR_HIHGLIGHT);
    }
    ((Label) txRow.lookup("#amount")).setText(Utils.formatNanoValue(txEntity.getAmount()));

    ((Label) txRow.lookup("#fees"))
        .setText(Utils.formatNanoValue(txEntity.getTx().getTotalFees().getCoins()));
    ((Label) txRow.lookup("#time"))
        .setText(
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(new Timestamp(txEntity.getTx().getNow() * 1000).getTime())));

    showButtonWithMessage(txRow, txEntity);
  }

  public void populateTxRowWithData(
      String shortBlockSeqno, javafx.scene.Node txRow, TxEntity txEntity) {

    ((Label) txRow.lookup("#block")).setText(shortBlockSeqno);
    ((Label) txRow.lookup("#typeTx")).setText(txEntity.getTypeTx());
    ((Label) txRow.lookup("#typeMsg")).setText(txEntity.getTypeMsg());

    ((Label) txRow.lookup("#status")).setText(txEntity.getStatus());
    ((Label) txRow.lookup("#txidHidden")).setText(txEntity.getTxHash());
    ((Label) txRow.lookup("#txAccAddrHidden")).setText(txEntity.getAccountAddress());
    ((Label) txRow.lookup("#txLt")).setText(txEntity.getTxLt().toString());
    ((Label) txRow.lookup("#txid"))
        .setText(
            txEntity.getTxHash().substring(0, 8) + "..." + txEntity.getTxHash().substring(56, 64));

    ((Label) txRow.lookup("#from")).setText(txEntity.getFrom());
    ((Label) txRow.lookup("#to")).setText(txEntity.getTo());
    ((Label) txRow.lookup("#amount")).setText(Utils.formatNanoValue(txEntity.getAmount()));

    ((Label) txRow.lookup("#fees"))
        .setText(Utils.formatNanoValue(txEntity.getTx().getTotalFees().getCoins()));
    ((Label) txRow.lookup("#time"))
        .setText(
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(new Timestamp(txEntity.getCreatedAt() * 1000).getTime())));

    showButtonWithMessage(txRow, txEntity);
  }

  private void showButtonWithMessage(javafx.scene.Node txRow, TxEntity txEntity) {
    /* todo
    String msg = null;
    if (txEntity.getTypeTx().contains("Message")
        && !txEntity.getTypeMsg().contains("External In")) {
      if (!(txEntity.getTx().getInMsg().getBody().getCells().isEmpty())
          && (txEntity.getTx().getInMsg().getValue().getToncoins().compareTo(BigDecimal.ZERO) > 0)
          && !txEntity.getTx().getInMsg().getBody().getCells().get(0).equals("FFFFFFFF")) {

        msg =
            txEntity.getTx().getInMsg().getBody().getCells().stream()
                .map(
                    c -> {
                      try {
                        return new String(Hex.decodeHex(c.toCharArray()));
                      } catch (DecoderException e) {
                        log.debug("Cannot convert hex msg to string");
                        return "      Cannot convert hex msg to string";
                      }
                    })
                .collect(Collectors.joining());

      } else if ((!txEntity.getTx().getOutMsgs().isEmpty()
          && !txEntity.getTx().getOutMsgs().get(0).getBody().getCells().isEmpty())) {
        if (txEntity
                    .getTx()
                    .getOutMsgs()
                    .get(0)
                    .getValue()
                    .getToncoins()
                    .compareTo(BigDecimal.ZERO)
                > 0
            && !txEntity
                .getTx()
                .getOutMsgs()
                .get(0)
                .getBody()
                .getCells()
                .get(0)
                .equals("FFFFFFFF")) {

          msg =
              txEntity.getTx().getOutMsgs().get(0).getBody().getCells().stream()
                  .map(
                      c -> {
                        try {
                          return new String(Hex.decodeHex(c.toCharArray()));
                        } catch (DecoderException e) {
                          log.debug("Cannot convert hex msg to string");
                          return "     Cannot convert hex msg to string";
                        }
                      })
                  .collect(Collectors.joining());
        }
      }
    }
    if (StringUtils.isNotEmpty(msg) && msg.length() > 4) {
      msg = msg.substring(4);

      if (StringUtils.isAlphanumericSpace(msg) || StringUtils.isAsciiPrintable(msg)) {
        javafx.scene.Node txMsgBtn = txRow.lookup("#txMsgBtn");
        if (nonNull(txMsgBtn)) {
          txMsgBtn.setVisible(true);
          txMsgBtn.setUserData(msg);

          txMsgBtn.setOnMouseClicked(
              mouseEvent -> {
                log.info(
                    "in msg btn clicked on block {}, {}",
                    ((Label) txRow.lookup("#block")).getText(),
                    txRow.lookup("#txMsgBtn").getUserData());
                mainController.showMessage(String.valueOf(txRow.lookup("#txMsgBtn").getUserData()));
              });
        } else {
          log.error("cannot get txMsgBtn");
        }
      }
    }
    */
  }

  public List<TxEntity> extractTxsAndMsgs(
      ResultLastBlock lastBlock, org.ton.java.tlb.Transaction txDetails) {
    List<TxEntity> txEntity = new ArrayList<>();
    String to;
    String from;
    BigInteger amount = BigInteger.ZERO;
    String txType;
    String msgType;
    String status;
    //    AccountStates status;

    try {
      TransactionDescription txDesc = txDetails.getDescription();
      from = lastBlock.getWc() + ":" + txDetails.getAccountAddr().toUpperCase();
      to = "";
      txType = "Tx";
      msgType = txDesc.getType();
      status = txDetails.getEndStatus().toString();

      log.debug(
          "adding tx {} {} {} {} LT={} NOW={} seqno={}",
          txDetails.getAccountAddr(),
          txType,
          msgType,
          txDetails.getPrevTxHash(),
          txDetails.getLt(),
          txDetails.getNow(),
          lastBlock.getSeqno());
      txEntity.add(
          TxEntity.builder()
              .wc(lastBlock.getWc())
              .shard(lastBlock.getShard())
              .seqno(lastBlock.getSeqno())
              .blockRootHash(lastBlock.getRootHash())
              .blockFileHash(lastBlock.getFileHash())
              .txHash(txDetails.getPrevTxHash())
              .status(status)
              .createdAt(txDetails.getNow())
              .accountAddress(txDetails.getAccountAddr())
              .txLt(txDetails.getLt())
              .typeTx(txType)
              .typeMsg(msgType)
              .from(from)
              .to(to)
              .fromForSearch(from)
              .toForSearch(to)
              .amount(amount)
              .fees(txDetails.getTotalFees().getCoins())
              .tx(txDetails)
              .message(null)
              .build());

      // insert out msgs first
      if (txDetails.getOutMsgCount() != 0L) {
        for (org.ton.java.tlb.Message outMsg : txDetails.getInOut().getOutMessages()) {
          txType = "Message";
          msgType = outMsg.getInfo().getType();
          from = outMsg.getInfo().getSourceAddress().toUpperCase();
          to = outMsg.getInfo().getDestinationAddress().toUpperCase();
          amount = outMsg.getInfo().getValueCoins();
          status = "";

          log.debug(
              "adding out-msg {} {} {} {} LT={} NOW={} seqno={}",
              txDetails.getAccountAddr(),
              txType,
              msgType,
              txDetails.getPrevTxHash(),
              txDetails.getLt(),
              txDetails.getNow(),
              lastBlock.getSeqno());

          txEntity.add(
              TxEntity.builder()
                  .wc(lastBlock.getWc())
                  .shard(lastBlock.getShard())
                  .seqno(lastBlock.getSeqno())
                  .blockRootHash(lastBlock.getRootHash())
                  .blockFileHash(lastBlock.getFileHash())
                  .txHash(txDetails.getPrevTxHash())
                  .status(status)
                  .createdAt(txDetails.getNow())
                  .accountAddress(txDetails.getAccountAddr())
                  .txLt(txDetails.getLt())
                  .typeTx(txType)
                  .typeMsg(msgType)
                  .from(from)
                  .to(to)
                  .fromForSearch(from)
                  .toForSearch(to)
                  .amount(amount)
                  .fees(
                      (outMsg.getInfo() instanceof InternalMessageInfo)
                          ? ((InternalMessageInfo) outMsg.getInfo()).getTotalFees()
                          : BigInteger.ZERO) // total fee in out msg = fwd+ihr+import fee
                  .tx(txDetails)
                  .message(outMsg)
                  .build());
        }
      } // end out msgs

      // in msgs
      if (nonNull(txDetails.getInOut().getIn())) {
        org.ton.java.tlb.Message inMsg = txDetails.getInOut().getIn();
        from = inMsg.getInfo().getSourceAddress().toUpperCase();
        to = inMsg.getInfo().getDestinationAddress().toUpperCase();
        amount = inMsg.getInfo().getValueCoins();
        txType = "Message";
        msgType = inMsg.getInfo().getType();
        status = "";

        log.debug(
            "adding in-msg {} {} {} {} LT={} NOW={} seqno={}",
            txDetails.getAccountAddr(),
            txType,
            msgType,
            txDetails.getPrevTxHash(),
            txDetails.getLt(),
            txDetails.getNow(),
            lastBlock.getSeqno());

        txEntity.add(
            TxEntity.builder()
                .wc(lastBlock.getWc())
                .shard(lastBlock.getShard())
                .seqno(lastBlock.getSeqno())
                .blockRootHash(lastBlock.getRootHash())
                .blockFileHash(lastBlock.getFileHash())
                .txHash(txDetails.getPrevTxHash())
                .status(status)
                .createdAt(txDetails.getNow())
                .accountAddress(txDetails.getAccountAddr())
                .txLt(txDetails.getLt())
                .typeTx(txType)
                .typeMsg(msgType)
                .from(from)
                .to(to)
                .fromForSearch(from)
                .toForSearch(to)
                .amount(amount)
                .fees(
                    (inMsg.getInfo() instanceof InternalMessageInfo)
                        ? ((InternalMessageInfo) inMsg.getInfo()).getTotalFees()
                        : BigInteger.ZERO) // total fee in out msg = fwd+ihr+import fee
                .tx(txDetails)
                .message(inMsg)
                .build());
      }
      return txEntity;
    } catch (Exception e) {
      log.error("Error inserting tx entity", e);
      return null;
    }
  }

  private void updateTopInfoBarGui(int shardsNum) {

    MainController c = fxmlLoader.getController();
    Platform.runLater(
        () -> {
          try {
            // update GUI
            c.shardsNum.setSecondaryText(String.valueOf(shardsNum));
            c.liteClientInfo.setSecondaryText(
                String.format(
                    "%s:%s",
                    settings.getGenesisNode().getPublicIp(),
                    settings.getGenesisNode().getLiteServerPort()));
          } catch (Exception e) {
            log.error("error updating top bar gui, {}", e.getMessage());
          }
        });
  }

  private void updateBlocksTabGui(ResultLastBlock lastBlock) {
    MainController c = fxmlLoader.getController();

    CompletableFuture.supplyAsync(
            () -> {
              if (!Boolean.TRUE.equals(autoScroll)) {
                return null;
              }

              try {
                FXMLLoader blockRowLoader = new FXMLLoader(App.class.getResource("blockrow.fxml"));
                return (javafx.scene.Node) blockRowLoader.load();
              } catch (IOException e) {
                log.error("Error loading blockrow.fxml file: {}", e.getMessage(), e);
                return null;
              }
            },
            ForkJoinPool.commonPool())
        .thenAccept(
            blockRow -> {
              Platform.runLater(
                  () -> {
                    try {
                      if (lastBlock.getWc().equals(-1L)) {
                        c.currentBlockNum.setSecondaryText(lastBlock.getSeqno().toString());
                      }

                      if (Boolean.TRUE.equals(autoScroll) && blockRow != null) {
                        if (lastBlock.getWc() == -1L) {
                          blockRow
                              .lookup("#blockRowBorderPane")
                              .getStyleClass()
                              .add("row-pane-gray");
                        }

                        showInGuiOnlyUniqueBlocks(c, lastBlock, blockRow);
                      }
                    } catch (Exception e) {
                      log.error("Error displaying block: {}", e.getMessage(), e);
                    }
                  });
            });
  }

  private void showInGuiOnlyUniqueBlocks(
      MainController c, ResultLastBlock finalLastBlock, javafx.scene.Node blockRow) {
    String shortBlockSeq = finalLastBlock.getShortBlockSeqno();

    if (!concurrentBlocksHashMap.containsKey(shortBlockSeq)) {

      if (concurrentBlocksHashMap.size() > 400) {
        int targetSize = 200;
        int toRemove = concurrentBlocksHashMap.size() - targetSize;
        Iterator<String> it = concurrentBlocksHashMap.keySet().iterator();
        while (it.hasNext() && toRemove > 0) {
          it.next();
          it.remove();
          toRemove--;
        }
      }

      concurrentBlocksHashMap.put(shortBlockSeq, finalLastBlock.getCreatedAt());

      populateBlockRowWithData(finalLastBlock, blockRow, null);

      ObservableList<javafx.scene.Node> items = c.blockslistviewid.getItems();

      if (items.size() > blocksScrollBarHighWaterMark.get()) {
        items.remove(items.size() - 1);
      }

      items.add(0, blockRow);
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

        ResultLastBlock resultLastBlock =
            ResultLastBlock.builder()
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
        log.debug(
            "adding block to found gui {} roothash {}", block.getSeqno(), block.getRoothash());

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

  public void showFoundTxsInGui(
      JFXListView<javafx.scene.Node> listView,
      List<TxEntity> foundTxs,
      String searchFor,
      String accountAddr) {

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
      emit(
          new CustomSearchEvent(
              CustomEvent.Type.SEARCH_SIZE_ACCOUNTS_TXS, txRows.size(), accountAddr));
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

  public void populateBlockRowWithData(
      ResultLastBlock finalLastBlock, javafx.scene.Node blockRow, String searchFor) {

    ((Label) blockRow.lookup("#wc")).setText(finalLastBlock.getWc().toString());
    ((Label) blockRow.lookup("#shard")).setText(finalLastBlock.getShard());
    ((Label) blockRow.lookup("#seqno")).setText(finalLastBlock.getSeqno().toString());
    if (((Label) blockRow.lookup("#seqno")).getText().equals(searchFor)) {
      blockRow
          .lookup("#seqno")
          .setStyle(blockRow.lookup("#seqno").getStyle() + FOUND_COLOR_HIHGLIGHT);
    }
    ((Label) blockRow.lookup("#filehash")).setText(finalLastBlock.getFileHash().toUpperCase());
    ((Label) blockRow.lookup("#roothash")).setText(finalLastBlock.getRootHash().toUpperCase());
    if (((Label) blockRow.lookup("#filehash")).getText().equals(searchFor)) {
      blockRow
          .lookup("#filehash")
          .setStyle(blockRow.lookup("#filehash").getStyle() + FOUND_COLOR_HIHGLIGHT);
    }
    if (((Label) blockRow.lookup("#roothash")).getText().equals(searchFor)) {
      blockRow
          .lookup("#roothash")
          .setStyle(blockRow.lookup("#roothash").getStyle() + FOUND_COLOR_HIHGLIGHT);
    }
    SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");

    ((Label) blockRow.lookup("#createdatDate"))
        .setText(
            formatterDate.format(
                new Date(new Timestamp(finalLastBlock.getCreatedAt() * 1000).getTime())));
    ((Label) blockRow.lookup("#createdatTime"))
        .setText(
            formatterTime.format(
                new Date(new Timestamp(finalLastBlock.getCreatedAt() * 1000).getTime())));
  }

  public void insertBlockEntity(ResultLastBlock lastBlock) {
    log.debug("inserting block into db {} ", lastBlock);
    BlockEntity block =
        BlockEntity.builder()
            .createdAt(lastBlock.getCreatedAt())
            .wc(lastBlock.getWc())
            .shard(lastBlock.getShard())
            .seqno(lastBlock.getSeqno())
            .filehash(lastBlock.getFileHash().toUpperCase())
            .roothash(lastBlock.getRootHash().toUpperCase())
            .build();

    App.dbPool.insertBlock(block);
  }

  public void runAccountsMonitor() {

    Executors.newSingleThreadScheduledExecutor()
        .scheduleWithFixedDelay(
            () -> {
              Thread.currentThread().setName("MyLocalTon - Accounts Monitor");
              try {
                for (WalletEntity wallet : App.dbPool.getAllWallets()) {
                  if (Main.appActive.get()) {
                    Address address = Address.of(wallet.getWc() + ":" + wallet.getHexAddress());
                    RawAccountState accountState = tonlib.getRawAccountState(address);
                    WalletVersion walletVersion =
                        MyLocalTonUtils.detectWalletVersion(accountState.getCode(), address);

                    long subWalletId = -1;
                    long seqno = -1;

                    try {
                      seqno = tonlib.getSeqno(address);
                    } catch (Throwable ignored) {
                      // log.debug("can't detect contract's seqno for address {}", address.toRaw());
                    }

                    try {
                      subWalletId =
                          tonlib.getAccountState(address).getAccount_state().getWallet_id();
                      if (subWalletId == 0) {
                        subWalletId = -1L;
                      }
                    } catch (Throwable ignored) {
                      log.debug("can't detect contract's walletId for address {}", address.toRaw());
                    }

                    log.debug(
                        "runAccountsMonitor: {}, {}, {}, {}",
                        walletVersion,
                        seqno,
                        subWalletId,
                        accountState.getBalance());

                    App.dbPool.updateWalletStateAndSeqno(
                        wallet, accountState, seqno, walletVersion);

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
            },
            0L,
            6L,
            TimeUnit.SECONDS);
  }

  public void reap(Node node) {
    try {

      if (isNull(node.getWalletAddress())) {
        log.error("Reaping rewards. {} wallet is not present.", node.getNodeName());
        return;
      }

      ResultComputeReturnStake result =
          LiteClientParser.parseRunMethodComputeReturnStake(
              LiteClient.getInstance(LiteClientEnum.GLOBAL)
                  .executeComputeReturnedStake(
                      node,
                      settings.getElectorSmcAddrHex(),
                      node.getWalletAddress().getHexWalletAddress()));

      if (result.getStake().compareTo(BigInteger.ZERO) > 0) {
        log.info(
            "Reaping rewards. {} reward size is {}, send request for stake recovery",
            node.getNodeName(),
            result.getStake());

        // create recover-query.boc
        new Fift().createRecoverStake(node);

        // send stake and validator-query.boc to elector
        SendToncoinsParam sendToncoinsParam =
            SendToncoinsParam.builder()
                .executionNode(node)
                .workchain(node.getWalletAddress().getWc())
                .fromWallet(node.getWalletAddress())
                .fromWalletVersion(WalletVersion.V3R2) // default validator wallet type
                .fromSubWalletId(settings.getWalletSettings().getDefaultSubWalletId())
                .destAddr(settings.getElectorSmcAddrHex())
                .amount(BigInteger.valueOf(ONE_BLN)) // (BigDecimal.valueOf(1L))
                .bocLocation(node.getTonBinDir() + "recover-query.boc")
                .build();

        new MyWallet().sendTonCoins(sendToncoinsParam);

        // Basic rewards statistics. Better to fetch from the DB actual values, since recover stake
        // may fail e.g.

        node.setLastRewardCollected(result.getStake());
        node.setTotalRewardsCollected(node.getTotalRewardsCollected().add(result.getStake()));

        // returned stake - 10001 + 1
        // big time todo review
        node.setLastPureRewardCollected(
            result
                .getStake()
                //                .multiply(BigInteger.valueOf(ONE_BLN))
                .subtract(
                    node.getDefaultValidatorStake()
                    //                        .subtract(BigInteger.valueOf(ONE_BLN))
                    //                        .multiply(BigInteger.valueOf(ONE_BLN)))
                    ));

        node.setTotalPureRewardsCollected(
            node.getTotalPureRewardsCollected().add(node.getLastPureRewardCollected()));
        node.setElectionsRipped(node.getElectionsRipped().add(BigInteger.ONE));
        node.setAvgPureRewardCollected(
            node.getTotalPureRewardsCollected().divide(node.getElectionsRipped()));

        saveSettingsToGson();
      } else {
        log.info(
            "Reaping rewards. {} reward size is {}, nothing to reap.",
            node.getNodeName(),
            result.getStake());
      }
    } catch (Exception e) {
      log.error("Error reaping rewards. Error {}", e.getMessage());
      log.error(ExceptionUtils.getStackTrace(e));
    }
  }

  private void updateReapedValuesTab(Node node) {
    try {
      log.debug(
          "{} updating reaped values {}",
          node.getNodeName(),
          Utils.formatNanoValue(node.getLastRewardCollected()));

      switch (node.getNodeName()) {
        case "genesis":
          mainController.validator1totalCollected.setText(
              Utils.formatNanoValue(node.getTotalRewardsCollected(), 2));
          mainController.validator1LastCollected.setText(
              Utils.formatNanoValue(node.getLastRewardCollected(), 2));
          mainController.validator1TotalRewardsPure.setText(
              Utils.formatNanoValue(node.getTotalPureRewardsCollected(), 2));
          mainController.validator1LastRewardPure.setText(
              Utils.formatNanoValue(node.getLastPureRewardCollected(), 2));
          mainController.validator1AvgPureReward.setText(
              Utils.formatNanoValue(node.getAvgPureRewardCollected(), 2));
          mainController.participatedInElections1.setText(
              node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
          break;
        case "node2":
          mainController.validator2totalCollected.setText(
              Utils.formatNanoValue(node.getTotalRewardsCollected(), 2));
          mainController.validator2LastCollected.setText(
              Utils.formatNanoValue(node.getLastRewardCollected(), 2));
          mainController.validator2TotalRewardsPure.setText(
              Utils.formatNanoValue(node.getTotalPureRewardsCollected(), 2));
          mainController.validator2LastRewardPure.setText(
              Utils.formatNanoValue(node.getLastPureRewardCollected(), 2));
          mainController.validator2AvgPureReward.setText(
              Utils.formatNanoValue(node.getAvgPureRewardCollected(), 2));
          mainController.participatedInElections2.setText(
              node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
          break;
        case "node3":
          mainController.validator3totalCollected.setText(
              Utils.formatNanoValue(node.getTotalRewardsCollected(), 2));
          mainController.validator3LastCollected.setText(
              Utils.formatNanoValue(node.getLastRewardCollected(), 2));
          mainController.validator3TotalRewardsPure.setText(
              Utils.formatNanoValue(node.getTotalPureRewardsCollected(), 2));
          mainController.validator3LastRewardPure.setText(
              Utils.formatNanoValue(node.getLastPureRewardCollected(), 2));
          mainController.validator3AvgPureReward.setText(
              Utils.formatNanoValue(node.getAvgPureRewardCollected(), 2));
          mainController.participatedInElections3.setText(
              node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
          break;
        case "node4":
          mainController.validator4totalCollected.setText(
              Utils.formatNanoValue(node.getTotalRewardsCollected(), 2));
          mainController.validator4LastCollected.setText(
              Utils.formatNanoValue(node.getLastRewardCollected(), 2));
          mainController.validator4TotalRewardsPure.setText(
              Utils.formatNanoValue(node.getTotalPureRewardsCollected(), 2));
          mainController.validator4LastRewardPure.setText(
              Utils.formatNanoValue(node.getAvgPureRewardCollected(), 2));
          mainController.validator4AvgPureReward.setText(
              Utils.formatNanoValue(node.getAvgPureRewardCollected(), 2));
          mainController.participatedInElections4.setText(
              node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
          break;
        case "node5":
          mainController.validator5totalCollected.setText(
              Utils.formatNanoValue(node.getTotalRewardsCollected(), 2));
          mainController.validator5LastCollected.setText(
              Utils.formatNanoValue(node.getLastRewardCollected(), 2));
          mainController.validator5TotalRewardsPure.setText(
              Utils.formatNanoValue(node.getTotalPureRewardsCollected(), 2));
          mainController.validator5LastRewardPure.setText(
              Utils.formatNanoValue(node.getLastPureRewardCollected(), 2));
          mainController.validator5AvgPureReward.setText(
              Utils.formatNanoValue(node.getAvgPureRewardCollected(), 2));
          mainController.participatedInElections5.setText(
              node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
          break;
        case "node6":
          mainController.validator6totalCollected.setText(
              Utils.formatNanoValue(node.getTotalRewardsCollected(), 2));
          mainController.validator6LastCollected.setText(
              Utils.formatNanoValue(node.getLastRewardCollected(), 2));
          mainController.validator6TotalRewardsPure.setText(
              Utils.formatNanoValue(node.getTotalPureRewardsCollected(), 2));
          mainController.validator6LastRewardPure.setText(
              Utils.formatNanoValue(node.getLastPureRewardCollected(), 2));
          mainController.validator6AvgPureReward.setText(
              Utils.formatNanoValue(node.getAvgPureRewardCollected(), 2));
          mainController.participatedInElections6.setText(
              node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
          break;
        case "node7":
          mainController.validator7totalCollected.setText(
              Utils.formatNanoValue(node.getTotalRewardsCollected(), 2));
          mainController.validator7LastCollected.setText(
              Utils.formatNanoValue(node.getLastRewardCollected(), 2));
          mainController.validator7TotalRewardsPure.setText(
              Utils.formatNanoValue(node.getTotalPureRewardsCollected(), 2));
          mainController.validator7LastRewardPure.setText(
              Utils.formatNanoValue(node.getLastPureRewardCollected(), 2));
          mainController.validator7AvgPureReward.setText(
              Utils.formatNanoValue(node.getAvgPureRewardCollected(), 2));
          mainController.participatedInElections7.setText(
              node.getElectionsCounter().size() + " (" + node.getElectionsRipped() + ")");
          break;
        default:
          throw new Error("Unknown node name");
      }
    } catch (Throwable te) {
      log.error("Error updating validation rewards: {}", te.getMessage());
    }
  }

  public void createValidatorPubKeyAndAdnlAddress(Node node, long electionId) throws Exception {
    log.debug("{} creating validator PubKey and ADNL address", node.getNodeName());
    long electionEnd = electionId + 600;

    createSigningKeyForValidation(node, electionId, electionEnd);
    createAdnlKeyForValidation(node, node.getValidationSigningKey(), electionEnd);

    node.setValidationPubKeyAndAdnlCreated(true); // not used

    saveSettingsToGson();
  }

  void createSigningKeyForValidation(Node node, long electionId, long electionEnd)
      throws ExecutionException, InterruptedException {
    ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();

    String signingKey = validatorEngineConsole.generateNewNodeKey(node);
    String signingPubKey = validatorEngineConsole.exportPubKey(node, signingKey);

    log.debug(
        "{} signingKey {}, new signingPubKey {} for current elections",
        node.getNodeName(),
        signingKey,
        signingPubKey);

    node.setValidationSigningKey(signingKey);
    node.setValidationSigningPubKey(signingPubKey);
    validatorEngineConsole.addPermKey(node, signingKey, electionId, electionEnd);
    validatorEngineConsole.addTempKey(node, signingKey, electionEnd);
  }

  void createAdnlKeyForValidation(Node node, String signingKey, long electionEnd)
      throws ExecutionException, InterruptedException {
    ValidatorEngineConsole validatorEngineConsole = new ValidatorEngineConsole();
    String adnlKey = validatorEngineConsole.generateNewNodeKey(node);
    log.debug("{} new adnlKey {} for current elections", node.getNodeName(), adnlKey);

    node.setPrevValidationAndlKey(node.getValidationAndlKey());
    node.setValidationAndlKey(adnlKey); // shown on validator tab as ADNL address

    validatorEngineConsole.addAdnl(node, adnlKey);
    validatorEngineConsole.addValidatorAddr(node, signingKey, adnlKey, electionEnd);
    // validatorEngineConsole.getStats(node);
  }

  public void createFullnode(Node node, boolean enableLiteServer, boolean start) throws Exception {
    if (Files.exists(Paths.get(node.getTonDbArchiveDir()))) {
      log.info("{} already created, just start it", node.getNodeName());
      if (start) {
        new ValidatorEngine().startValidator(node, node.getNodeGlobalConfigLocation());
      }
      return;
    }
    log.info("creating new {}", node.getNodeName());
    node.extractBinaries();

    ValidatorEngine validatorEngine = new ValidatorEngine();
    validatorEngine.generateValidatorKeys(node, false);
    validatorEngine.initFullnode(node, settings.getGenesisNode().getNodeGlobalConfigLocation());

    if (!node.getNodeName().contains("genesis")) {
      node.setFlag("cloning");
      if (SystemUtils.IS_OS_WINDOWS) {
        //               on Windows locked files cannot be copied. As an option, we can shut down
        // genesis node, copy the files and start it again.
        //               but there is a connection issue with this on Windows.
        //               As an alternative, we can exclude locked files (LOCK), that's why
        // Utils.copyDirectory is used.
        FileUtils.copyDirectory(
            new File(settings.getGenesisNode().getTonBinDir() + "/zerostate"),
            new File(node.getTonBinDir() + "/zerostate"));
        FileUtils.copyDirectory(
            new File(settings.getGenesisNode().getTonDbStaticDir()),
            new File(node.getTonDbStaticDir()));
        //              copy ignoring locked files
        MyLocalTonUtils.copyDirectory(
            settings.getGenesisNode().getTonDbArchiveDir(),
            node.getTonDbArchiveDir()); // if only this dir, then fails with "Check `ptr &&
        // "deferencing null Ref"` failed"
        MyLocalTonUtils.copyDirectory(
            settings.getGenesisNode().getTonDbCellDbDir(),
            node.getTonDbCellDbDir()); // with archive and celldb only - fails with -
        // [!shardclient][&masterchain_block_handle_->inited_next_left()]
        MyLocalTonUtils.copyDirectory(
            settings.getGenesisNode().getTonDbFilesDir(), node.getTonDbFilesDir());
        MyLocalTonUtils.copyDirectory(
            settings.getGenesisNode().getTonDbCatchainsDir(),
            node
                .getTonDbCatchainsDir()); // [!shardclient][&masterchain_block_handle_->inited_next_left()]
      } else {
        // speed up synchronization - copy archive, catchains, files, state and celldb directories
        FileUtils.copyDirectory(
            new File(settings.getGenesisNode().getTonBinDir() + "/zerostate"),
            new File(node.getTonBinDir() + "/zerostate"));
        FileUtils.copyDirectory(
            new File(settings.getGenesisNode().getTonDbStaticDir()),
            new File(node.getTonDbStaticDir()));
        FileUtils.copyDirectory(
            new File(settings.getGenesisNode().getTonDbArchiveDir()),
            new File(node.getTonDbArchiveDir()));
        FileUtils.copyDirectory(
            new File(settings.getGenesisNode().getTonDbCatchainsDir()),
            new File(node.getTonDbCatchainsDir()));
        FileUtils.copyDirectory(
            new File(settings.getGenesisNode().getTonDbCellDbDir()),
            new File(node.getTonDbCellDbDir()));
        FileUtils.copyDirectory(
            new File(settings.getGenesisNode().getTonDbFilesDir()),
            new File(node.getTonDbFilesDir()));
      }
    }

    if (enableLiteServer) {
      validatorEngine.enableLiteServer(node, node.getNodeGlobalConfigLocation(), false);
    }

    if (start) {
      validatorEngine.startValidator(node, node.getNodeGlobalConfigLocation());
    }
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
}
