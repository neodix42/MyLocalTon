package org.ton.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.ton.actions.MyLocalTon;
import org.ton.db.DbPool;
import org.ton.executors.dhtserver.DhtServer;
import org.ton.executors.validatorengine.ValidatorEngine;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.ui.controllers.MainController;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.ui.custom.events.CustomEventBus.emit;

@Slf4j
public class App extends Application {

    public static Scene scene;
    public static StackPane root;
    public static FXMLLoader fxmlLoader;
    public static MainController mainController;
    public static DbPool dbPool;
    public static boolean firstAppLaunch = true;
    public static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        log.info("Starting application");

        fxmlLoader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/main/main.fxml"));
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        scene = new Scene(root);
        mainController = fxmlLoader.getController();
        mainController.setHostServices(getHostServices());

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);

        primaryStage.setOnShown(windowEvent -> {
            log.debug("onShown, stage loaded");

            if (MyLocalTon.getInstance().getSettings().getActiveNodes().size() == 0) {
                mainController.showLoadingPane("Initializing TON blockchain very first time.", "It can take up to 2 minutes, please wait.");
            } else if (MyLocalTon.getInstance().getSettings().getActiveNodes().size() == 1) {
                mainController.showLoadingPane("Starting TON blockchain...", "Should take no longer than 45 seconds.");
            } else {
                mainController.showLoadingPane("Starting TON blockchain... Starting " + MyLocalTon.getInstance().getSettings().getActiveNodes().size(), "validators may take up to 3 minutes.");
            }
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

    public static void setRoot(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getClassLoader().getResource(fxml + ".fxml"));
        scene.setRoot(fxmlLoader.load());
    }

    public static void main(String[] args) throws Throwable {

        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, Objects.requireNonNull(App.class.getClassLoader().getResourceAsStream("org/ton/fonts/RobotoMono-Medium.ttf"))));

        MyLocalTon myLocalTon = MyLocalTon.getInstance();
        myLocalTon.setSettings(Utils.loadSettings());
        myLocalTon.saveSettingsToGson(); //create default config
        MyLocalTonSettings settings = myLocalTon.getSettings();
        log.info("myLocalTon config file location: {}", MyLocalTonSettings.SETTINGS_FILE);

        Utils.setMyLocalTonLogLevel(settings.getGenesisNode().getMyLocalTonLogLevel());

        // override MyLocalTon log level
        if (!Arrays.asList(args).isEmpty()) {
            if (args[0].equalsIgnoreCase("debug")) {
                Utils.setMyLocalTonLogLevel("DEBUG");
                settings.getGenesisNode().setTonLogLevel("DEBUG");
            }
        }

        System.setProperty("objectdb.home", MyLocalTonSettings.DB_DIR);
        System.setProperty("objectdb.conf", MyLocalTonSettings.DB_SETTINGS_FILE);

        // start GUI
        Executors.newSingleThreadExecutor().execute(Application::launch);

        Node genesisNode = settings.getGenesisNode();
        genesisNode.extractBinaries();

        // initialize DB
        dbPool = new DbPool(settings);

        DhtServer dhtServer = new DhtServer();

        List<String> dhtNodes = dhtServer.initDhtServer(genesisNode);

        myLocalTon.initGenesis(genesisNode);

        dhtServer.addDhtNodesToGlobalConfig(dhtNodes, genesisNode.getNodeGlobalConfigLocation());

        dhtServer.startDhtServer(genesisNode, genesisNode.getNodeGlobalConfigLocation());

//        start 2nd DHT server
//        List<String> dhtNodes2 = dhtServer.initDhtServer(settings.getNode2());
//        dhtServer.addDhtNodesToGlobalConfig(dhtNodes2, genesisNode.getNodeGlobalConfigLocation());
//        dhtServer.startDhtServer(settings.getNode2(), genesisNode.getNodeGlobalConfigLocation());

        //create hardfork
        //ResultLastBlock newBlock = myLocalTon.generateNewBlock(genesisNode, forkFromBlock, "");
        //myLocalTon.addHardForkEntryIntoMyGlobalConfig(genesisNode, genesisNode.getNodeGlobalConfigLocation(), newBlock);

        new ValidatorEngine().startValidator(genesisNode, genesisNode.getNodeGlobalConfigLocation());

        // start other validators
        for (String nodeName : settings.getActiveNodes()) {
            if (!nodeName.contains("genesis")) {
                long pid = new ValidatorEngine().startValidator(settings.getNodeByName(nodeName), genesisNode.getNodeGlobalConfigLocation()).pid();
                log.info("started validator {} with pid {}", nodeName, pid);
            }
        }

        myLocalTon.runNodesStatusMonitor();

        Utils.waitForBlockchainReady(genesisNode);
        Utils.waitForNodeSynchronized(genesisNode);

        //------------
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        File dbDir = new File(settings.getGenesisNode().getTonDbDir(), "celldb");
        RocksDB db;
        try {
            Files.createDirectories(dbDir.getParentFile().toPath());
            Files.createDirectories(dbDir.getAbsoluteFile().toPath());
            db = RocksDB.open(options, dbDir.getAbsolutePath());
        } catch (IOException | RocksDBException ex) {
            log.error("Error initializing RocksDB, check configurations and permissions, exception: {}, message: {}, stackTrace: {}",
                    ex.getCause(), ex.getMessage(), ex.getStackTrace());
        }
        log.info("RocksDB initialized and ready to use");

        myLocalTon.runBlockchainMonitor(genesisNode);

        myLocalTon.runBlockchainSizeMonitor();

        while (isNull(mainController)) {
            log.info("Waiting for UI to start...");
            Thread.sleep(1000);
        }

        mainController.showSuccessMsg("TON blockchain is ready!", 2);
        Platform.runLater(() -> emit(new CustomActionEvent(CustomEvent.Type.BLOCKCHAIN_READY)));
        //mainController.removeLoadingPane();
        try {
            Platform.runLater(() -> mainController.removeLoadingPane());

            Thread.sleep(2100);

            myLocalTon.createPreInstalledWallets(genesisNode);
        } catch (Exception e) {
            e.printStackTrace();
        }

        myLocalTon.runBlockchainExplorer();

        Thread.sleep(1000);
        mainController.showSuccessMsg("Wallets are ready. You are all set!", 5);
        Platform.runLater(() -> emit(new CustomActionEvent(CustomEvent.Type.WALLETS_READY)));

        myLocalTon.runAccountsMonitor();

        myLocalTon.runValidationMonitor();

        mainController.addValidatorBtn.setDisable(false);
    }
}