package org.ton.mylocalton.main;

import static java.util.Objects.nonNull;
import static org.ton.mylocalton.ui.custom.events.CustomEventBus.emit;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
import org.ton.java.tonlib.types.MasterChainInfo;
import org.ton.java.utils.Utils;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.db.DbPool;
import org.ton.mylocalton.executors.dhtserver.DhtServer;
import org.ton.mylocalton.executors.validatorengine.ValidatorEngine;
import org.ton.mylocalton.settings.MyLocalTonSettings;
import org.ton.mylocalton.settings.Node;
import org.ton.mylocalton.ui.controllers.MainController;
import org.ton.mylocalton.ui.custom.events.CustomEvent;
import org.ton.mylocalton.ui.custom.events.event.CustomActionEvent;
import org.ton.mylocalton.utils.MyLocalTonUtils;

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

  public static void main(MyLocalTonSettings s, MyLocalTon mlt, String[] args) {
    settings = s;
    myLocalTon = mlt;
    appArgs = args;
    if (!GraphicsEnvironment.isHeadless()) {
      launch(args);
    } else {
      // Headless mode - without UI, instant background initialization
      doBackgroundInitialization();
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
          List<String> dhtNodes;
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

          myLocalTon.initTonlib(genesisNode);
          long syncDelay = 100;
          while (syncDelay > 10) {
            try {
              MasterChainInfo masterChainInfo = MyLocalTon.tonlib.getLast();
              log.info("masterChainInfo {}", masterChainInfo);
              if (nonNull(masterChainInfo) && (masterChainInfo.getLast().getSeqno() > 0)) {
                log.info("masterChainInfo {}", masterChainInfo.getLast().getShortBlockSeqno());
                syncDelay = MyLocalTonUtils.getSyncDelay();
                log.info("out of sync seconds {}", syncDelay);
              }
              Utils.sleep(1);
            } catch (Throwable e) {
              log.error("Error in launching TON blockchain: {}", e.getMessage());
              if (MyLocalTonUtils.doShutdown()) {
                log.info("system exit 44");
                System.exit(44);
              }
            }
          }

          myLocalTon.runNodesStatusMonitor();

          //          try {
          //
          //            MyLocalTonUtils.waitForBlockchainReady(genesisNode);
          //            MyLocalTonUtils.waitForNodeSynchronized(genesisNode);
          //          } catch (Exception e) {
          //            log.error(e.getMessage(), e);
          //          }

          myLocalTon.runBlockchainMonitor(genesisNode);

          //          myLocalTon.initTonlib(genesisNode);

          myLocalTon.runBlockchainSizeMonitor();

          try {
            myLocalTon.createInitialWallets(genesisNode);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }

          myLocalTon.runBlockchainExplorer();
          myLocalTon.runTonHttpApi();
          myLocalTon.runDataGenerator();

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
                      MyLocalTon.getInstance().createValidatorControllingSmartContract(node);
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
  public void start(Stage stage) throws IOException {
    primaryStage = stage;
    log.debug("Starting UI");

    fxmlLoader =
        new FXMLLoader(App.class.getClassLoader().getResource("org/ton/mylocalton/main/main.fxml"));
    root = fxmlLoader.load();

    scene = new Scene(root);
    mainController = fxmlLoader.getController();
    mainController.setHostServices(getHostServices());

    Image icon =
        new Image(
            Objects.requireNonNull(
                getClass()
                    .getClassLoader()
                    .getResourceAsStream("org/ton/mylocalton/images/logo.png")));
    primaryStage.getIcons().add(icon);

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
