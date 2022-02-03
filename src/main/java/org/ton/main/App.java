package org.ton.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.ton.actions.MyLocalTon;
import org.ton.db.DbPool;
import org.ton.executors.validatorengine.ValidatorEngine;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.ui.controllers.MainController;
import org.ton.utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executors;

import static java.util.Objects.nonNull;

@Slf4j
public class App extends Application {

    public static Scene scene;
    public static StackPane root;
    public static FXMLLoader fxmlLoader;
    public static MainController mainController;
    public static DbPool dbPool;

    @Override
    public void start(Stage primaryStage) throws IOException {
        log.info("Starting application");
        fxmlLoader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/main/main.fxml"));
        root = fxmlLoader.load();
        scene = new Scene(root);
        mainController = fxmlLoader.getController();

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);
        primaryStage.setOnShown(windowEvent -> log.debug("onShown, stage loaded"));
        primaryStage.show();
    }

    private void closeWindowEvent(WindowEvent event) {
        log.debug("close event consumed");
        event.consume();
        if (nonNull(MyLocalTon.getInstance().getMonitorExecutorService())) {
            MyLocalTon.getInstance().getMonitorExecutorService().shutdownNow();
        }
        mainController.shutdown(); // saving settings
        mainController.showShutdownMsg("Shutting down TON blockchain...", 5 * 60L);
    }

    public static void setRoot(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getClassLoader().getResource(fxml + ".fxml"));
        scene.setRoot(fxmlLoader.load());
    }

    public static void main(String[] args) throws Throwable {

        if (!Arrays.asList(args).isEmpty()) {
            log.info("R E S E T T I N G: {}", Arrays.asList(args));
            Thread.sleep(1000);
            FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "MyLocalTonDB"));
            FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "genesis"));
            FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "templates"));
            FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "myLocalTon.log"));
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, Objects.requireNonNull(App.class.getClassLoader().getResourceAsStream("org/ton/fonts/RobotoMono-Medium.ttf"))));

        MyLocalTon myLocalTon = MyLocalTon.getInstance();
        myLocalTon.setSettings(new MyLocalTonSettings().loadSettings());
        myLocalTon.saveSettingsToGson(); //create default config

        log.info("myLocalTon config file location: {}", MyLocalTonSettings.SETTINGS_FILE);
        Utils.setMyLocalTonLogLevel(myLocalTon.getSettings().getLogSettings().getMyLocalTonLogLevel());

        System.setProperty("objectdb.home", MyLocalTonSettings.DB_DIR);
        System.setProperty("objectdb.conf", MyLocalTonSettings.DB_SETTINGS_FILE);

        // start GUI
        Executors.newSingleThreadExecutor().execute(Application::launch);

        Node genesisNode = myLocalTon.getSettings().getGenesisNode();
        genesisNode.extractBinaries();

        // initialize DB
        dbPool = new DbPool(myLocalTon.getSettings());

        Process validatorGenesisProcess = myLocalTon.initGenesis(genesisNode);

        Thread.sleep(4000);
        if (nonNull(validatorGenesisProcess)) {
            validatorGenesisProcess.destroy();
        } else {
            mainController.showWarningMsg("Starting TON blockchain... Should take no longer than 45 seconds.", 5 * 60L);
        }

        //create hardfork
        //ResultLastBlock newBlock = myLocalTon.generateNewBlock(genesisNode, forkFromBlock, "");
        //myLocalTon.addHardForkEntryIntoMyGlobalConfig(genesisNode, genesisNode.getNodeGlobalConfigLocation(), newBlock);

        ValidatorEngine validatorEngine = new ValidatorEngine();
        myLocalTon.setGenesisValidatorProcess(validatorEngine.startValidator(genesisNode, genesisNode.getNodeGlobalConfigLocation()));

        Utils.waitForBlockchainReady(genesisNode);

        myLocalTon.runBlockchainMonitor(genesisNode);

        myLocalTon.runBlockchainSizeMonitor();
        mainController.showSuccessMsg("TON blockchain is ready!", 2);
        Thread.sleep(3000);

        myLocalTon.createPreInstalledWallets(genesisNode);


        myLocalTon.runBlockchainExplorer();

        Thread.sleep(1000);
        mainController.showSuccessMsg("Wallets are ready. You are all set!", 5);

        myLocalTon.runAccountsMonitor();

        //myLocalTon.runValidationMonitor(genesisNode);
    }
}