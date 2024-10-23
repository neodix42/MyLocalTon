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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.actions.MyLocalTon;
import org.ton.db.DbPool;
import org.ton.executors.dhtserver.DhtServer;
import org.ton.executors.validatorengine.ValidatorEngine;
import org.ton.executors.validatorengine.ValidatorEngineExecutor;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.ui.controllers.MainController;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.utils.MyLocalTonUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    //    public static Tonlib tonlib;
    public static boolean firstAppLaunch = true;
    public static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        log.debug("Starting application");

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

            if (MyLocalTon.getInstance().getSettings().getActiveNodes().isEmpty()) {
                mainController.showLoadingPane("Initializing TON blockchain very first time", "It can take up to 2 minutes");
            } else if (MyLocalTon.getInstance().getSettings().getActiveNodes().size() == 1) {
                mainController.showLoadingPane("Starting TON blockchain", "Should take no longer than 45 seconds");
            } else {
                mainController.showLoadingPane("Starting TON blockchain", "Launching " + MyLocalTon.getInstance().getSettings().getActiveNodes().size() + " validators, wait up to 3 minutes");
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

    public static void main(MyLocalTonSettings settings, MyLocalTon myLocalTon, String[] args) throws Throwable {

        // start GUI
        if (!GraphicsEnvironment.isHeadless()) {
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, Objects.requireNonNull(App.class.getClassLoader().getResourceAsStream("org/ton/fonts/RobotoMono-Medium.ttf"))));
            Executors.newSingleThreadExecutor().execute(Application::launch);
        }

        Node genesisNode = settings.getGenesisNode();
        genesisNode.extractBinaries();
        if (nonNull(settings.getCustomTonBinariesPath())) {
            genesisNode.setTonCustomBinDir(settings.getCustomTonBinariesPath());
        }

        if (!Arrays.asList(args).isEmpty()) {
            for (String arg : args) {

                if (arg.equalsIgnoreCase("test-binaries")) {
                    Pair<Process, Future<String>> validator = new ValidatorEngineExecutor().execute(genesisNode, "-V");
                    validator.getLeft().waitFor(5, TimeUnit.SECONDS);
                    if (validator.getLeft().exitValue() != 0) {
                        System.out.println("Simple binary test failed.");
                        System.exit(10);
                    } else {
                        System.out.println("Simple binary test passed.");
                        System.exit(0);
                    }
                }
            }
        }

        // initialize DB
        dbPool = new DbPool(settings);

        DhtServer dhtServer = new DhtServer();

        List<String> dhtNodes = dhtServer.initDhtServer(genesisNode);

        myLocalTon.initGenesis(genesisNode);

        if (!Arrays.asList(args).isEmpty()) {
            for (String arg : args) {
                if (arg.equalsIgnoreCase("test-tonlib")) {
                    myLocalTon.testInitTonlib(genesisNode);
                }
            }
        }

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

        MyLocalTonUtils.waitForBlockchainReady(genesisNode);
        MyLocalTonUtils.waitForNodeSynchronized(genesisNode);

        myLocalTon.runBlockchainMonitor(genesisNode);

        myLocalTon.initTonlib(genesisNode);

        myLocalTon.runBlockchainSizeMonitor();
        if (!GraphicsEnvironment.isHeadless()) {
            while (isNull(mainController) && !GraphicsEnvironment.isHeadless()) {
                log.info("Waiting for UI to start...");
                Thread.sleep(1000);
            }
        }

        if (!GraphicsEnvironment.isHeadless()) {
            mainController.showSuccessMsg("TON blockchain is ready!", 2);
            Platform.runLater(() -> emit(new CustomActionEvent(CustomEvent.Type.BLOCKCHAIN_READY)));

            Platform.runLater(() -> emit(new CustomActionEvent(CustomEvent.Type.BLOCKCHAIN_READY)));
            //mainController.removeLoadingPane();
            try {
                Platform.runLater(() -> mainController.removeLoadingPane());

                Thread.sleep(2100);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("TON blockchain is ready!");
        }

        myLocalTon.createInitialWallets(genesisNode);

        myLocalTon.createFaucetWallet();

        myLocalTon.runBlockchainExplorer();
        myLocalTon.runTonHttpApi();

        Thread.sleep(1000);
        if (!GraphicsEnvironment.isHeadless()) {
            mainController.showSuccessMsg("Wallets are ready. You are all set!", 5);
            Platform.runLater(() -> emit(new CustomActionEvent(CustomEvent.Type.WALLETS_READY)));
        } else {
            log.info("Wallets are ready. You are all set!");
        }

        myLocalTon.runAccountsMonitor();

        myLocalTon.runValidationMonitor();

        if (!Arrays.asList(args).isEmpty()) {
            for (String arg : args) {
                if (arg.contains("with-validators-")) {
                    int i = Integer.parseInt(arg.replace("with-validators-", ""));
                    if ((i <= 0) || (i > 6)) {
                        log.error("Wrong number of validators. Allowed values 1..6. No validators will be added.");
                        break;
                    }
                    log.info("adding {} validators", i);

                    for (int j = 2; j <= i + 1; j++) {
                        Node node = settings.getNodeByName("node" + j);
                        if (settings.getActiveNodes().contains(node.getNodeName())) {
                            log.info(node.getNodeName() + " already added");
                        } else {

                            //creating additional validator node
                            log.info("creating validator {}", node.getNodeName());
                            node.setTonLogLevel(settings.getGenesisNode().getTonLogLevel());
                            node.setTonCustomBinDir(settings.getGenesisNode().getTonCustomBinDir());

                            //delete unfinished or failed node creation
                            FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + node.getNodeName()));

                            MyLocalTon.getInstance().createFullnode(node, true, true);

//                            DhtServer newDhtServer = new DhtServer();
//                            List<String> newDhtNodes = newDhtServer.initDhtServer(node);
//                            newDhtServer.addDhtNodesToGlobalConfig(newDhtNodes, genesisNode.getNodeGlobalConfigLocation());
//                            newDhtServer.startDhtServer(node, node.getNodeLocalConfigLocation());

                            settings.getActiveNodes().add(node.getNodeName());

//                            for (int x = 2; x <= 7; x++) {
//                                Node nodeTemp = settings.getNodeByName("node" + x);
//                                if (settings.getActiveNodes().contains(nodeTemp.getNodeName())) {
//                                    newDhtServer.copyDhtServersFromGenesisGlobalConfig(nodeTemp.getNodeLocalConfigLocation());
//                                    newDhtServer.copyDhtServersFromGenesisGlobalConfig(nodeTemp.getNodeGlobalConfigLocation());
//                                }
//                            }

                            MyLocalTon.getInstance().saveSettingsToGson();

                            log.info("Creating new validator controlling smart-contract (wallet) for node {}", node.getNodeName());
                            MyLocalTon.getInstance().createWalletEntity(node, null, WalletVersion.V3R2, -1L, settings.getWalletSettings().getDefaultSubWalletId(), node.getInitialValidatorWalletAmount(), true);
                        }
                    }
                }
            }
        }

        if (!GraphicsEnvironment.isHeadless()) {
            mainController.addValidatorBtn.setDisable(false);
        }
    }
}