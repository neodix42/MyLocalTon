package org.ton.main;

import static java.util.Objects.nonNull;
import static org.ton.ui.custom.events.CustomEventBus.emit;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.ton.actions.MyLocalTon;
import org.ton.db.DbPool;
import org.ton.executors.dhtserver.DhtServer;
import org.ton.executors.validatorengine.ValidatorEngine;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.ui.controllers.MainController;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.utils.MyLocalTonUtils;

@Slf4j
public class App extends Application {

  public static Scene scene;
  public static StackPane root;
  public static FXMLLoader fxmlLoader;
  public static MainController mainController;
  public static DbPool dbPool;

  public static Boolean testBinaries = false;
  public static AtomicInteger testBinariesCounter = new AtomicInteger(0);

  public static Stage primaryStage;
  private static MyLocalTonSettings settings;
  private static MyLocalTon myLocalTon;
  private static String[] appArgs;

  public static void main(MyLocalTonSettings s, MyLocalTon mlt, String[] args) throws ExecutionException, InterruptedException, TimeoutException {
    settings = s;
    myLocalTon = mlt;
    appArgs = args;
    if (!GraphicsEnvironment.isHeadless()) {
      launch(args);
    } else {
      // Headless mode - without UI, instant background initialization
      doBackgroundInitialization().get(365, TimeUnit.DAYS);
      log.info("exiting 1");
    }
  }

  private static CompletableFuture<Void> doBackgroundInitialization() {
    return CompletableFuture.runAsync(
        () -> {
          Node genesisNode = settings.getGenesisNode();
          try {
            genesisNode.extractBinaries();
          } catch (IOException e) {
            log.error(e.getMessage(), e);
          }

          if (Arrays.asList(appArgs).contains("test-binaries")) {
            log.info("Running in TEST MODE");
            testBinaries = true;
          }

          dbPool = new DbPool(settings);

          DhtServer dhtServer = new DhtServer();
          List<String> dhtNodes = null;
          try {
            dhtNodes = dhtServer.initDhtServer(genesisNode);
            myLocalTon.initGenesis(genesisNode);
            dhtServer.addDhtNodesToGlobalConfig(
                dhtNodes, genesisNode.getNodeGlobalConfigLocation());
            dhtServer.startDhtServer(genesisNode, genesisNode.getNodeGlobalConfigLocation());
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }

          new ValidatorEngine()
              .startValidator(genesisNode, genesisNode.getNodeGlobalConfigLocation());

          // start other validators
          for (String nodeName : settings.getActiveNodes()) {
            if (!nodeName.contains("genesis")) {
              new ValidatorEngine()
                  .startValidator(
                      settings.getNodeByName(nodeName), genesisNode.getNodeGlobalConfigLocation());
            }
          }

          myLocalTon.runNodesStatusMonitor();

          try {
            MyLocalTonUtils.waitForBlockchainReady(genesisNode);
            MyLocalTonUtils.waitForNodeSynchronized(genesisNode);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }

          myLocalTon.runBlockchainMonitor(genesisNode);

          myLocalTon.initTonlib(genesisNode);

          myLocalTon.runBlockchainSizeMonitor();

          try {
            myLocalTon.createInitialWallets(genesisNode);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
          myLocalTon.createFaucetWallet();

          myLocalTon.runBlockchainExplorer();
          myLocalTon.runTonHttpApi();

          if (!Arrays.asList(appArgs).isEmpty()) {
            for (String arg : appArgs) {
              if (arg.startsWith("with-validators-")) {
                int i = Integer.parseInt(arg.replace("with-validators-", ""));
                if (i > 0 && i <= 6) {
                  log.info("adding {} validators", i);
                  for (int j = 2; j <= i + 1; j++) {
                    Node node = settings.getNodeByName("node" + j);
                    if (!settings.getActiveNodes().contains(node.getNodeName())) {
                      FileUtils.deleteQuietly(
                          new File(
                              MyLocalTonSettings.MY_APP_DIR + File.separator + node.getNodeName()));
                      try {
                        MyLocalTon.getInstance().createFullnode(node, true, true);
                      } catch (Exception e) {
                        log.error(e.getMessage(), e);
                      }
                      settings.getActiveNodes().add(node.getNodeName());
                      MyLocalTon.getInstance()
                          .createWalletEntity(
                              node,
                              null,
                              WalletVersion.V3R2,
                              -1L,
                              settings.getWalletSettings().getDefaultSubWalletId(),
                              node.getInitialValidatorWalletAmount(),
                              true);
                    }
                  }
                } else {
                  log.error("Wrong number of validators. Allowed values 1..6.");
                }
              }
            }
          }

          myLocalTon.runAccountsMonitor();
          myLocalTon.runValidationMonitor();
        });
  }

  @Override
  public void start(Stage stage) {
    primaryStage = stage;
    log.debug("Starting UI");

    fxmlLoader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/main/main.fxml"));
    try {
      root = fxmlLoader.load();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
    scene = new Scene(root);
    mainController = fxmlLoader.getController();
    mainController.setHostServices(getHostServices());

    try {
      Image icon =
          new Image(
              Objects.requireNonNull(
                  getClass().getClassLoader().getResourceAsStream("org/ton/images/logo.png")));
      primaryStage.getIcons().add(icon);
    } catch (NullPointerException e) {
      log.error("Icon not found. Exception thrown {}", e.getMessage(), e);
    }

    primaryStage.setScene(scene);
    primaryStage.setResizable(false);
    primaryStage.initStyle(StageStyle.UNDECORATED);
    primaryStage
        .getScene()
        .getWindow()
        .addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);

    primaryStage.setOnShown(
        windowEvent -> {
          if (MyLocalTon.getInstance().getSettings().getActiveNodes().isEmpty()) {
            mainController.showLoadingPane(
                "Initializing TON blockchain very first time", "It can take up to 2 minutes");
          } else if (MyLocalTon.getInstance().getSettings().getActiveNodes().size() == 1) {
            mainController.showLoadingPane(
                "Starting TON blockchain", "Should take no longer than 45 seconds");
          } else {
            mainController.showLoadingPane(
                "Starting TON blockchain",
                "Launching "
                    + MyLocalTon.getInstance().getSettings().getActiveNodes().size()
                    + " validators, wait up to 3 minutes");
          }

          // Running all the heavy initialization in the background thread
          doBackgroundInitialization()
              .thenRun(
                  () -> {
                    // Upon completion, we update the UI
                    Platform.runLater(
                        () -> {
                          mainController.removeLoadingPane();
                          mainController.showSuccessMsg("TON blockchain is ready!", 2);
                          emit(new CustomActionEvent(CustomEvent.Type.BLOCKCHAIN_READY));
                          mainController.showSuccessMsg("Wallets are ready. You are all set!", 5);
                        });
                  });
        });

    primaryStage.show();
  }

  private void closeWindowEvent(WindowEvent event) {
    log.debug("close event consumed");
    event.consume();
    if (nonNull(MyLocalTon.getInstance().getMonitorExecutorService())) {
      MyLocalTon.getInstance().getMonitorExecutorService().shutdownNow();
    }

    mainController.saveSettings();
    mainController.showShutdownMsg("Shutting down TON blockchain...", 5);
  }
}
