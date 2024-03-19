package org.ton.ui.controllers;

import com.jfoenix.controls.*;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.ton.actions.MyLocalTon;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.WalletEntity;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.blockchainexplorer.BlockchainExplorer;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.*;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.executors.liteclient.api.config.Validator;
import org.ton.executors.tonhttpapi.TonHttpApi;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.utils.Utils;
import org.ton.main.App;
import org.ton.parameters.ValidationParam;
import org.ton.settings.*;
import org.ton.ui.custom.control.CustomComboBox;
import org.ton.ui.custom.control.CustomInfoLabel;
import org.ton.ui.custom.control.CustomTextField;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.ui.custom.events.event.CustomNotificationEvent;
import org.ton.ui.custom.events.event.CustomSearchEvent;
import org.ton.ui.custom.layout.*;
import org.ton.utils.MyLocalTonUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.actions.MyLocalTon.MAX_ROWS_IN_GUI;
import static org.ton.main.App.mainController;
import static org.ton.ui.custom.events.CustomEventBus.emit;
import static org.ton.ui.custom.events.CustomEventBus.listenFor;

@Slf4j
public class MainController implements Initializable {

    public static final String LIGHT_BLUE = "#dbedff";

    public static final long ONE_BLN = 1000000000L;
    public static final String TELEGRAM_NEODIX = "https://telegram.me/neodix";
    @FXML
    public StackPane superWindow;

    @FXML
    public CustomMainLayout mainLayout;

    @FXML
    public CustomInfoLabel currentBlockNum;

    @FXML
    public CustomInfoLabel liteClientInfo;

    @FXML
    public CustomInfoLabel shardsNum;

    @FXML
    public CustomInfoLabel dbSizeId;

    @FXML
    public JFXListView<Node> blockslistviewid;

    @FXML
    public JFXListView<Node> transactionsvboxid;

    @FXML
    public JFXListView<Node> accountsvboxid;

    @FXML
    public TextField nodeStateTtl1;

    @FXML
    public TextField nodeBlockTtl1;

    @FXML
    public TextField nodeArchiveTtl1;

    @FXML
    public TextField nodeKeyProofTtl1;

    @FXML
    public TextField nodeSyncBefore1;

    @FXML
    public JFXButton myLocalTonDbDirBtn;

    @FXML
    public Tab logsTab;

    @FXML
    public Tab validationTab;

    @FXML
    public JFXTabPane validationTabs;

    @FXML
    public Label nodeStatus2;

    @FXML
    public Label nodeStatus3;

    @FXML
    public Label nodePublicPort1;

    @FXML
    public Label nodeConsolePort1;

    @FXML
    public Label liteServerPort1;

    @FXML
    public Label nodeStatus1;

    @FXML
    public Label totalParticipants;

    @FXML
    public Label totalValidators;

    @FXML
    public JFXCheckBox enableBlockchainExplorer;

    @FXML
    public JFXCheckBox enableTonHttpApi;

    @FXML
    public Label enableTonHttpApiLabel;

    @FXML
    public Label enableBlockchainExplorerLabel;

    @FXML
    public Tab explorerTab;

    @FXML
    public WebView webView;

    @FXML
    public WebView webViewTonHttpApi;

    @FXML
    public Label validator1WalletBalance;

    @FXML
    public Label validator1WalletAddress;

    @FXML
    public Label validator1AdnlAddress;

    @FXML
    public Label blockchainLaunched;

    @FXML
    public Label startCycle;

    @FXML
    public Label endCycle;

    @FXML
    public Label startElections;

    @FXML
    public Label endElections;

    @FXML
    public Label nextElections;

    @FXML
    public Label minterAddr;

    @FXML
    public Label configAddr;

    @FXML
    public Label electorAddr;

    @FXML
    public Label validationPeriod;

    @FXML
    public Label electionPeriod;

    @FXML
    public Label holdPeriod;

    @FXML
    public Label minimumStake;

    @FXML
    public Label maximumStake;

    @FXML
    public Label validator1PubKeyHex;

    @FXML
    public ProgressBar validationCountDown;

    @FXML
    public Label minterBalance;

    @FXML
    public Label configBalance;

    @FXML
    public Label electorBalance;

    @FXML
    public Label legendHoldStake;

    @FXML
    public Label legendValidation;

    @FXML
    public Label legendElections;

    @FXML
    public Label legendPause;

    @FXML
    public Label stakeHoldRange3;

    @FXML
    public Label validationRange3;

    @FXML
    public Label pauseRange3;

    @FXML
    public Label electionsRange3;

    @FXML
    public Label stakeHoldRange2;

    @FXML
    public Label validationRange2;

    @FXML
    public Label pauseRange2;

    @FXML
    public Label electionsRange2;

    @FXML
    public Label stakeHoldRange1;

    @FXML
    public Label validationRange1;

    @FXML
    public Label pauseRange1;

    @FXML
    public Label electionsRange1;

    @FXML
    public Pane electionsChartPane;

    @FXML
    public Separator timeLine;

    @FXML
    public Label validator1PubKeyInteger;

    @FXML
    public ProgressBar progressValidationUpdate;

    @FXML
    public Label validator1AdnlAddressNext;

    @FXML
    public Label validator1PubKeyHexNext;

    @FXML
    public Label validator1PubKeyIntegerNext;

    @FXML
    public Label validator1totalCollected;

    @FXML
    public Label validator1LastCollected;

    @FXML
    public Label validator1TotalRewardsPure;

    @FXML
    public Label validator1LastRewardPure;

    @FXML
    public Label validator1AvgPureReward;

    @FXML
    public Label participatedInElections1;

    @FXML
    public JFXButton addValidatorBtn;

    @FXML
    public Tab genesisnode1;

    @FXML
    public Tab validator2tab;

    @FXML
    public Tab validator3tab;

    @FXML
    public Tab validator4tab;

    @FXML
    public Tab validator5tab;

    @FXML
    public Tab validator6tab;

    @FXML
    public Tab validator7tab;

    @FXML
    public Label nodePublicPort2;

    @FXML
    public Label nodeConsolePort2;

    @FXML
    public Label liteServerPort2;

    @FXML
    public Label validator2AdnlAddress;

    @FXML
    public Label validator2PubKeyHex;

    @FXML
    public Label validator2PubKeyInteger;

    @FXML
    public Label validator2AdnlAddressNext;

    @FXML
    public Label validator2PubKeyIntegerNext;

    @FXML
    public Label validator2PubKeyHexNext;

    @FXML
    public Label validator2WalletAddress;

    @FXML
    public Label validator2WalletBalance;

    @FXML
    public Label validator2totalCollected;

    @FXML
    public Label validator2LastCollected;

    @FXML
    public Label validator2TotalRewardsPure;

    @FXML
    public Label validator2LastRewardPure;

    @FXML
    public Label validator2AvgPureReward;

    @FXML
    public Label participatedInElections2;

    @FXML
    public JFXButton deleteValidatorBtn2;

    @FXML
    public Label nodePublicPort3;

    @FXML
    public Label nodeConsolePort3;

    @FXML
    public Label liteServerPort3;

    @FXML
    public Label validator3AdnlAddress;

    @FXML
    public Label validator3PubKeyHex;

    @FXML
    public Label validator3PubKeyInteger;

    @FXML
    public Label validator3AdnlAddressNext;

    @FXML
    public Label validator3PubKeyHexNext;

    @FXML
    public Label validator3PubKeyIntegerNext;

    @FXML
    public Label validator3WalletAddress;

    @FXML
    public Label validator3WalletBalance;

    @FXML
    public Label validator3totalCollected;

    @FXML
    public Label validator3TotalRewardsPure;

    @FXML
    public Label validator3LastCollected;

    @FXML
    public Label validator3LastRewardPure;

    @FXML
    public Label validator3AvgPureReward;

    @FXML
    public Label participatedInElections3;

    @FXML
    public JFXButton deleteValidatorBtn3;

    @FXML
    public Label nodeStatus4;

    @FXML
    public Label nodePublicPort4;

    @FXML
    public Label nodeConsolePort4;

    @FXML
    public Label liteServerPort4;

    @FXML
    public Label validator4AdnlAddress;

    @FXML
    public Label validator4AdnlAddressNext;

    @FXML
    public Label validator4PubKeyHexNext;

    @FXML
    public Label validator4PubKeyIntegerNext;

    @FXML
    public Label validator4WalletAddress;

    @FXML
    public Label validator4WalletBalance;

    @FXML
    public Label validator4totalCollected;

    @FXML
    public Label validator4LastCollected;

    @FXML
    public Label validator4TotalRewardsPure;

    @FXML
    public Label validator4LastRewardPure;

    @FXML
    public Label validator4AvgPureReward;

    @FXML
    public Label participatedInElections4;

    @FXML
    public JFXButton deleteValidatorBtn4;

    @FXML
    public Label nodeStatus5;

    @FXML
    public Label nodePublicPort5;

    @FXML
    public Label nodeConsolePort5;

    @FXML
    public Label liteServerPort5;

    @FXML
    public Label validator5AdnlAddress;

    @FXML
    public Label validator5PubKeyHex;

    @FXML
    public Label validator5PubKeyInteger;

    @FXML
    public Label validator5AdnlAddressNext;

    @FXML
    public Label validator5PubKeyHexNext;

    @FXML
    public Label validator5PubKeyIntegerNext;

    @FXML
    public Label validator5WalletAddress;

    @FXML
    public Label validator5WalletBalance;

    @FXML
    public Label validator5totalCollected;

    @FXML
    public Label validator5LastCollected;

    @FXML
    public Label validator5TotalRewardsPure;

    @FXML
    public Label validator5LastRewardPure;

    @FXML
    public Label validator5AvgPureReward;

    @FXML
    public Label participatedInElections5;

    @FXML
    public JFXButton deleteValidatorBtn5;

    @FXML
    public Label nodeStatus6;

    @FXML
    public Label nodePublicPort6;

    @FXML
    public Label nodeConsolePort6;

    @FXML
    public Label liteServerPort6;

    @FXML
    public Label validator6AdnlAddress;

    @FXML
    public Label validator6PubKeyHex;

    @FXML
    public Label validator6PubKeyInteger;

    @FXML
    public Label validator6AdnlAddressNext;

    @FXML
    public Label validator6PubKeyHexNext;

    @FXML
    public Label validator6PubKeyIntegerNext;

    @FXML
    public Label validator6WalletAddress;

    @FXML
    public Label validator6WalletBalance;

    @FXML
    public Label validator6totalCollected;

    @FXML
    public Label validator6LastCollected;

    @FXML
    public Label validator6TotalRewardsPure;

    @FXML
    public Label validator6LastRewardPure;

    @FXML
    public Label validator6AvgPureReward;

    @FXML
    public Label participatedInElections6;

    @FXML
    public JFXButton deleteValidatorBtn6;

    @FXML
    public Label nodeStatus7;

    @FXML
    public Label nodePublicPort7;

    @FXML
    public Label nodeConsolePort7;

    @FXML
    public Label liteServerPort7;

    @FXML
    public Label validator7AdnlAddress;

    @FXML
    public Label validator7PubKeyHex;

    @FXML
    public Label validator7PubKeyInteger;

    @FXML
    public Label validator7AdnlAddressNext;

    @FXML
    public Label validator7PubKeyHexNext;

    @FXML
    public Label validator7PubKeyIntegerNext;

    @FXML
    public Label validator7WalletAddress;

    @FXML
    public Label validator7WalletBalance;

    @FXML
    public Label validator7totalCollected;

    @FXML
    public Label validator7LastCollected;

    @FXML
    public Label validator7TotalRewardsPure;

    @FXML
    public Label validator7LastRewardPure;

    @FXML
    public Label validator7AvgPureReward;

    @FXML
    public Label participatedInElections7;

    @FXML
    public JFXButton deleteValidatorBtn7;

    @FXML
    public Label validator4PubKeyHex;

    @FXML
    public Label validator4PubKeyInteger;

    @FXML
    public Tab settingsLogsValidator1Tab;

    @FXML
    public JFXTabPane subLogsTabs;

    @FXML
    public JFXTextField nodeStateTtl2;

    @FXML
    public JFXTextField nodeBlockTtl2;

    @FXML
    public JFXTextField nodeArchiveTtl2;

    @FXML
    public JFXTextField nodeKeyProofTtl2;

    @FXML
    public JFXTextField nodeSyncBefore2;

    @FXML
    public JFXTextField configNodePublicPort2;

    @FXML
    public JFXTextField configNodeConsolePort2;

    @FXML
    public JFXTextField configLiteServerPort2;

    @FXML
    public JFXTextField validatorWalletDeposit2;

    @FXML
    public JFXTextField validatorDefaultStake2;

    @FXML
    public JFXTextField validatorWalletDeposit1;

    @FXML
    public JFXTextField validatorDefaultStake1;

    @FXML
    public JFXTextField nodeSyncBefore3;

    @FXML
    public JFXTextField nodeKeyProofTtl3;

    @FXML
    public JFXTextField nodeArchiveTtl3;

    @FXML
    public JFXTextField nodeBlockTtl3;

    @FXML
    public JFXTextField nodeStateTtl3;

    @FXML
    public JFXTextField validatorDefaultStake3;

    @FXML
    public JFXTextField validatorWalletDeposit3;

    @FXML
    public JFXTextField configLiteServerPort3;

    @FXML
    public JFXTextField configNodePublicPort3;

    @FXML
    public JFXTextField configNodeConsolePort3;

    @FXML
    public JFXTextField configNodeConsolePort4;

    @FXML
    public JFXTextField configNodePublicPort4;

    @FXML
    public JFXTextField configLiteServerPort4;

    @FXML
    public JFXTextField validatorWalletDeposit4;

    @FXML
    public JFXTextField validatorDefaultStake4;

    @FXML
    public JFXTextField nodeStateTtl4;

    @FXML
    public JFXTextField nodeBlockTtl4;

    @FXML
    public JFXTextField nodeArchiveTtl4;

    @FXML
    public JFXTextField nodeKeyProofTtl4;

    @FXML
    public JFXTextField nodeSyncBefore4;

    @FXML
    public JFXTextField configNodeConsolePort5;

    @FXML
    public JFXTextField configNodePublicPort5;

    @FXML
    public JFXTextField configLiteServerPort5;

    @FXML
    public JFXTextField validatorWalletDeposit5;

    @FXML
    public JFXTextField validatorDefaultStake5;

    @FXML
    public JFXTextField nodeStateTtl5;

    @FXML
    public JFXTextField nodeBlockTtl5;

    @FXML
    public JFXTextField nodeArchiveTtl5;

    @FXML
    public JFXTextField nodeKeyProofTtl5;

    @FXML
    public JFXTextField nodeSyncBefore5;

    @FXML
    public JFXTextField configNodeConsolePort6;

    @FXML
    public JFXTextField configNodePublicPort6;

    @FXML
    public JFXTextField configLiteServerPort6;

    @FXML
    public JFXTextField validatorWalletDeposit6;

    @FXML
    public JFXTextField validatorDefaultStake6;

    @FXML
    public JFXTextField nodeStateTtl6;

    @FXML
    public JFXTextField nodeBlockTtl6;

    @FXML
    public JFXTextField nodeArchiveTtl6;

    @FXML
    public JFXTextField nodeKeyProofTtl6;

    @FXML
    public JFXTextField nodeSyncBefore6;

    @FXML
    public JFXTextField configNodeConsolePort7;

    @FXML
    public JFXTextField configNodePublicPort7;

    @FXML
    public JFXTextField configLiteServerPort7;

    @FXML
    public JFXTextField validatorWalletDeposit7;

    @FXML
    public JFXTextField validatorDefaultStake7;

    @FXML
    public JFXTextField nodeStateTtl7;

    @FXML
    public JFXTextField nodeBlockTtl7;

    @FXML
    public JFXTextField nodeArchiveTtl7;

    @FXML
    public JFXTextField nodeKeyProofTtl7;

    @FXML
    public JFXTextField nodeSyncBefore7;

    @FXML
    public Label tonDonationAddress;

    @FXML
    JFXCheckBox shardStateCheckbox;

    @FXML
    JFXCheckBox showMsgBodyCheckBox;

    @FXML
    Label searchTabText;

    @FXML
    public JFXListView<Node> foundBlockslistviewid, foundTxsvboxid, foundAccountsvboxid, foundAccountsTxsvboxid;

    @FXML
    TextField configNodePublicPort1;

    @FXML
    TextField configNodeConsolePort1;

    @FXML
    TextField configLiteServerPort1;

    @FXML
    TextField configDhtServerPort1;

    @FXML
    ImageView aboutLogo;

    @FXML
    private Label scrollBtn;

    @FXML
    private SVGPath scrollPath;

    @FXML
    private CustomTextField validatorLogDir1, dhtLogDir1, myLocalTonLog, validatorLogDir2, validatorLogDir3,
            validatorLogDir4, validatorLogDir5, validatorLogDir6, validatorLogDir7, coinsPerWallet,
            globalId, initialBalance, maxValidators, maxMainValidators, minValidators, electedFor, minStake,
            electionStartBefore, minTotalStake, stakesFrozenFor, maxFactor, gasPrice, gasPriceMc, cellPrice,
            cellPriceMc, electionEndBefore, maxStake;

    @FXML
    public JFXCheckBox tickTockCheckBox;

    @FXML
    public JFXCheckBox mainConfigTxCheckBox;

    @FXML
    public JFXCheckBox inOutMsgsCheckBox;

    @FXML
    public CustomComboBox myLogLevel, tonLogLevel, tonLogLevel2, tonLogLevel3, tonLogLevel4, tonLogLevel5, tonLogLevel6, tonLogLevel7;

    private MyLocalTonSettings settings;

    JFXDialog sendDialog;
    JFXDialog yesNoDialog;
    private JFXDialog loadingDialog;
    private JFXDialog createDialog;
    private HostServices hostServices;

    public void showSendDialog(String srcAddr) {
        FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/send-coin-pane.fxml"));
        Parent parent = null;
        try {
            parent = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        SendCoinPaneController controller = loader.getController();
        controller.setHiddenWalletAddr(srcAddr);

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(parent);

        sendDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
        sendDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        sendDialog.close();
                    }
                }
        );
        sendDialog.setOnDialogOpened(jfxDialogEvent -> controller.requestFocusToDestinationAddress());
        sendDialog.show();
    }


    public void showInfoMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> emit(new CustomNotificationEvent(CustomEvent.Type.INFO, msg, durationSeconds)));
    }

    public void showSuccessMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> emit(new CustomNotificationEvent(CustomEvent.Type.SUCCESS, msg, durationSeconds)));
    }

    public void showErrorMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> emit(new CustomNotificationEvent(CustomEvent.Type.ERROR, msg, durationSeconds)));
    }

    public void showWarningMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> emit(new CustomNotificationEvent(CustomEvent.Type.WARNING, msg, durationSeconds)));
    }

    public void showShutdownMsg(String msg, double durationSeconds) {
        Platform.runLater(() -> {
            emit(new CustomNotificationEvent(CustomEvent.Type.INFO, msg, durationSeconds));

            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.schedule(() -> {
                Thread.currentThread().setName("MyLocalTon - Shutdown");
                saveSettings();
                Platform.exit(); // closes main form

                if (MyLocalTonUtils.doShutdown()) {
                    log.info("system exit 0");
                    System.exit(0);
                }
            }, 3, TimeUnit.SECONDS);
        });
    }

    public void shutdown() {
        saveSettings();
    }

    @FXML
    void myLocalTonFileBtnAction() throws IOException {
        log.info("open mylocalton log {}", myLocalTonLog.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start notepad " + myLocalTonLog.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + myLocalTonLog.getFieldText());
        }
    }

    @FXML
    void dhtLogDirBtnAction1() throws IOException {
        log.debug("open dht dir {}", dhtLogDir1.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start " + dhtLogDir1.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + dhtLogDir1.getFieldText());
        }
    }

    @FXML
    void valLogDirBtnAction1() throws IOException {
        log.debug("open validator log dir {}", validatorLogDir1.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start " + validatorLogDir1.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + validatorLogDir1.getFieldText());
        }
    }

    @FXML
    void blocksOnScroll(ScrollEvent event) {
        try {
            Node n1 = blockslistviewid.lookup(".scroll-bar");

            if (n1 instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) n1;

                if (event.getDeltaY() < 0 && bar.getValue() > 0) { // bottom reached
                    Platform.runLater(() -> {
                        BorderPane bp = (BorderPane) blockslistviewid.getItems().get(blockslistviewid.getItems().size() - 1);
                        long lastSeqno = Long.parseLong(((Label) ((Node) bp).lookup("#seqno")).getText());
                        long wc = Long.parseLong(((Label) ((Node) bp).lookup("#wc")).getText());

                        String createdatDate = ((Label) ((Node) bp).lookup("#createdatDate")).getText();
                        String createdatTime = ((Label) ((Node) bp).lookup("#createdatTime")).getText();

                        long createdAt = MyLocalTonUtils.datetimeToTimestamp(createdatDate + " " + createdatTime);

                        log.debug("bottom reached, seqno {}, time {}, hwm {} ", lastSeqno, MyLocalTonUtils.toUtcNoSpace(createdAt), MyLocalTon.getInstance().getBlocksScrollBarHighWaterMark().get());

                        if (blockslistviewid.getItems().size() > MAX_ROWS_IN_GUI) {
                            showWarningMsg("Maximum amount (" + MyLocalTon.getInstance().getBlocksScrollBarHighWaterMark().get() + ") of visible blocks in GUI reached.", 5);
                            return;
                        }

                        List<BlockEntity> blocks = App.dbPool.loadBlocksBefore(createdAt);
                        MyLocalTon.getInstance().getBlocksScrollBarHighWaterMark().addAndGet(blocks.size());

                        ObservableList<Node> blockRows = FXCollections.observableArrayList();

                        for (BlockEntity block : blocks) {
                            try {
                                FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("blockrow.fxml"));
                                javafx.scene.Node blockRow = fxmlLoader.load();

                                ResultLastBlock resultLastBlock = ResultLastBlock.builder()
                                        .createdAt(block.getCreatedAt())
                                        .seqno(block.getSeqno())
                                        .rootHash(block.getRoothash())
                                        .fileHash(block.getFilehash())
                                        .wc(block.getWc())
                                        .shard(block.getShard())
                                        .build();

                                MyLocalTon.getInstance().populateBlockRowWithData(resultLastBlock, blockRow, null);

                                if (resultLastBlock.getWc() == -1L) {
                                    (blockRow.lookup("#blockRowBorderPane")).getStyleClass().add("row-pane-gray");
                                }

                                log.debug("Adding block {} roothash {}", block.getSeqno(), block.getRoothash());

                                blockRows.add(blockRow);

                            } catch (IOException e) {
                                log.error("Error loading blockrow.fxml file, {}", e.getMessage());
                                return;
                            }
                        }

                        log.debug("blockRows.size  {}", blockRows.size());

                        if ((blockRows.isEmpty()) && (lastSeqno < 10)) {
                            log.debug("On start some blocks were skipped, load them now from 1 to {}", lastSeqno - 1);

                            LongStream.range(1, 10).forEach(i -> {
                                try {
                                    ResultLastBlock block = LiteClientParser.parseBySeqno(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeBySeqno(MyLocalTon.getInstance().getSettings().getGenesisNode(), -1L, "8000000000000000", new BigInteger(String.valueOf(i))));
                                    log.debug("Load missing block {}: {}", i, block.getFullBlockSeqno());
                                    MyLocalTon.getInstance().insertBlocksAndTransactions(MyLocalTon.getInstance().getSettings().getGenesisNode(), block, false);
                                } catch (Exception e) {
                                    log.error("cannot load missing blocks {}", e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        }
                        blockslistviewid.getItems().addAll(blockRows);
                    });
                }
                if (event.getDeltaY() > 0) { // top reached
                    log.debug("top reached");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void txsOnScroll(ScrollEvent event) {

        log.debug("txsOnScroll: {}", event);

        Node n1 = transactionsvboxid.lookup(".scroll-bar");

        if (n1 instanceof ScrollBar) {
            ScrollBar bar = (ScrollBar) n1;

            if (event.getDeltaY() < 0 && bar.getValue() > 0) { // bottom reached

                Platform.runLater(() -> {

                    BorderPane bp = (BorderPane) transactionsvboxid.getItems().get(transactionsvboxid.getItems().size() - 1);
                    String shortseqno = ((Label) ((Node) bp).lookup("#block")).getText();

                    long createdAt = MyLocalTonUtils.datetimeToTimestamp(((Label) ((Node) bp).lookup("#time")).getText());

                    BlockShortSeqno blockShortSeqno = BlockShortSeqno.builder()
                            .wc(Long.valueOf(StringUtils.substringBetween(shortseqno, "(", ",")))
                            .shard(StringUtils.substringBetween(shortseqno, ",", ","))
                            .seqno(new BigInteger(StringUtils.substring(StringUtils.substringAfterLast(shortseqno, ","), 0, -1)))
                            .build();

                    log.debug("bottom reached, seqno {}, hwm {}, createdAt {}, utc {}", blockShortSeqno.getSeqno(), MyLocalTon.getInstance().getTxsScrollBarHighWaterMark().get(), createdAt, MyLocalTonUtils.toUtcNoSpace(createdAt));

                    if (blockShortSeqno.getSeqno().compareTo(BigInteger.ONE) == 0) {
                        return;
                    }

                    if (transactionsvboxid.getItems().size() > MAX_ROWS_IN_GUI) {
                        showWarningMsg("Maximum amount (" + MyLocalTon.getInstance().getTxsScrollBarHighWaterMark().get() + ") of visible TXs in GUI reached.", 5);
                        return;
                    }

                    List<TxEntity> txs = App.dbPool.loadTxsBefore(createdAt);

                    MyLocalTon.getInstance().applyTxGuiFilters(txs);

                    MyLocalTon.getInstance().getTxsScrollBarHighWaterMark().addAndGet(txs.size());

                    ObservableList<Node> txRows = FXCollections.observableArrayList();

                    for (TxEntity txEntity : txs) {
                        try {
                            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("txrow.fxml"));
                            javafx.scene.Node txRow = fxmlLoader.load();

                            String shortBlock = String.format("(%d,%s,%d)", txEntity.getWc(), txEntity.getShard(), txEntity.getSeqno());

                            ResultListBlockTransactions resultListBlockTransactions = ResultListBlockTransactions.builder()
                                    .txSeqno(new BigInteger(txEntity.getSeqno().toString()))
                                    .hash(txEntity.getTxHash())
                                    .accountAddress(txEntity.getTx().getAccountAddr())
                                    .lt(txEntity.getTx().getLt())
                                    .build();

                            Transaction txDetails = Transaction.builder()
                                    .accountAddr(txEntity.getTx().getAccountAddr())
                                    .description(txEntity.getTx().getDescription())
                                    .inMsg(txEntity.getTx().getInMsg())
                                    .endStatus(txEntity.getTx().getEndStatus())
                                    .now(txEntity.getTx().getNow())
                                    .totalFees(txEntity.getTx().getTotalFees())
                                    .lt(new BigInteger(txEntity.getTxLt().toString()))
                                    .build();

                            MyLocalTon.getInstance().populateTxRowWithData(shortBlock, resultListBlockTransactions, txDetails, txRow, txEntity);

                            if (txEntity.getTypeTx().equals("Message")) {
                                txRow.setStyle("-fx-background-color: e9f4ff;");
                            }

                            log.debug("adding tx hash {}, addr {}", txEntity.getTxHash(), txEntity.getTx().getAccountAddr());

                            txRows.add(txRow);

                        } catch (IOException e) {
                            log.error("error loading txrow.fxml file, {}", e.getMessage());
                            return;
                        }
                    }
                    log.debug("txRows.size  {}", txRows.size());

                    if ((txRows.isEmpty()) && (blockShortSeqno.getSeqno().compareTo(BigInteger.TEN) < 0)) {
                        log.debug("on start some blocks were skipped and thus some transactions get lost, load them from blocks 1");

                        LongStream.range(1, 10).forEach(i -> {
                            try {
                                ResultLastBlock block = LiteClientParser.parseBySeqno(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeBySeqno(MyLocalTon.getInstance().getSettings().getGenesisNode(), -1L, "8000000000000000", new BigInteger(String.valueOf(i))));
                                log.debug("load missing block {}: {}", i, block.getFullBlockSeqno());
                                MyLocalTon.getInstance().insertBlocksAndTransactions(MyLocalTon.getInstance().getSettings().getGenesisNode(), block, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    transactionsvboxid.getItems().addAll(txRows);
                });
            }
            if (event.getDeltaY() > 0) { // top reached
                log.debug("top reached");
            }
        }
    }

    @FXML
    void scrollBtnAction() {
        MyLocalTon.getInstance().setAutoScroll(!MyLocalTon.getInstance().getAutoScroll());

        if (Boolean.TRUE.equals(MyLocalTon.getInstance().getAutoScroll())) {
            scrollPath.getStyleClass().clear();
            scrollPath.getStyleClass().add("scroll-btn-path-on");
        } else {
            scrollPath.getStyleClass().clear();
            scrollPath.getStyleClass().add("scroll-btn-path-off");
        }
        log.debug("auto scroll {}", MyLocalTon.getInstance().getAutoScroll());
    }

    private void showLoading(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("modal_progress" + ".fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        stage.setTitle("My modal window");
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(((Node) event.getSource()).getScene().getWindow());
        stage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        superWindow.setOnMousePressed(pressEvent -> {
            superWindow.setOnMouseDragged(dragEvent -> {
                App.primaryStage.setX(dragEvent.getScreenX() - pressEvent.getSceneX());
                App.primaryStage.setY(dragEvent.getScreenY() - pressEvent.getSceneY());
            });
        });

        settings = MyLocalTon.getInstance().getSettings();

        WebEngine browser = webView.getEngine();

        EventHandler<KeyEvent> onlyDigits = keyEvent -> {
            if (!((TextField) keyEvent.getSource()).getText().matches("[\\d\\.\\-]+")) {
                ((TextField) keyEvent.getSource()).setText(((TextField) keyEvent.getSource()).getText().replaceAll("[^\\d\\.\\-]", ""));
            }
        };

        listenFor(CustomActionEvent.class, this::handle);
        listenFor(CustomSearchEvent.class, this::handle);
        coinsPerWallet.getTextField().setOnKeyTyped(onlyDigits);

        configNodePublicPort1.setOnKeyTyped(onlyDigits);
        configNodeConsolePort1.setOnKeyTyped(onlyDigits);
        configLiteServerPort1.setOnKeyTyped(onlyDigits);
        configDhtServerPort1.setOnKeyTyped(onlyDigits);
        validatorWalletDeposit1.setOnKeyTyped(onlyDigits);
        validatorDefaultStake1.setOnKeyTyped(onlyDigits);
        nodeStateTtl1.setOnKeyTyped(onlyDigits);
        nodeBlockTtl1.setOnKeyTyped(onlyDigits);
        nodeArchiveTtl1.setOnKeyTyped(onlyDigits);
        nodeKeyProofTtl1.setOnKeyTyped(onlyDigits);
        nodeSyncBefore1.setOnKeyTyped(onlyDigits);

        configNodePublicPort2.setOnKeyTyped(onlyDigits);
        configNodeConsolePort2.setOnKeyTyped(onlyDigits);
        configLiteServerPort2.setOnKeyTyped(onlyDigits);
        validatorWalletDeposit2.setOnKeyTyped(onlyDigits);
        validatorDefaultStake2.setOnKeyTyped(onlyDigits);
        nodeStateTtl2.setOnKeyTyped(onlyDigits);
        nodeBlockTtl2.setOnKeyTyped(onlyDigits);
        nodeArchiveTtl2.setOnKeyTyped(onlyDigits);
        nodeKeyProofTtl2.setOnKeyTyped(onlyDigits);
        nodeSyncBefore2.setOnKeyTyped(onlyDigits);

        configNodePublicPort3.setOnKeyTyped(onlyDigits);
        configNodeConsolePort3.setOnKeyTyped(onlyDigits);
        configLiteServerPort3.setOnKeyTyped(onlyDigits);
        validatorWalletDeposit3.setOnKeyTyped(onlyDigits);
        validatorDefaultStake3.setOnKeyTyped(onlyDigits);
        nodeStateTtl3.setOnKeyTyped(onlyDigits);
        nodeBlockTtl3.setOnKeyTyped(onlyDigits);
        nodeArchiveTtl3.setOnKeyTyped(onlyDigits);
        nodeKeyProofTtl3.setOnKeyTyped(onlyDigits);
        nodeSyncBefore3.setOnKeyTyped(onlyDigits);

        configNodePublicPort4.setOnKeyTyped(onlyDigits);
        configNodeConsolePort4.setOnKeyTyped(onlyDigits);
        configLiteServerPort4.setOnKeyTyped(onlyDigits);
        validatorWalletDeposit4.setOnKeyTyped(onlyDigits);
        validatorDefaultStake4.setOnKeyTyped(onlyDigits);
        nodeStateTtl4.setOnKeyTyped(onlyDigits);
        nodeBlockTtl4.setOnKeyTyped(onlyDigits);
        nodeArchiveTtl4.setOnKeyTyped(onlyDigits);
        nodeKeyProofTtl4.setOnKeyTyped(onlyDigits);
        nodeSyncBefore4.setOnKeyTyped(onlyDigits);

        configNodePublicPort5.setOnKeyTyped(onlyDigits);
        configNodeConsolePort5.setOnKeyTyped(onlyDigits);
        configLiteServerPort5.setOnKeyTyped(onlyDigits);
        validatorWalletDeposit5.setOnKeyTyped(onlyDigits);
        validatorDefaultStake5.setOnKeyTyped(onlyDigits);
        nodeStateTtl5.setOnKeyTyped(onlyDigits);
        nodeBlockTtl5.setOnKeyTyped(onlyDigits);
        nodeArchiveTtl5.setOnKeyTyped(onlyDigits);
        nodeKeyProofTtl5.setOnKeyTyped(onlyDigits);
        nodeSyncBefore5.setOnKeyTyped(onlyDigits);

        configNodePublicPort6.setOnKeyTyped(onlyDigits);
        configNodeConsolePort6.setOnKeyTyped(onlyDigits);
        configLiteServerPort6.setOnKeyTyped(onlyDigits);
        validatorWalletDeposit6.setOnKeyTyped(onlyDigits);
        validatorDefaultStake6.setOnKeyTyped(onlyDigits);
        nodeStateTtl6.setOnKeyTyped(onlyDigits);
        nodeBlockTtl6.setOnKeyTyped(onlyDigits);
        nodeArchiveTtl6.setOnKeyTyped(onlyDigits);
        nodeKeyProofTtl6.setOnKeyTyped(onlyDigits);
        nodeSyncBefore6.setOnKeyTyped(onlyDigits);

        configNodePublicPort7.setOnKeyTyped(onlyDigits);
        configNodeConsolePort7.setOnKeyTyped(onlyDigits);
        configLiteServerPort7.setOnKeyTyped(onlyDigits);
        validatorWalletDeposit7.setOnKeyTyped(onlyDigits);
        validatorDefaultStake7.setOnKeyTyped(onlyDigits);
        nodeStateTtl7.setOnKeyTyped(onlyDigits);
        nodeBlockTtl7.setOnKeyTyped(onlyDigits);
        nodeArchiveTtl7.setOnKeyTyped(onlyDigits);
        nodeKeyProofTtl7.setOnKeyTyped(onlyDigits);
        nodeSyncBefore7.setOnKeyTyped(onlyDigits);

        globalId.setOnKeyTyped(onlyDigits);
        initialBalance.setOnKeyTyped(onlyDigits);
        maxMainValidators.setOnKeyTyped(onlyDigits);
        minValidators.setOnKeyTyped(onlyDigits);
        maxValidators.setOnKeyTyped(onlyDigits);
        electedFor.setOnKeyTyped(onlyDigits);
        electionStartBefore.setOnKeyTyped(onlyDigits);
        electionEndBefore.setOnKeyTyped(onlyDigits);
        stakesFrozenFor.setOnKeyTyped(onlyDigits);
        gasPrice.setOnKeyTyped(onlyDigits);
        gasPriceMc.setOnKeyTyped(onlyDigits);
        cellPrice.setOnKeyTyped(onlyDigits);
        cellPriceMc.setOnKeyTyped(onlyDigits);
        minStake.setOnKeyTyped(onlyDigits);
        maxStake.setOnKeyTyped(onlyDigits);
        minTotalStake.setOnKeyTyped(onlyDigits);
        maxFactor.setOnKeyTyped(onlyDigits);
        electionEndBefore.setOnKeyTyped(onlyDigits);

        mainLayout.getSearchBar().getTextField().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                log.debug("search for {}", mainLayout.getSearchBar().getSearchText());

                foundBlockslistviewid.getItems().clear();
                foundTxsvboxid.getItems().clear();
                foundAccountsvboxid.getItems().clear();

                String searchFor = mainLayout.getSearchBar().getSearchText();

                List<BlockEntity> foundBlocksEntities = App.dbPool.searchBlocks(searchFor);
                MyLocalTon.getInstance().showFoundBlocksInGui(foundBlocksEntities, searchFor);

                List<TxEntity> foundTxsEntities = App.dbPool.searchTxs(searchFor);
                MyLocalTon.getInstance().showFoundTxsInGui(null, foundTxsEntities, searchFor, "");

                List<WalletEntity> foundAccountsEntities = App.dbPool.searchAccounts(searchFor);
                MyLocalTon.getInstance().showFoundAccountsInGui(foundAccountsEntities, searchFor);
                Platform.runLater(() -> emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SHOW)));
            }
        });

        tickTockCheckBox.setSelected(settings.getUiSettings().isShowTickTockTransactions());
        mainConfigTxCheckBox.setSelected(settings.getUiSettings().isShowMainConfigTransactions());
        inOutMsgsCheckBox.setSelected(settings.getUiSettings().isShowInOutMessages());
        enableBlockchainExplorer.setSelected(settings.getUiSettings().isEnableBlockchainExplorer());
        enableTonHttpApi.setSelected(settings.getUiSettings().isEnableTonHttpApi());
        showMsgBodyCheckBox.setSelected(settings.getUiSettings().isShowBodyInMessage());
        shardStateCheckbox.setSelected(settings.getUiSettings().isShowShardStateInBlockDump());

        coinsPerWallet.setFieldText(settings.getWalletSettings().getInitialAmount().toString());

        validatorLogDir1.setFieldText(settings.getGenesisNode().getTonLogDir());
        myLocalTonLog.setFieldText(MyLocalTonSettings.LOG_FILE);
        dhtLogDir1.setFieldText(settings.getGenesisNode().getDhtServerDir());

        validatorLogDir2.setFieldText(settings.getNode2().getTonLogDir());
        validatorLogDir3.setFieldText(settings.getNode3().getTonLogDir());
        validatorLogDir4.setFieldText(settings.getNode4().getTonLogDir());
        validatorLogDir5.setFieldText(settings.getNode5().getTonLogDir());
        validatorLogDir6.setFieldText(settings.getNode6().getTonLogDir());
        validatorLogDir7.setFieldText(settings.getNode7().getTonLogDir());

        minValidators.setFieldText(settings.getBlockchainSettings().getMinValidators().toString());
        maxValidators.setFieldText(settings.getBlockchainSettings().getMaxValidators().toString());
        maxMainValidators.setFieldText(settings.getBlockchainSettings().getMaxMainValidators().toString());

        electedFor.setFieldText(settings.getBlockchainSettings().getElectedFor().toString());
        electionStartBefore.setFieldText(settings.getBlockchainSettings().getElectionStartBefore().toString());
        electionEndBefore.setFieldText(settings.getBlockchainSettings().getElectionEndBefore().toString());
        stakesFrozenFor.setFieldText(settings.getBlockchainSettings().getElectionStakesFrozenFor().toString());

        globalId.setFieldText(settings.getBlockchainSettings().getGlobalId().toString());
        initialBalance.setFieldText(settings.getBlockchainSettings().getInitialBalance().toString());
        gasPrice.setFieldText(settings.getBlockchainSettings().getGasPrice().toString());
        gasPriceMc.setFieldText(settings.getBlockchainSettings().getGasPriceMc().toString());
        cellPrice.setFieldText(settings.getBlockchainSettings().getCellPrice().toString());
        cellPriceMc.setFieldText(settings.getBlockchainSettings().getCellPriceMc().toString());

        minStake.setFieldText(settings.getBlockchainSettings().getMinValidatorStake().toString());
        maxStake.setFieldText(settings.getBlockchainSettings().getMaxValidatorStake().toString());
        minTotalStake.setFieldText(settings.getBlockchainSettings().getMinTotalValidatorStake().toString());
        maxFactor.setFieldText(settings.getBlockchainSettings().getMaxFactor().toString());

        nodeBlockTtl1.setText(settings.getGenesisNode().getValidatorBlockTtl().toString());
        nodeArchiveTtl1.setText(settings.getGenesisNode().getValidatorArchiveTtl().toString());
        nodeKeyProofTtl1.setText(settings.getGenesisNode().getValidatorKeyProofTtl().toString());
        nodeStateTtl1.setText(settings.getGenesisNode().getValidatorStateTtl().toString());
        nodeSyncBefore1.setText(settings.getGenesisNode().getValidatorSyncBefore().toString());
        configNodePublicPort1.setText(settings.getGenesisNode().getPublicPort().toString());
        configNodeConsolePort1.setText(settings.getGenesisNode().getConsolePort().toString());
        configLiteServerPort1.setText(settings.getGenesisNode().getLiteServerPort().toString());
        configDhtServerPort1.setText(settings.getGenesisNode().getDhtPort().toString());
        validatorWalletDeposit1.setText(settings.getGenesisNode().getInitialValidatorWalletAmount().toString());
        validatorDefaultStake1.setText(settings.getGenesisNode().getDefaultValidatorStake().toString());

        nodeBlockTtl2.setText(settings.getNode2().getValidatorBlockTtl().toString());
        nodeArchiveTtl2.setText(settings.getNode2().getValidatorArchiveTtl().toString());
        nodeKeyProofTtl2.setText(settings.getNode2().getValidatorKeyProofTtl().toString());
        nodeStateTtl2.setText(settings.getNode2().getValidatorStateTtl().toString());
        nodeSyncBefore2.setText(settings.getNode2().getValidatorSyncBefore().toString());
        configNodePublicPort2.setText(settings.getNode2().getPublicPort().toString());
        configNodeConsolePort2.setText(settings.getNode2().getConsolePort().toString());
        configLiteServerPort2.setText(settings.getNode2().getLiteServerPort().toString());
        validatorWalletDeposit2.setText(settings.getNode2().getInitialValidatorWalletAmount().toString());
        validatorDefaultStake2.setText(settings.getNode2().getDefaultValidatorStake().toString());

        nodeBlockTtl3.setText(settings.getNode3().getValidatorBlockTtl().toString());
        nodeArchiveTtl3.setText(settings.getNode3().getValidatorArchiveTtl().toString());
        nodeKeyProofTtl3.setText(settings.getNode3().getValidatorKeyProofTtl().toString());
        nodeStateTtl3.setText(settings.getNode3().getValidatorStateTtl().toString());
        nodeSyncBefore3.setText(settings.getNode3().getValidatorSyncBefore().toString());
        configNodePublicPort3.setText(settings.getNode3().getPublicPort().toString());
        configNodeConsolePort3.setText(settings.getNode3().getConsolePort().toString());
        configLiteServerPort3.setText(settings.getNode3().getLiteServerPort().toString());
        validatorWalletDeposit3.setText(settings.getNode3().getInitialValidatorWalletAmount().toString());
        validatorDefaultStake3.setText(settings.getNode3().getDefaultValidatorStake().toString());

        nodeBlockTtl4.setText(settings.getNode4().getValidatorBlockTtl().toString());
        nodeArchiveTtl4.setText(settings.getNode4().getValidatorArchiveTtl().toString());
        nodeKeyProofTtl4.setText(settings.getNode4().getValidatorKeyProofTtl().toString());
        nodeStateTtl4.setText(settings.getNode4().getValidatorStateTtl().toString());
        nodeSyncBefore4.setText(settings.getNode4().getValidatorSyncBefore().toString());
        configNodePublicPort4.setText(settings.getNode4().getPublicPort().toString());
        configNodeConsolePort4.setText(settings.getNode4().getConsolePort().toString());
        configLiteServerPort4.setText(settings.getNode4().getLiteServerPort().toString());
        validatorWalletDeposit4.setText(settings.getNode4().getInitialValidatorWalletAmount().toString());
        validatorDefaultStake4.setText(settings.getNode4().getDefaultValidatorStake().toString());

        nodeBlockTtl5.setText(settings.getNode5().getValidatorBlockTtl().toString());
        nodeArchiveTtl5.setText(settings.getNode5().getValidatorArchiveTtl().toString());
        nodeKeyProofTtl5.setText(settings.getNode5().getValidatorKeyProofTtl().toString());
        nodeStateTtl5.setText(settings.getNode5().getValidatorStateTtl().toString());
        nodeSyncBefore5.setText(settings.getNode5().getValidatorSyncBefore().toString());
        configNodePublicPort5.setText(settings.getNode5().getPublicPort().toString());
        configNodeConsolePort5.setText(settings.getNode5().getConsolePort().toString());
        configLiteServerPort5.setText(settings.getNode5().getLiteServerPort().toString());
        validatorWalletDeposit5.setText(settings.getNode5().getInitialValidatorWalletAmount().toString());
        validatorDefaultStake5.setText(settings.getNode5().getDefaultValidatorStake().toString());

        nodeBlockTtl6.setText(settings.getNode6().getValidatorBlockTtl().toString());
        nodeArchiveTtl6.setText(settings.getNode6().getValidatorArchiveTtl().toString());
        nodeKeyProofTtl6.setText(settings.getNode6().getValidatorKeyProofTtl().toString());
        nodeStateTtl6.setText(settings.getNode6().getValidatorStateTtl().toString());
        nodeSyncBefore6.setText(settings.getNode6().getValidatorSyncBefore().toString());
        configNodePublicPort6.setText(settings.getNode6().getPublicPort().toString());
        configNodeConsolePort6.setText(settings.getNode6().getConsolePort().toString());
        configLiteServerPort6.setText(settings.getNode6().getLiteServerPort().toString());
        validatorWalletDeposit6.setText(settings.getNode6().getInitialValidatorWalletAmount().toString());
        validatorDefaultStake6.setText(settings.getNode6().getDefaultValidatorStake().toString());

        nodeBlockTtl7.setText(settings.getNode7().getValidatorBlockTtl().toString());
        nodeArchiveTtl7.setText(settings.getNode7().getValidatorArchiveTtl().toString());
        nodeKeyProofTtl7.setText(settings.getNode7().getValidatorKeyProofTtl().toString());
        nodeStateTtl7.setText(settings.getNode7().getValidatorStateTtl().toString());
        nodeSyncBefore7.setText(settings.getNode7().getValidatorSyncBefore().toString());
        configNodePublicPort7.setText(settings.getNode7().getPublicPort().toString());
        configNodeConsolePort7.setText(settings.getNode7().getConsolePort().toString());
        configLiteServerPort7.setText(settings.getNode7().getLiteServerPort().toString());
        validatorWalletDeposit7.setText(settings.getNode7().getInitialValidatorWalletAmount().toString());
        validatorDefaultStake7.setText(settings.getNode7().getDefaultValidatorStake().toString());

        tonLogLevel.addItem("DEBUG");
        tonLogLevel.addItem("WARNING");
        tonLogLevel.addItem("INFO");
        tonLogLevel.addItem("ERROR");
        tonLogLevel.addItem("FATAL");
        tonLogLevel.selectItem(settings.getGenesisNode().getTonLogLevel());

        tonLogLevel2.addItem("DEBUG");
        tonLogLevel2.addItem("WARNING");
        tonLogLevel2.addItem("INFO");
        tonLogLevel2.addItem("ERROR");
        tonLogLevel2.addItem("FATAL");
        tonLogLevel2.selectItem(settings.getNode2().getTonLogLevel());

        tonLogLevel3.addItem("DEBUG");
        tonLogLevel3.addItem("WARNING");
        tonLogLevel3.addItem("INFO");
        tonLogLevel3.addItem("ERROR");
        tonLogLevel3.addItem("FATAL");
        tonLogLevel3.selectItem(settings.getNode3().getTonLogLevel());

        tonLogLevel4.addItem("DEBUG");
        tonLogLevel4.addItem("WARNING");
        tonLogLevel4.addItem("INFO");
        tonLogLevel4.addItem("ERROR");
        tonLogLevel4.addItem("FATAL");
        tonLogLevel4.selectItem(settings.getNode4().getTonLogLevel());

        tonLogLevel5.addItem("DEBUG");
        tonLogLevel5.addItem("WARNING");
        tonLogLevel5.addItem("INFO");
        tonLogLevel5.addItem("ERROR");
        tonLogLevel5.addItem("FATAL");
        tonLogLevel5.selectItem(settings.getNode5().getTonLogLevel());

        tonLogLevel6.addItem("DEBUG");
        tonLogLevel6.addItem("WARNING");
        tonLogLevel6.addItem("INFO");
        tonLogLevel6.addItem("ERROR");
        tonLogLevel6.addItem("FATAL");
        tonLogLevel6.selectItem(settings.getNode6().getTonLogLevel());

        tonLogLevel7.addItem("DEBUG");
        tonLogLevel7.addItem("WARNING");
        tonLogLevel7.addItem("INFO");
        tonLogLevel7.addItem("ERROR");
        tonLogLevel7.addItem("FATAL");
        tonLogLevel7.selectItem(settings.getNode7().getTonLogLevel());

        myLogLevel.addItem("INFO");
        myLogLevel.addItem("DEBUG");
        myLogLevel.addItem("ERROR");
        myLogLevel.selectItem(settings.getGenesisNode().getMyLocalTonLogLevel());

        enableBlockchainExplorer.setVisible(false);
        enableBlockchainExplorerLabel.setVisible(false);

        enableBlockchainExplorer.setVisible(true);
        enableBlockchainExplorerLabel.setVisible(true);

        enableTonHttpApi.setVisible(false);
        enableTonHttpApiLabel.setVisible(false);

        enableTonHttpApi.setVisible(true);
        enableTonHttpApiLabel.setVisible(true);

        addValidatorBtn.setVisible(true);

        // validator-tabs
        validationTabs.getTabs().remove(validator2tab);
        validationTabs.getTabs().remove(validator3tab);
        validationTabs.getTabs().remove(validator4tab);
        validationTabs.getTabs().remove(validator5tab);
        validationTabs.getTabs().remove(validator6tab);
        validationTabs.getTabs().remove(validator7tab);

        for (String n : Arrays.asList("node2", "node3", "node4", "node5", "node6", "node7")) {
            if (settings.getActiveNodes().contains(n)) {
                validationTabs.getTabs().add(getNodeTabByName(n));
            }
        }
        mainLayout.setExplorer(enableBlockchainExplorer.isSelected());
        mainLayout.setTonHttpApi(enableTonHttpApi.isSelected());
    }

    public Tab getNodeTabByName(String nodeName) {
        switch (nodeName) {
            case "genesis":
                return genesisnode1;
            case "node2":
                return validator2tab;
            case "node3":
                return validator3tab;
            case "node4":
                return validator4tab;
            case "node5":
                return validator5tab;
            case "node6":
                return validator6tab;
            case "node7":
                return validator7tab;
            default:
                return null;
        }
    }

    public void startNativeBlockchainExplorer() {
        if (settings.getUiSettings().isEnableBlockchainExplorer()) {
            log.info("Starting native blockchain-explorer on port {}", settings.getUiSettings().getBlockchainExplorerPort());
            BlockchainExplorer blockchainExplorer = new BlockchainExplorer();
            blockchainExplorer.startBlockchainExplorer(settings.getGenesisNode(), settings.getGenesisNode().getNodeGlobalConfigLocation(), settings.getUiSettings().getBlockchainExplorerPort());
            Utils.sleep(2);
            webView.getEngine().load("http://127.0.0.1:" + settings.getUiSettings().getBlockchainExplorerPort() + "/last");
        }
    }

    public void startTonHttpApi() {
        if (settings.getUiSettings().isEnableTonHttpApi()) {
            log.info("Starting ton-http-api on port {}", settings.getUiSettings().getTonHttpApiPort());
            Utils.sleep(3);
            TonHttpApi tonHttpApi = new TonHttpApi();
            tonHttpApi.startTonHttpApi(settings.getGenesisNode(), settings.getGenesisNode().getNodeGlobalConfigLocation(), settings.getUiSettings().getTonHttpApiPort());
            Utils.sleep(5);
            webViewTonHttpApi.getEngine().load("http://127.0.0.1:" + settings.getUiSettings().getTonHttpApiPort());
        }
    }

    public void showAccTxs(String hexAddr) {
        foundAccountsTxsvboxid.getItems().clear();
        List<TxEntity> foundAccountTxsEntities = App.dbPool.searchTxs(hexAddr);
        MyLocalTon.getInstance().showFoundTxsInGui(foundAccountsTxsvboxid, foundAccountTxsEntities, hexAddr, hexAddr);
    }

    public void saveSettings() {
        log.debug("saving all settings");
        settings.getUiSettings().setShowTickTockTransactions(tickTockCheckBox.isSelected());
        settings.getUiSettings().setShowMainConfigTransactions(mainConfigTxCheckBox.isSelected());
        settings.getUiSettings().setShowInOutMessages(inOutMsgsCheckBox.isSelected());
        settings.getUiSettings().setShowBodyInMessage(showMsgBodyCheckBox.isSelected());
        settings.getUiSettings().setEnableBlockchainExplorer(enableBlockchainExplorer.isSelected());
        settings.getUiSettings().setEnableTonHttpApi(enableTonHttpApi.isSelected());
        settings.getUiSettings().setShowShardStateInBlockDump(shardStateCheckbox.isSelected());

        settings.getWalletSettings().setInitialAmount(new BigInteger(coinsPerWallet.getFieldText()));

        settings.getBlockchainSettings().setMinValidators(Long.valueOf(minValidators.getFieldText()));
        settings.getBlockchainSettings().setMaxValidators(Long.valueOf(maxValidators.getFieldText()));
        settings.getBlockchainSettings().setMaxMainValidators(Long.valueOf(maxMainValidators.getFieldText()));

        settings.getBlockchainSettings().setGlobalId(Long.valueOf(globalId.getFieldText()));
        settings.getBlockchainSettings().setInitialBalance(Long.valueOf(initialBalance.getFieldText()));

        settings.getBlockchainSettings().setElectedFor(Long.valueOf(electedFor.getFieldText()));
        settings.getBlockchainSettings().setElectionStartBefore(Long.valueOf(electionStartBefore.getFieldText()));
        settings.getBlockchainSettings().setElectionEndBefore(Long.valueOf(electionEndBefore.getFieldText()));
        settings.getBlockchainSettings().setElectionStakesFrozenFor(Long.valueOf(stakesFrozenFor.getFieldText()));
        settings.getBlockchainSettings().setGasPrice(Long.valueOf(gasPrice.getFieldText()));
        settings.getBlockchainSettings().setGasPriceMc(Long.valueOf(gasPriceMc.getFieldText()));
        settings.getBlockchainSettings().setCellPrice(Long.valueOf(cellPrice.getFieldText()));
        settings.getBlockchainSettings().setCellPriceMc(Long.valueOf(cellPriceMc.getFieldText()));

        settings.getBlockchainSettings().setMinValidatorStake(Long.valueOf(minStake.getFieldText()));
        settings.getBlockchainSettings().setMaxValidatorStake(Long.valueOf(maxStake.getFieldText()));
        settings.getBlockchainSettings().setMinTotalValidatorStake(Long.valueOf(minTotalStake.getFieldText()));
        settings.getBlockchainSettings().setMaxFactor(new BigDecimal(maxFactor.getFieldText()));

        settings.getGenesisNode().setValidatorBlockTtl(Long.valueOf(nodeBlockTtl1.getText()));
        settings.getGenesisNode().setValidatorArchiveTtl(Long.valueOf(nodeArchiveTtl1.getText()));
        settings.getGenesisNode().setValidatorKeyProofTtl(Long.valueOf(nodeKeyProofTtl1.getText()));
        settings.getGenesisNode().setValidatorStateTtl(Long.valueOf(nodeStateTtl1.getText()));
        settings.getGenesisNode().setValidatorSyncBefore(Long.valueOf(nodeSyncBefore1.getText()));
        settings.getGenesisNode().setMyLocalTonLogLevel(myLogLevel.getValue());
        settings.getGenesisNode().setPublicPort(Integer.valueOf(configNodePublicPort1.getText()));
        settings.getGenesisNode().setConsolePort(Integer.valueOf(configNodeConsolePort1.getText()));
        settings.getGenesisNode().setLiteServerPort(Integer.valueOf(configLiteServerPort1.getText()));
        settings.getGenesisNode().setDhtPort(Integer.valueOf(configDhtServerPort1.getText()));
        settings.getGenesisNode().setInitialValidatorWalletAmount(new BigInteger(validatorWalletDeposit1.getText()));
        settings.getGenesisNode().setDefaultValidatorStake(new BigInteger(validatorDefaultStake1.getText()));

        settings.getNode2().setValidatorBlockTtl(Long.valueOf(nodeBlockTtl2.getText()));
        settings.getNode2().setValidatorArchiveTtl(Long.valueOf(nodeArchiveTtl2.getText()));
        settings.getNode2().setValidatorKeyProofTtl(Long.valueOf(nodeKeyProofTtl2.getText()));
        settings.getNode2().setValidatorStateTtl(Long.valueOf(nodeStateTtl2.getText()));
        settings.getNode2().setValidatorSyncBefore(Long.valueOf(nodeSyncBefore2.getText()));
        settings.getNode2().setPublicPort(Integer.valueOf(configNodePublicPort2.getText()));
        settings.getNode2().setConsolePort(Integer.valueOf(configNodeConsolePort2.getText()));
        settings.getNode2().setLiteServerPort(Integer.valueOf(configLiteServerPort2.getText()));
        settings.getNode2().setInitialValidatorWalletAmount(new BigInteger(validatorWalletDeposit2.getText()));
        settings.getNode2().setDefaultValidatorStake(new BigInteger(validatorDefaultStake2.getText()));
        settings.getNode2().setTonLogLevel(tonLogLevel2.getValue());

        settings.getNode3().setValidatorBlockTtl(Long.valueOf(nodeBlockTtl3.getText()));
        settings.getNode3().setValidatorArchiveTtl(Long.valueOf(nodeArchiveTtl3.getText()));
        settings.getNode3().setValidatorKeyProofTtl(Long.valueOf(nodeKeyProofTtl3.getText()));
        settings.getNode3().setValidatorStateTtl(Long.valueOf(nodeStateTtl3.getText()));
        settings.getNode3().setValidatorSyncBefore(Long.valueOf(nodeSyncBefore3.getText()));
        settings.getNode3().setPublicPort(Integer.valueOf(configNodePublicPort3.getText()));
        settings.getNode3().setConsolePort(Integer.valueOf(configNodeConsolePort3.getText()));
        settings.getNode3().setLiteServerPort(Integer.valueOf(configLiteServerPort3.getText()));
        settings.getNode3().setInitialValidatorWalletAmount(new BigInteger(validatorWalletDeposit3.getText()));
        settings.getNode3().setDefaultValidatorStake(new BigInteger(validatorDefaultStake3.getText()));
        settings.getNode3().setTonLogLevel(tonLogLevel3.getValue());

        settings.getNode4().setValidatorBlockTtl(Long.valueOf(nodeBlockTtl4.getText()));
        settings.getNode4().setValidatorArchiveTtl(Long.valueOf(nodeArchiveTtl4.getText()));
        settings.getNode4().setValidatorKeyProofTtl(Long.valueOf(nodeKeyProofTtl4.getText()));
        settings.getNode4().setValidatorStateTtl(Long.valueOf(nodeStateTtl4.getText()));
        settings.getNode4().setValidatorSyncBefore(Long.valueOf(nodeSyncBefore4.getText()));
        settings.getNode4().setPublicPort(Integer.valueOf(configNodePublicPort4.getText()));
        settings.getNode4().setConsolePort(Integer.valueOf(configNodeConsolePort4.getText()));
        settings.getNode4().setLiteServerPort(Integer.valueOf(configLiteServerPort4.getText()));
        settings.getNode4().setInitialValidatorWalletAmount(new BigInteger(validatorWalletDeposit4.getText()));
        settings.getNode4().setDefaultValidatorStake(new BigInteger(validatorDefaultStake4.getText()));
        settings.getNode4().setTonLogLevel(tonLogLevel4.getValue());

        settings.getNode5().setValidatorBlockTtl(Long.valueOf(nodeBlockTtl5.getText()));
        settings.getNode5().setValidatorArchiveTtl(Long.valueOf(nodeArchiveTtl5.getText()));
        settings.getNode5().setValidatorKeyProofTtl(Long.valueOf(nodeKeyProofTtl5.getText()));
        settings.getNode5().setValidatorStateTtl(Long.valueOf(nodeStateTtl5.getText()));
        settings.getNode5().setValidatorSyncBefore(Long.valueOf(nodeSyncBefore5.getText()));
        settings.getNode5().setPublicPort(Integer.valueOf(configNodePublicPort5.getText()));
        settings.getNode5().setConsolePort(Integer.valueOf(configNodeConsolePort5.getText()));
        settings.getNode5().setLiteServerPort(Integer.valueOf(configLiteServerPort5.getText()));
        settings.getNode5().setInitialValidatorWalletAmount(new BigInteger(validatorWalletDeposit5.getText()));
        settings.getNode5().setDefaultValidatorStake(new BigInteger(validatorDefaultStake5.getText()));
        settings.getNode5().setTonLogLevel(tonLogLevel5.getValue());

        settings.getNode6().setValidatorBlockTtl(Long.valueOf(nodeBlockTtl6.getText()));
        settings.getNode6().setValidatorArchiveTtl(Long.valueOf(nodeArchiveTtl6.getText()));
        settings.getNode6().setValidatorKeyProofTtl(Long.valueOf(nodeKeyProofTtl6.getText()));
        settings.getNode6().setValidatorStateTtl(Long.valueOf(nodeStateTtl6.getText()));
        settings.getNode6().setValidatorSyncBefore(Long.valueOf(nodeSyncBefore6.getText()));
        settings.getNode6().setPublicPort(Integer.valueOf(configNodePublicPort6.getText()));
        settings.getNode6().setConsolePort(Integer.valueOf(configNodeConsolePort6.getText()));
        settings.getNode6().setLiteServerPort(Integer.valueOf(configLiteServerPort6.getText()));
        settings.getNode6().setInitialValidatorWalletAmount(new BigInteger(validatorWalletDeposit6.getText()));
        settings.getNode6().setDefaultValidatorStake(new BigInteger(validatorDefaultStake6.getText()));
        settings.getNode6().setTonLogLevel(tonLogLevel6.getValue());

        settings.getNode7().setValidatorBlockTtl(Long.valueOf(nodeBlockTtl7.getText()));
        settings.getNode7().setValidatorArchiveTtl(Long.valueOf(nodeArchiveTtl7.getText()));
        settings.getNode7().setValidatorKeyProofTtl(Long.valueOf(nodeKeyProofTtl7.getText()));
        settings.getNode7().setValidatorStateTtl(Long.valueOf(nodeStateTtl7.getText()));
        settings.getNode7().setValidatorSyncBefore(Long.valueOf(nodeSyncBefore7.getText()));
        settings.getNode7().setPublicPort(Integer.valueOf(configNodePublicPort7.getText()));
        settings.getNode7().setConsolePort(Integer.valueOf(configNodeConsolePort7.getText()));
        settings.getNode7().setLiteServerPort(Integer.valueOf(configLiteServerPort7.getText()));
        settings.getNode7().setInitialValidatorWalletAmount(new BigInteger(validatorWalletDeposit7.getText()));
        settings.getNode7().setDefaultValidatorStake(new BigInteger(validatorDefaultStake7.getText()));
        settings.getNode7().setTonLogLevel(tonLogLevel7.getValue());

        MyLocalTonUtils.saveSettingsToGson(settings);
    }

    public void accountsOnScroll(ScrollEvent scrollEvent) {
        log.debug("accountsOnScroll");
    }

    public void foundBlocksOnScroll(ScrollEvent scrollEvent) {
        log.debug("foundBlocksOnScroll");
    }

    public void foundTxsOnScroll(ScrollEvent scrollEvent) {
        log.debug("foundTxsOnScroll");
    }

    public void liteServerClicked() throws IOException {
        String lastCommand = LiteClient.getInstance(LiteClientEnum.GLOBAL).getLastCommand(MyLocalTon.getInstance().getSettings().getGenesisNode());
        log.info("show console with last command, {}", lastCommand);

        if (SystemUtils.IS_OS_WINDOWS) {
            log.info("cmd /c start cmd.exe /k \"echo " + lastCommand + " && " + lastCommand + "\"");
            Runtime.getRuntime().exec("cmd /c start cmd.exe /k \"echo " + lastCommand + " && " + lastCommand + "\"");
        } else if (SystemUtils.IS_OS_LINUX) {
            if (Files.exists(Paths.get("/usr/bin/xterm"))) {
                log.info("/usr/bin/xterm -hold -geometry 200 -e " + lastCommand);
                Runtime.getRuntime().exec("/usr/bin/xterm -hold -geometry 200 -e " + lastCommand);
            } else {
                log.info("xterm is not installed");
            }
        } else {
            //log.info("zsh -c \"" + lastCommand + "\"");
            //Runtime.getRuntime().exec("zsh -c \"" + lastCommand + "\"");
            log.debug("terminal call not implemented");
        }

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(lastCommand);
        clipboard.setContent(content);
        log.debug(lastCommand + " copied");
        App.mainController.showInfoMsg("lite-client last command copied to clipboard", 2);
    }

    public void resetAction() {
        FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/reset-blockchain-pane.fxml"));

        Parent parent = null;
        try {
            parent = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(parent);

        yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
        yesNoDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        yesNoDialog.close();
                    }
                }
        );

        yesNoDialog.show();
    }

    public void showDialogMessage(String header, String body) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/confirm-pane.fxml"));
                Parent parent = loader.load();
                ConfirmPaneController controller = loader.getController();
                controller.setAction(ConfirmPaneController.Action.CONFIRM);

                controller.setHeader(header);
                controller.setBody(body);

                JFXDialogLayout content = new JFXDialogLayout();
                content.setBody(parent);

                yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
                yesNoDialog.setOnKeyPressed(keyEvent -> {
                            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                                yesNoDialog.close();
                            }
                        }
                );
                yesNoDialog.show();
            } catch (IOException e) {
                log.error("Cannot load resource org/ton/ui/custom/layout/confirm-pane.fxml");
                e.printStackTrace();
            }
        });
    }

    public void showDialogConfirmDeleteNode(org.ton.settings.Node node) {
        Platform.runLater(() -> {
            try {

                FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/confirm-pane.fxml"));
                Parent parent = loader.load();
                ConfirmPaneController controller = loader.getController();
                controller.setHeight(470.0);
                controller.setAction(ConfirmPaneController.Action.DELETE_NODE);
                controller.setHeader("Confirmation");
                controller.setAddress(node.getNodeName());

                String stopsWokring = "";
                MyLocalTonSettings settings = MyLocalTon.getInstance().getSettings();
                if (SystemUtils.IS_OS_WINDOWS) {
                    stopsWokring = "\n\nIf you delete this node your main workchain becomes inactive. In comparison with Linux and MacOS versions of MyLocalTon, where you can remove validators until you maintain a two-thirds consensus, " +
                            "on Windows the whole blockchain stops working if you remove one node. If consensus is met upon restart you will be fine.";
                } else {
                    int cutoff = (int) Math.ceil(settings.getActiveNodes().size() * 66 / 100.0);
                    log.info("total active nodes {} vs minimum required {}", settings.getActiveNodes().size(), cutoff);
                    if ((settings.getActiveNodes().size() - 1 < cutoff) || (settings.getActiveNodes().size() == 3 && cutoff == 2)) {
                        stopsWokring = "\n\nIf this node is an active validator your main workchain becomes inactive, i.e. stops working, since a two-thirds consensus of validators will not be reached.";
                    }
                }
                controller.setBody("Are you sure you want to delete the selected validator? All data and funds will be lost and obviously validator will be removed from elections. Don't forget to collect all validation rewards." + stopsWokring);

                JFXDialogLayout content = new JFXDialogLayout();
                content.setBody(parent);

                yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
                yesNoDialog.setOnKeyPressed(keyEvent -> {
                            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                                yesNoDialog.close();
                            }
                        }
                );
                yesNoDialog.show();
            } catch (IOException e) {
                log.error("Cannot load resource org/ton/ui/custom/layout/confirm-pane.fxml");
                e.printStackTrace();
            }
        });
    }

    public void showDialogInstallPython() {
        Platform.runLater(() -> {
            try {

                FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/confirm-pane.fxml"));
                Parent parent = loader.load();
                ConfirmPaneController controller = loader.getController();
                controller.setHeight(170.0);
                controller.setAction(ConfirmPaneController.Action.INSTALL_PYTHON);
                if (SystemUtils.IS_OS_MAC) {
                    controller.setHeader("python 3.11+ is not installed");
                    controller.setBody("Do you want to download and start the Python installation now?");

                } else {
                    controller.setHeader("python3 is not installed");
                    controller.setBody("Do you want to download and start the Python installation now?");
                }

                controller.setOkButtonText("Install now");

                JFXDialogLayout content = new JFXDialogLayout();
                content.setBody(parent);

                yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
                yesNoDialog.setOnKeyPressed(keyEvent -> {
                            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                                yesNoDialog.close();
                            }
                        }
                );
                yesNoDialog.show();
            } catch (IOException e) {
                log.error("Cannot load resource org/ton/ui/custom/layout/confirm-pane.fxml");
                e.printStackTrace();
            }
        });
    }

    public void showDialogInstallPip() {
        Platform.runLater(() -> {
            try {

                FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/confirm-pane.fxml"));
                Parent parent = loader.load();
                ConfirmPaneController controller = loader.getController();
                controller.setHeight(170.0);
                controller.setAction(ConfirmPaneController.Action.INSTALL_PIP);
                controller.setHeader("pip is not installed");

                controller.setBody("Python is installed, but pip (package installer for Python) is missing. Do you want to install pip now?");
                controller.setOkButtonText("Install now");

                JFXDialogLayout content = new JFXDialogLayout();
                content.setBody(parent);

                yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
                yesNoDialog.setOnKeyPressed(keyEvent -> {
                            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                                yesNoDialog.close();
                            }
                        }
                );
                yesNoDialog.show();
            } catch (IOException e) {
                log.error("Cannot load resource org/ton/ui/custom/layout/confirm-pane.fxml");
                e.printStackTrace();
            }
        });
    }

    public void showDialogInstallTonHttpApi() {
        Platform.runLater(() -> {
            try {

                FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/confirm-pane.fxml"));
                Parent parent = loader.load();
                ConfirmPaneController controller = loader.getController();
                controller.setHeight(170.0);
                controller.setAction(ConfirmPaneController.Action.INSTALL_TON_HTTP_API);
                controller.setHeader("ton-http-api is not installed");

                controller.setBody("Python and pip are installed, but ton-http-api is missing. Do you want to install ton-http-api now?");
                controller.setOkButtonText("Install now");

                JFXDialogLayout content = new JFXDialogLayout();
                content.setBody(parent);

                yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
                yesNoDialog.setOnKeyPressed(keyEvent -> {
                            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                                yesNoDialog.close();
                            }
                        }
                );
                yesNoDialog.show();
            } catch (IOException e) {
                log.error("Cannot load resource org/ton/ui/custom/layout/confirm-pane.fxml");
                e.printStackTrace();
            }
        });
    }

    public void showMessage(String msg) {
        try {

            FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/confirm-pane.fxml"));
            Parent parent = loader.load();
            ConfirmPaneController controller = loader.getController();
            controller.setAction(ConfirmPaneController.Action.CONFIRM);

            controller.setHeader("Message");
            controller.setBody(msg);
            controller.setOkButtonText("Close");

            JFXDialogLayout content = new JFXDialogLayout();
            content.setBody(parent);

            yesNoDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
            yesNoDialog.setOnKeyPressed(keyEvent -> {
                        if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                            yesNoDialog.close();
                        }
                    }
            );

            yesNoDialog.show();
        } catch (Exception e) {
            log.error("Cannot show message, error {}", e.getMessage());
        }
    }

    public void createNewAccountBtn() throws IOException {

        FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/accounts-create-pane.fxml"));
        Parent parent = loader.load();
        AccountsCreatePaneController controller = loader.getController();

//        controller.setWalletVersionText(settings.getWalletSettings().getWalletVersion().getValue());

//        if (
//                (settings.getWalletSettings().getWalletVersion().equals(WalletVersion.V3R1))
//                        || (settings.getWalletSettings().getWalletVersion().equals(WalletVersion.V3R2))
//                        || (settings.getWalletSettings().getWalletVersion().equals(WalletVersion.V4R2))
//        ) {
//            controller.showSubWalletID();
//        } else {
//            controller.hideSubWalletID();
//        }

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(parent);

        createDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);

        createDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        createDialog.close();
                    }
                }
        );

        createDialog.show();

    }

    public void updateValidationTabInfo() {
        try {

            ValidationParam v;
            Object lastKey = settings.elections.keySet().toArray()[settings.elections.size() - 1];  // get last element
            v = settings.elections.get(lastKey);

            LiteClient liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);
            ResultConfig34 config34 = LiteClientParser.parseConfig34(liteClient.executeGetCurrentValidators(settings.getGenesisNode()));
            ResultConfig32 config32 = LiteClientParser.parseConfig32(liteClient.executeGetPreviousValidators(settings.getGenesisNode()));
            ResultConfig36 config36 = LiteClientParser.parseConfig36(liteClient.executeGetNextValidators(settings.getGenesisNode()));

            totalValidators.setText(config32.getValidators().getValidators().size() + " / " + config34.getValidators().getValidators().size() + " / " + config36.getValidators().getValidators().size());
            String previous = "Previous validators (Public key, ADNL address, weight): " + System.lineSeparator() + config32.getValidators().getValidators().stream().map(i -> i.getPublicKey() + "  " + i.getAdnlAddress() + "  " + i.getWeight()).collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator() + System.lineSeparator();
            String current = "Current validators: " + System.lineSeparator() + config34.getValidators().getValidators().stream().map(i -> i.getPublicKey() + "  " + i.getAdnlAddress() + "  " + i.getWeight()).collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator() + System.lineSeparator();
            String next = "Next validators (available only within a Break time): " + System.lineSeparator() + config36.getValidators().getValidators().stream().map(i -> i.getPublicKey() + "  " + i.getAdnlAddress() + "  " + i.getWeight()).collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator();
            totalValidators.setTooltip(new Tooltip(previous + current + next));

            blockchainLaunched.setText(MyLocalTonUtils.toLocal(v.getBlockchainLaunchTime()));

            colorValidationTiming(v);

            long validationStartInAgoSeconds = Math.abs(MyLocalTonUtils.getCurrentTimeSeconds() - v.getStartValidationCycle());
            String startsValidationDuration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(validationStartInAgoSeconds).toMillis(), "HH:mm:ss", true);
            if ((MyLocalTonUtils.getCurrentTimeSeconds() - v.getStartValidationCycle()) > 0) {
                startCycle.setText(MyLocalTonUtils.toLocal(v.getStartValidationCycle()) + "  Started ago (" + startsValidationDuration + ")");
            } else {
                startCycle.setText(MyLocalTonUtils.toLocal(v.getStartValidationCycle()) + "  Starts in (" + startsValidationDuration + ")");
            }
            long validationDurationInSeconds = v.getEndValidationCycle() - v.getStartValidationCycle();
            String validation1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(validationDurationInSeconds).toMillis(), "HH:mm:ss", true);
            endCycle.setText(MyLocalTonUtils.toLocal(v.getEndValidationCycle()) + "  Duration (" + validation1Duration + ")");

            long electionsStartsInAgoSeconds = Math.abs(MyLocalTonUtils.getCurrentTimeSeconds() - v.getStartElections());
            String startsElectionDuration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(electionsStartsInAgoSeconds).toMillis(), "HH:mm:ss", true);
            if ((MyLocalTonUtils.getCurrentTimeSeconds() - v.getStartElections()) > 0) {
                startElections.setText(MyLocalTonUtils.toLocal(v.getStartElections()) + "  Started ago (" + startsElectionDuration + ") Election Id " + v.getStartValidationCycle());
                startElections.setTooltip(new Tooltip("Election Id (" + MyLocalTonUtils.toLocal(v.getStartValidationCycle()) + ")"));
            } else {
                startElections.setText(MyLocalTonUtils.toLocal(v.getStartElections()) + "  Starts in (" + startsElectionDuration + ")");
                startElections.setTooltip(null);
            }
            long electionDurationInSeconds = v.getEndElections() - v.getStartElections();
            String elections1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(electionDurationInSeconds).toMillis(), "HH:mm:ss", true);

            endElections.setText(MyLocalTonUtils.toLocal(v.getEndElections()) + "  Duration (" + elections1Duration + ")");

            long nextElectionsStartsInAgoSeconds = Math.abs(MyLocalTonUtils.getCurrentTimeSeconds() - v.getNextElections());
            String nextElectionDuration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(nextElectionsStartsInAgoSeconds).toMillis(), "HH:mm:ss", true);
            if ((MyLocalTonUtils.getCurrentTimeSeconds() - v.getNextElections()) > 0) {
                nextElections.setText(MyLocalTonUtils.toLocal(v.getNextElections()) + "  Started ago (" + nextElectionDuration + ")");
            } else {
                nextElections.setText(MyLocalTonUtils.toLocal(v.getNextElections()) + "  Starts in (" + nextElectionDuration + ")");
            }

            minterAddr.setText(v.getMinterAddr());
            configAddr.setText(v.getConfigAddr());
            electorAddr.setText(v.getElectorAddr());
            validationPeriod.setText(v.getValidationDuration().toString() + " (" + DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(v.getValidationDuration()).toMillis(), "HH:mm:ss", true) + ")");
            electionPeriod.setText(v.getElectionDuration().toString() + " (" + DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(v.getElectionDuration()).toMillis(), "HH:mm:ss", true) + ")");

            holdPeriod.setText(v.getHoldPeriod().toString() + " (" + DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(v.getHoldPeriod()).toMillis(), "HH:mm:ss", true) + ")");
            minimumStake.setText(String.format("%,.9f", new BigDecimal(v.getMinStake().divide(BigInteger.valueOf(ONE_BLN)))));
            maximumStake.setText(String.format("%,.9f", new BigDecimal(v.getMaxStake().divide(BigInteger.valueOf(ONE_BLN)))));

            //every 30 sec
            //MyLocalTonSettings settings = MyLocalTon.getInstance().getSettings();

            LiteClientAccountState accountState = LiteClientParser.parseGetAccount(liteClient.executeGetAccount(settings.getGenesisNode(), settings.getMainWalletAddrFull()));
            minterBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));

            accountState = LiteClientParser.parseGetAccount(liteClient.executeGetAccount(settings.getGenesisNode(), settings.getConfigSmcAddrHex()));
            configBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));

            accountState = LiteClientParser.parseGetAccount(liteClient.executeGetAccount(settings.getGenesisNode(), settings.getElectorSmcAddrHex()));
            electorBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));

            List<ResultListParticipants> participants = LiteClientParser.parseRunMethodParticipantList(liteClient.executeGetParticipantList(settings.getGenesisNode(), settings.getElectorSmcAddrHex()));
            totalParticipants.setText(String.valueOf(participants.size()));
            String participantsTooltip = "Participants (Public key, weight): " + System.lineSeparator() + LiteClientParser.parseRunMethodParticipantList(liteClient.executeGetParticipantList(settings.getGenesisNode(), settings.getElectorSmcAddrHex())).stream().map(i -> i.getPubkey() + "  " + i.getWeight()).collect(Collectors.joining(System.lineSeparator())) + System.lineSeparator();
            totalParticipants.setTooltip(new Tooltip(participantsTooltip));

            // validator pages
            updateValidator1TabPage(v);
            updateValidator2TabPage(v);
            updateValidator3TabPage(v);
            updateValidator4TabPage(v);
            updateValidator5TabPage(v);
            updateValidator6TabPage(v);
            updateValidator7TabPage(v);

        } catch (Exception e) {
            log.error("Error updating validation tab GUI! Error {}", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private void updateValidator1TabPage(ValidationParam v) {
        GenesisNode node1 = settings.getGenesisNode();
        if (nonNull(node1.getWalletAddress())) {
            if (isNull(node1.getPrevValidationAndlKey())) { // very first elections, no previous validators yet
                validator1AdnlAddress.setText(v.getCurrentValidators().get(0).getAdnlAddress());
                validator1PubKeyHex.setText(v.getCurrentValidators().get(0).getPublicKey());
                validator1PubKeyInteger.setText(new BigInteger(v.getCurrentValidators().get(0).getPublicKey().toUpperCase(), 16) + " (used in participants list)");
            } else { // in a list of current validators we must find an entry from previous next validators
                for (Validator validator : v.getCurrentValidators()) {
                    if (nonNull(validator.getAdnlAddress())) {
                        if (validator.getAdnlAddress().equals(node1.getPrevValidationAndlKey())) {
                            validator1AdnlAddress.setText(validator.getAdnlAddress());
                            validator1PubKeyHex.setText(validator.getPublicKey());
                            validator1PubKeyInteger.setText(new BigInteger(validator.getPublicKey().toUpperCase(), 16) + " (used in participants list)");
                        }
                    }
                }
            }

            LiteClientAccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(settings.getGenesisNode(), node1.getWalletAddress().getFullWalletAddress()));
            validator1AdnlAddressNext.setText(node1.getValidationAndlKey());
            validator1PubKeyHexNext.setText(node1.getValidationPubKeyHex());
            validator1PubKeyIntegerNext.setText(node1.getValidationPubKeyInteger());
            validator1WalletAddress.setText(node1.getWalletAddress().getFullWalletAddress());
            validator1WalletBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
            nodePublicPort1.setText(node1.getPublicPort().toString());
            nodeConsolePort1.setText(node1.getConsolePort().toString());
            liteServerPort1.setText(node1.getLiteServerPort().toString());
        }
    }

    private void updateValidator2TabPage(ValidationParam v) {
        Node2 node2 = settings.getNode2();
        if (nonNull(node2.getWalletAddress())) {
            if (nonNull(node2.getPrevValidationAndlKey())) {
                for (Validator validator : v.getCurrentValidators()) {  // in a list of current validators we must find an entry from previous next validators
                    if (nonNull(validator.getAdnlAddress())) {
                        if (validator.getAdnlAddress().equals(node2.getPrevValidationAndlKey())) {
                            validator2AdnlAddress.setText(validator.getAdnlAddress());
                            validator2PubKeyHex.setText(validator.getPublicKey());
                            validator2PubKeyInteger.setText(new BigInteger(validator.getPublicKey().toUpperCase(), 16) + " (used in participants list)");
                        }
                    }
                }
            }

            LiteClientAccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(settings.getGenesisNode(), node2.getWalletAddress().getFullWalletAddress()));
            validator2AdnlAddressNext.setText(node2.getValidationAndlKey());
            validator2PubKeyHexNext.setText(node2.getValidationPubKeyHex());
            validator2PubKeyIntegerNext.setText(node2.getValidationPubKeyInteger());
            validator2WalletAddress.setText(node2.getWalletAddress().getFullWalletAddress());
            validator2WalletBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
            nodePublicPort2.setText(node2.getPublicPort().toString());
            nodeConsolePort2.setText(node2.getConsolePort().toString());
            liteServerPort2.setText(node2.getLiteServerPort().toString());

            deleteValidatorBtn2.setDisable(isNull(node2.getWalletAddress()));
        }
    }

    private void updateValidator3TabPage(ValidationParam v) {
        Node3 node3 = settings.getNode3();
        if (nonNull(node3.getWalletAddress())) {
            if (nonNull(node3.getPrevValidationAndlKey())) {
                for (Validator validator : v.getCurrentValidators()) {
                    if (nonNull(validator.getAdnlAddress())) {
                        if (validator.getAdnlAddress().equals(node3.getPrevValidationAndlKey())) {
                            validator3AdnlAddress.setText(validator.getAdnlAddress());
                            validator3PubKeyHex.setText(validator.getPublicKey());
                            validator3PubKeyInteger.setText(new BigInteger(validator.getPublicKey().toUpperCase(), 16) + " (used in participants list)");
                        }
                    }
                }
            }

            LiteClientAccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(settings.getGenesisNode(), node3.getWalletAddress().getFullWalletAddress()));
            validator3AdnlAddressNext.setText(node3.getValidationAndlKey());
            validator3PubKeyHexNext.setText(node3.getValidationPubKeyHex());
            validator3PubKeyIntegerNext.setText(node3.getValidationPubKeyInteger());
            validator3WalletAddress.setText(node3.getWalletAddress().getFullWalletAddress());
            validator3WalletBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
            nodePublicPort3.setText(node3.getPublicPort().toString());
            nodeConsolePort3.setText(node3.getConsolePort().toString());
            liteServerPort3.setText(node3.getLiteServerPort().toString());

            deleteValidatorBtn3.setDisable(isNull(node3.getWalletAddress()));
        }
    }

    private void updateValidator4TabPage(ValidationParam v) {
        Node4 node4 = settings.getNode4();
        if (nonNull(node4.getWalletAddress())) {
            if (nonNull(node4.getPrevValidationAndlKey())) {
                for (Validator validator : v.getCurrentValidators()) {
                    if (nonNull(validator.getAdnlAddress())) {
                        if (validator.getAdnlAddress().equals(node4.getPrevValidationAndlKey())) {
                            validator4AdnlAddress.setText(validator.getAdnlAddress());
                            validator4PubKeyHex.setText(validator.getPublicKey());
                            validator4PubKeyInteger.setText(new BigInteger(validator.getPublicKey().toUpperCase(), 16) + " (used in participants list)");
                        }
                    }
                }
            }

            LiteClientAccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(settings.getGenesisNode(), node4.getWalletAddress().getFullWalletAddress()));
            validator4AdnlAddressNext.setText(node4.getValidationAndlKey());
            validator4PubKeyHexNext.setText(node4.getValidationPubKeyHex());
            validator4PubKeyIntegerNext.setText(node4.getValidationPubKeyInteger());
            validator4WalletAddress.setText(node4.getWalletAddress().getFullWalletAddress());
            validator4WalletBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
            nodePublicPort4.setText(node4.getPublicPort().toString());
            nodeConsolePort4.setText(node4.getConsolePort().toString());
            liteServerPort4.setText(node4.getLiteServerPort().toString());

            deleteValidatorBtn4.setDisable(isNull(node4.getWalletAddress()));
        }
    }

    private void updateValidator5TabPage(ValidationParam v) {
        Node5 node5 = settings.getNode5();
        if (nonNull(node5.getWalletAddress())) {
            if (nonNull(node5.getPrevValidationAndlKey())) {
                for (Validator validator : v.getCurrentValidators()) {
                    if (nonNull(validator.getAdnlAddress())) {
                        if (nonNull(validator.getAdnlAddress())) {
                            if (validator.getAdnlAddress().equals(node5.getPrevValidationAndlKey())) {
                                validator5AdnlAddress.setText(validator.getAdnlAddress());
                                validator5PubKeyHex.setText(validator.getPublicKey());
                                validator5PubKeyInteger.setText(new BigInteger(validator.getPublicKey().toUpperCase(), 16) + " (used in participants list)");
                            }
                        }
                    }
                }
            }

            LiteClientAccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(settings.getGenesisNode(), node5.getWalletAddress().getFullWalletAddress()));
            validator5AdnlAddressNext.setText(node5.getValidationAndlKey());
            validator5PubKeyHexNext.setText(node5.getValidationPubKeyHex());
            validator5PubKeyIntegerNext.setText(node5.getValidationPubKeyInteger());
            validator5WalletAddress.setText(node5.getWalletAddress().getFullWalletAddress());
            validator5WalletBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
            nodePublicPort5.setText(node5.getPublicPort().toString());
            nodeConsolePort5.setText(node5.getConsolePort().toString());
            liteServerPort5.setText(node5.getLiteServerPort().toString());

            deleteValidatorBtn5.setDisable(isNull(node5.getWalletAddress()));
        }
    }

    private void updateValidator6TabPage(ValidationParam v) {
        Node6 node6 = settings.getNode6();
        if (nonNull(node6.getWalletAddress())) {
            if (nonNull(node6.getPrevValidationAndlKey())) {
                for (Validator validator : v.getCurrentValidators()) {
                    if (nonNull(validator.getAdnlAddress())) {
                        if (validator.getAdnlAddress().equals(node6.getPrevValidationAndlKey())) {
                            validator6AdnlAddress.setText(validator.getAdnlAddress());
                            validator6PubKeyHex.setText(validator.getPublicKey());
                            validator6PubKeyInteger.setText(new BigInteger(validator.getPublicKey().toUpperCase(), 16) + " (used in participants list)");
                        }
                    }
                }
            }

            LiteClientAccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(settings.getGenesisNode(), node6.getWalletAddress().getFullWalletAddress()));
            validator6AdnlAddressNext.setText(node6.getValidationAndlKey());
            validator6PubKeyHexNext.setText(node6.getValidationPubKeyHex());
            validator6PubKeyIntegerNext.setText(node6.getValidationPubKeyInteger());
            validator6WalletAddress.setText(node6.getWalletAddress().getFullWalletAddress());
            validator6WalletBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
            nodePublicPort6.setText(node6.getPublicPort().toString());
            nodeConsolePort6.setText(node6.getConsolePort().toString());
            liteServerPort6.setText(node6.getLiteServerPort().toString());

            if (isNull(node6.getWalletAddress())) {
                deleteValidatorBtn6.setDisable(true);
            } else {
                deleteValidatorBtn6.setDisable(false);
            }
        }
    }

    private void updateValidator7TabPage(ValidationParam v) {
        Node7 node7 = settings.getNode7();
        if (nonNull(node7.getWalletAddress())) {
            if (nonNull(node7.getPrevValidationAndlKey())) {
                for (Validator validator : v.getCurrentValidators()) {
                    if (nonNull(validator.getAdnlAddress())) {
                        if (validator.getAdnlAddress().equals(node7.getPrevValidationAndlKey())) {
                            validator7AdnlAddress.setText(validator.getAdnlAddress());
                            validator7PubKeyHex.setText(validator.getPublicKey());
                            validator7PubKeyInteger.setText(new BigInteger(validator.getPublicKey().toUpperCase(), 16) + " (used in participants list)");
                        }
                    }
                }
            }

            LiteClientAccountState accountState = LiteClientParser.parseGetAccount(LiteClient.getInstance(LiteClientEnum.GLOBAL).executeGetAccount(settings.getGenesisNode(), node7.getWalletAddress().getFullWalletAddress()));
            validator7AdnlAddressNext.setText(node7.getValidationAndlKey());
            validator7PubKeyHexNext.setText(node7.getValidationPubKeyHex());
            validator7PubKeyIntegerNext.setText(node7.getValidationPubKeyInteger() + " (used in participants list)");
            validator7WalletAddress.setText(node7.getWalletAddress().getFullWalletAddress());
            validator7WalletBalance.setText(String.format("%,.9f", accountState.getBalance().getToncoins().divide(BigDecimal.valueOf(ONE_BLN), 9, RoundingMode.CEILING)));
            nodePublicPort7.setText(node7.getPublicPort().toString());
            nodeConsolePort7.setText(node7.getConsolePort().toString());
            liteServerPort7.setText(node7.getLiteServerPort().toString());

            deleteValidatorBtn7.setDisable(isNull(node7.getWalletAddress()));
        }
    }

    private void colorValidationTiming(ValidationParam v) {

        long currentTime = System.currentTimeMillis() / 1000;

        if (v.getStartValidationCycle() > currentTime) {
            startCycle.setTextFill(Color.GREEN);
        } else {
            startCycle.setTextFill(Color.BLACK);
        }

        if (v.getEndValidationCycle() > currentTime) {
            endCycle.setTextFill(Color.GREEN);
        } else {
            endCycle.setTextFill(Color.BLACK);
        }

        if (v.getStartElections() > currentTime) {
            startElections.setTextFill(Color.GREEN);
        } else {
            startElections.setTextFill(Color.BLACK);
        }

        if (v.getEndElections() > currentTime) {
            endElections.setTextFill(Color.GREEN);
        } else {
            endElections.setTextFill(Color.BLACK);
        }

        if (v.getNextElections() > currentTime) {
            nextElections.setTextFill(Color.GREEN);
        } else {
            nextElections.setTextFill(Color.BLACK);
        }
    }

    public void drawElections() {
        Platform.runLater(() -> {
            try {
                mainController.drawBarsAndLabels(); // once in elections enough (bars and labels)
                mainController.updateValidationTabInfo();
                mainController.drawTimeLine();

                mainController.electionsChartPane.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                log.error(ExceptionUtils.getStackTrace(e));
            }
        });
    }

    public void drawBarsAndLabels() {
        log.debug("draw drawBarsAndLabels");

        try {
            Object lastKey = settings.elections.keySet().toArray()[settings.elections.size() - 1];
            ValidationParam v = settings.elections.get(lastKey);

            positionBars(v);

            addLabelsToBars2(v);

            calculateTimeLineScale();

            saveSettings();

        } catch (Exception e) {
            e.printStackTrace();
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private void calculateTimeLineScale() {
        Object lastKey = settings.elections.keySet().toArray()[settings.elections.size() - 1];  // get last element
        ValidationParam v = settings.elections.get(lastKey);

        long startXHoldStakeLine2 = (long) stakeHoldRange2.getLayoutX();
        long startXHoldStakeLine3 = (long) stakeHoldRange3.getLayoutX();
        long endHoldStake2 = settings.getStakeHoldRange2End();
        long endHoldStake3 = settings.getStakeHoldRange3End();

        double scaleFactor = (double) 200 / v.getValidationDuration();
        long holdStakeWidth = (long) (v.getHoldPeriod() * scaleFactor);

        log.debug("startXHoldStakeLine3 {}, endHoldStake2 {}, endHoldStake3 {} holdStakeWidth {}", startXHoldStakeLine3, endHoldStake2, endHoldStake3, holdStakeWidth);

        long fullWidthInPixels = 0;
        long fullDurationSeconds = 0;

        if (settings.elections.size() > 1) {
            fullDurationSeconds = endHoldStake3 - v.getStartElections() + v.getValidationDuration();
            fullWidthInPixels = startXHoldStakeLine3 + holdStakeWidth;
        } else {
            fullDurationSeconds = endHoldStake3 - v.getStartElections();
            fullWidthInPixels = startXHoldStakeLine3 + holdStakeWidth;
        }

        double scale = (double) fullWidthInPixels / fullDurationSeconds;
        log.debug("full width {}px, {}s, scale {}", fullWidthInPixels, fullDurationSeconds, scale);
        settings.setTimeLineScale(scale);
    }

    public void drawTimeLine() {
        log.debug("draw time-line");
        long currentTime = MyLocalTonUtils.getCurrentTimeSeconds();

        if (nonNull(settings.getTimeLineScale())) {

            Object lastLastKey = settings.elections.keySet().toArray()[settings.elections.size() - 1];
            ValidationParam v = settings.elections.get(lastLastKey);
            long electionsDelta = v.getNextElections() - v.getStartElections();

            if ((currentTime - v.getStartElections()) > (electionsDelta * 2)) {
                Object lastKey = settings.elections.keySet().toArray()[settings.elections.size() - 1];
                v = settings.elections.get(lastKey);
            }

            long x = currentTime - v.getStartElections();
            double scaleFactor = (double) 200 / v.getValidationDuration();
            double xcoord;
            electionsDelta = v.getNextElections() - v.getStartElections();
            long electionsDeltaWidth = (long) (electionsDelta * scaleFactor);

            if (settings.elections.size() > 1) {
                xcoord = electionsDeltaWidth + (x * settings.getTimeLineScale());
            } else {
                xcoord = 0 + (x * settings.getTimeLineScale());
            }

            log.debug("electionsDelta {}, electionsDeltaWidth {}, timeScale {}, xcoord {}, x {}, v.getStartElections() {}, sizeElections {}", electionsDelta, electionsDeltaWidth, settings.getTimeLineScale(), xcoord, x, MyLocalTonUtils.toLocal(v.getStartElections()), settings.elections.size());
            timeLine.setLayoutX(xcoord);
            timeLine.setTooltip(new Tooltip(MyLocalTonUtils.toLocal(currentTime)));
            saveSettings();
        }
    }

    private void positionBars(ValidationParam v) {
        // assume duration of validation cycle is 1 unit of 200 pixels, then other ranges scaled down/up accordingly
        double scaleFactor = (double) 200 / v.getValidationDuration();

        long space = 3;
        long validationWidth = 200;
        long electionsWidth = (long) ((v.getEndElections() - v.getStartElections()) * scaleFactor);
        long pauseWidth = (long) ((v.getStartValidationCycle() - v.getEndElections()) * scaleFactor);
        long holdStakeWidth = (long) (v.getHoldPeriod() * scaleFactor);
        long electionsDelta = v.getNextElections() - v.getStartElections();
        long electionsDeltaWidth = (long) (electionsDelta * scaleFactor);
        log.debug("elections delta {}s, {}px", electionsDelta, electionsDeltaWidth);

        // start X position of line 1 (very first elections)
        long startXElectionsLine1 = 0;
        long startXPauseLine1 = startXElectionsLine1 + electionsWidth + space;
        long startXValidationLine1 = startXPauseLine1 + pauseWidth + space;
        long startXHoldStakeLine1 = startXValidationLine1 + validationWidth + space;

        // start X position of line 2 (next elections)
        long startXElectionsLine2 = (long) ((startXValidationLine1 + validationWidth) - (scaleFactor * v.getStartElectionsBefore())) - 3;
        long startXPauseLine2 = startXElectionsLine2 + electionsWidth + space;
        long startXValidationLine2 = startXPauseLine2 + pauseWidth + space;
        long startXHoldStakeLine2 = startXValidationLine2 + validationWidth + space;

        // start X position of line 3 (next elections)
        long startXElectionsLine3 = startXElectionsLine2 + startXElectionsLine2 - startXElectionsLine1;
        long startXPauseLine3 = startXElectionsLine3 + electionsWidth + space;
        long startXValidationLine3 = startXPauseLine3 + pauseWidth + space;
        long startXHoldStakeLine3 = startXValidationLine3 + validationWidth + space;

        log.debug("electionsWidth {}, pauseWidth {}, validationWidth {}, hostStakeWidth {}", electionsWidth, pauseWidth, validationWidth, holdStakeWidth);

        electionsRange1.setMinWidth(electionsWidth);
        electionsRange2.setMinWidth(electionsWidth);
        electionsRange3.setMinWidth(electionsWidth);

        pauseRange1.setMinWidth(pauseWidth);
        pauseRange2.setMinWidth(pauseWidth);
        pauseRange3.setMinWidth(pauseWidth);

        validationRange1.setMinWidth(validationWidth);
        validationRange2.setMinWidth(validationWidth);
        validationRange3.setMinWidth(validationWidth);

        stakeHoldRange1.setMinWidth(holdStakeWidth);
        stakeHoldRange2.setMinWidth(holdStakeWidth);
        stakeHoldRange3.setMinWidth(holdStakeWidth);

        electionsRange1.setLayoutX(startXElectionsLine1);
        pauseRange1.setLayoutX(startXPauseLine1);
        validationRange1.setLayoutX(startXValidationLine1);
        stakeHoldRange1.setLayoutX(startXHoldStakeLine1);

        electionsRange2.setLayoutX(startXElectionsLine2);
        pauseRange2.setLayoutX(startXPauseLine2);
        validationRange2.setLayoutX(startXValidationLine2);
        stakeHoldRange2.setLayoutX(startXHoldStakeLine2);

        electionsRange3.setLayoutX(startXElectionsLine3);
        pauseRange3.setLayoutX(startXPauseLine3);
        validationRange3.setLayoutX(startXValidationLine3);
        stakeHoldRange3.setLayoutX(startXHoldStakeLine3);

    }

    private void addLabelsToBars2(ValidationParam v1) {
        long electionsDelta1 = v1.getNextElections() - v1.getStartElections();
        long electionDurationInSeconds1 = v1.getEndElections() - v1.getStartElections();
        long electionDurationInSeconds2 = 0;
        String elections1Duration;
        String elections1ToolTip;
        long pauseDurationInSeconds;
        String pause1Duration;
        long validationDurationInSeconds;
        String validation1Duration;
        long stakeHoldDurationInSeconds;
        String stakeHold1Duration;

        ValidationParam v2 = null;

        if (settings.elections.size() > 1) {
            Object beforeLastKey = settings.elections.keySet().toArray()[settings.elections.size() - 2];
            v2 = settings.elections.get(beforeLastKey);
            electionDurationInSeconds2 = v2.getEndElections() - v2.getStartElections();
        }

        if (settings.elections.size() == 1) {
            elections1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(electionDurationInSeconds1).toMillis(), "HH:mm:ss", true);
            elections1ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v1.getStartElections()), MyLocalTonUtils.toLocal(v1.getEndElections()), elections1Duration);
            electionsRange1.setTooltip(new Tooltip(elections1ToolTip));

            pauseDurationInSeconds = v1.getStartValidationCycle() - v1.getEndElections();
            pause1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(pauseDurationInSeconds).toMillis(), "HH:mm:ss", true);
            String pause1ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v1.getEndElections()), MyLocalTonUtils.toLocal(v1.getStartValidationCycle()), pause1Duration);
            pauseRange1.setTooltip(new Tooltip(pause1ToolTip));

            validationDurationInSeconds = v1.getEndValidationCycle() - v1.getStartValidationCycle();
            validation1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(validationDurationInSeconds).toMillis(), "HH:mm:ss", true);
            String validation1ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v1.getStartValidationCycle()), MyLocalTonUtils.toLocal(v1.getEndValidationCycle()), validation1Duration);
            validationRange1.setTooltip(new Tooltip(validation1ToolTip));

            stakeHoldDurationInSeconds = v1.getHoldPeriod();
            stakeHold1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(stakeHoldDurationInSeconds).toMillis(), "HH:mm:ss", true);
            String holdStake1ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v1.getEndValidationCycle()), MyLocalTonUtils.toLocal(java.time.Duration.ofSeconds(v1.getEndValidationCycle()).plusSeconds(v1.getHoldPeriod()).toSeconds()), stakeHold1Duration);
            stakeHoldRange1.setTooltip(new Tooltip(holdStake1ToolTip));

            //2
            long endElections2 = java.time.Duration.ofSeconds(v1.getNextElections()).plusSeconds(electionDurationInSeconds1).toSeconds();
            String elections2ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v1.getNextElections()), MyLocalTonUtils.toLocal(endElections2), elections1Duration);
            electionsRange2.setTooltip(new Tooltip(elections2ToolTip));

            long endPause2 = java.time.Duration.ofSeconds(endElections2).plusSeconds(pauseDurationInSeconds).toSeconds();
            String pause2ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endElections2), MyLocalTonUtils.toLocal(endPause2), pause1Duration);
            pauseRange2.setTooltip(new Tooltip(pause2ToolTip));

            long endValidation2 = java.time.Duration.ofSeconds(endPause2).plusSeconds(validationDurationInSeconds).toSeconds();
            String validation2ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endPause2), MyLocalTonUtils.toLocal(endValidation2), validation1Duration);
            validationRange2.setTooltip(new Tooltip(validation2ToolTip));

            long endHoldStake2 = java.time.Duration.ofSeconds(endValidation2).plusSeconds(v1.getHoldPeriod()).toSeconds();
            String holdStake2ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endValidation2), MyLocalTonUtils.toLocal(endHoldStake2), stakeHold1Duration);
            stakeHoldRange2.setTooltip(new Tooltip(holdStake2ToolTip));
            settings.setStakeHoldRange2End(endHoldStake2);

            //3
            long startElections3 = java.time.Duration.ofSeconds(v1.getNextElections()).plusSeconds(electionsDelta1).toSeconds();
            long endElections3 = java.time.Duration.ofSeconds(v1.getNextElections()).plusSeconds(electionDurationInSeconds1 + electionsDelta1).toSeconds();
            String elections3ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(startElections3), MyLocalTonUtils.toLocal(endElections3), elections1Duration);
            electionsRange3.setTooltip(new Tooltip(elections3ToolTip));

            long endPause3 = java.time.Duration.ofSeconds(endElections3).plusSeconds(pauseDurationInSeconds).toSeconds();
            String pause3ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endElections3), MyLocalTonUtils.toLocal(endPause3), pause1Duration);
            pauseRange3.setTooltip(new Tooltip(pause3ToolTip));

            long endValidation3 = java.time.Duration.ofSeconds(endPause3).plusSeconds(validationDurationInSeconds).toSeconds();
            String validation3ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endPause3), MyLocalTonUtils.toLocal(endValidation3), validation1Duration);
            validationRange3.setTooltip(new Tooltip(validation3ToolTip));

            long endHoldStake3 = java.time.Duration.ofSeconds(endValidation3).plusSeconds(v1.getHoldPeriod()).toSeconds();
            String holdStake3ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endValidation3), MyLocalTonUtils.toLocal(endHoldStake3), stakeHold1Duration);
            stakeHoldRange3.setTooltip(new Tooltip(holdStake3ToolTip));
            settings.setStakeHoldRange3End(endHoldStake3);
        } else {
            elections1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(electionDurationInSeconds2).toMillis(), "HH:mm:ss", true);
            elections1ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v2.getStartElections()), MyLocalTonUtils.toLocal(v2.getEndElections()), elections1Duration);
            electionsRange1.setTooltip(new Tooltip(elections1ToolTip));

            pauseDurationInSeconds = v2.getStartValidationCycle() - v2.getEndElections();
            pause1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(pauseDurationInSeconds).toMillis(), "HH:mm:ss", true);
            String pause1ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v2.getEndElections()), MyLocalTonUtils.toLocal(v2.getStartValidationCycle()), pause1Duration);
            pauseRange1.setTooltip(new Tooltip(pause1ToolTip));

            validationDurationInSeconds = v2.getEndValidationCycle() - v2.getStartValidationCycle();
            validation1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(validationDurationInSeconds).toMillis(), "HH:mm:ss", true);
            String validation1ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v2.getStartValidationCycle()), MyLocalTonUtils.toLocal(v2.getEndValidationCycle()), validation1Duration);
            validationRange1.setTooltip(new Tooltip(validation1ToolTip));

            stakeHoldDurationInSeconds = v2.getHoldPeriod();
            stakeHold1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(stakeHoldDurationInSeconds).toMillis(), "HH:mm:ss", true);
            String holdStake1ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v2.getEndValidationCycle()), MyLocalTonUtils.toLocal(java.time.Duration.ofSeconds(v2.getEndValidationCycle()).plusSeconds(v2.getHoldPeriod()).toSeconds()), stakeHold1Duration);
            stakeHoldRange1.setTooltip(new Tooltip(holdStake1ToolTip));

            //2
            long endElections2 = java.time.Duration.ofSeconds(v1.getStartElections()).plusSeconds(electionDurationInSeconds1).toSeconds();
            String elections2ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(v1.getStartElections()), MyLocalTonUtils.toLocal(endElections2), elections1Duration);
            electionsRange2.setTooltip(new Tooltip(elections2ToolTip));

            pauseDurationInSeconds = v1.getStartValidationCycle() - v1.getEndElections();
            long endPause2 = java.time.Duration.ofSeconds(endElections2).plusSeconds(pauseDurationInSeconds).toSeconds();
            String pause2ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endElections2), MyLocalTonUtils.toLocal(endPause2), pause1Duration);
            pauseRange2.setTooltip(new Tooltip(pause2ToolTip));

            long endValidation2 = java.time.Duration.ofSeconds(endPause2).plusSeconds(validationDurationInSeconds).toSeconds();
            String validation2ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endPause2), MyLocalTonUtils.toLocal(endValidation2), validation1Duration);
            validationRange2.setTooltip(new Tooltip(validation2ToolTip));

            long endHoldStake2 = java.time.Duration.ofSeconds(endValidation2).plusSeconds(v1.getHoldPeriod()).toSeconds();
            String holdStake2ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endValidation2), MyLocalTonUtils.toLocal(endHoldStake2), stakeHold1Duration);
            stakeHoldRange2.setTooltip(new Tooltip(holdStake2ToolTip));

            settings.setStakeHoldRange2End(endHoldStake2);

            //3
            long startElections3 = java.time.Duration.ofSeconds(v1.getNextElections()).toSeconds();
            long endElections3 = java.time.Duration.ofSeconds(v1.getNextElections()).plusSeconds(electionDurationInSeconds2).toSeconds();
            String elections3ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(startElections3), MyLocalTonUtils.toLocal(endElections3), elections1Duration);
            electionsRange3.setTooltip(new Tooltip(elections3ToolTip));

            long endPause3 = java.time.Duration.ofSeconds(endElections3).plusSeconds(pauseDurationInSeconds).toSeconds();
            String pause3ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endElections3), MyLocalTonUtils.toLocal(endPause3), pause1Duration);
            pauseRange3.setTooltip(new Tooltip(pause3ToolTip));

            long endValidation3 = java.time.Duration.ofSeconds(endPause3).plusSeconds(validationDurationInSeconds).toSeconds();
            String validation3ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endPause3), MyLocalTonUtils.toLocal(endValidation3), validation1Duration);
            validationRange3.setTooltip(new Tooltip(validation3ToolTip));

            long endHoldStake3 = java.time.Duration.ofSeconds(endValidation3).plusSeconds(v1.getHoldPeriod()).toSeconds();
            String holdStake3ToolTip = String.format("Start: %s%nEnd: %s%nDuration: %s", MyLocalTonUtils.toLocal(endValidation3), MyLocalTonUtils.toLocal(endHoldStake3), stakeHold1Duration);
            stakeHoldRange3.setTooltip(new Tooltip(holdStake3ToolTip));

            settings.setStakeHoldRange3End(endHoldStake3);
        }
    }

    // copy validators fields
    public void validation1AdnlClicked(MouseEvent mouseEvent) {
        String addr = validator1AdnlAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation1WalletAddrClicked(MouseEvent mouseEvent) {
        String addr = validator1WalletAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation1PubKeyIntegerClicked(MouseEvent mouseEvent) {
        String addr = validator1PubKeyInteger.getText();
        if (nonNull(addr)) {
            addr = addr.substring(0, addr.indexOf("(") - 1);
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.info(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation1PubKeyHexClicked(MouseEvent mouseEvent) {
        String addr = validator1PubKeyHex.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation1AdnlClickedNext(MouseEvent mouseEvent) {
        String addr = validator1AdnlAddressNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation1PubKeyHexClickedNext(MouseEvent mouseEvent) {
        String addr = validator1PubKeyHexNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation1PubKeyIntegerClickedNext(MouseEvent mouseEvent) {
        String addr = validator1PubKeyIntegerNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void copyElectionId(MouseEvent mouseEvent) {
        String electionId = startElections.getText();
        electionId = electionId.substring(electionId.indexOf("Election Id ") + 12);
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(electionId);
        clipboard.setContent(content);
        log.debug(electionId + " copied");
        App.mainController.showInfoMsg(electionId + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation2AdnlClicked(MouseEvent mouseEvent) {
        String addr = validator2AdnlAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation2WalletAddrClicked(MouseEvent mouseEvent) {
        String addr = validator2WalletAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation2PubKeyIntegerClicked(MouseEvent mouseEvent) {
        String addr = validator2PubKeyInteger.getText();
        if (nonNull(addr)) {
            addr = addr.substring(0, addr.indexOf("(") - 1);
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.info(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation2PubKeyHexClicked(MouseEvent mouseEvent) {
        String addr = validator2PubKeyHex.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation2AdnlClickedNext(MouseEvent mouseEvent) {
        String addr = validator2AdnlAddressNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation2PubKeyHexClickedNext(MouseEvent mouseEvent) {
        String addr = validator2PubKeyHexNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation2PubKeyIntegerClickedNext(MouseEvent mouseEvent) {
        String addr = validator2PubKeyIntegerNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation3AdnlClicked(MouseEvent mouseEvent) {
        String addr = validator3AdnlAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation3WalletAddrClicked(MouseEvent mouseEvent) {
        String addr = validator3WalletAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation3PubKeyIntegerClicked(MouseEvent mouseEvent) {
        String addr = validator3PubKeyInteger.getText();
        if (nonNull(addr)) {
            addr = addr.substring(0, addr.indexOf("(") - 1);
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.info(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation3PubKeyHexClicked(MouseEvent mouseEvent) {
        String addr = validator3PubKeyHex.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation3AdnlClickedNext(MouseEvent mouseEvent) {
        String addr = validator3AdnlAddressNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation3PubKeyHexClickedNext(MouseEvent mouseEvent) {
        String addr = validator3PubKeyHexNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation3PubKeyIntegerClickedNext(MouseEvent mouseEvent) {
        String addr = validator3PubKeyIntegerNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation4AdnlClicked(MouseEvent mouseEvent) {
        String addr = validator4AdnlAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation4WalletAddrClicked(MouseEvent mouseEvent) {
        String addr = validator4WalletAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation4PubKeyIntegerClicked(MouseEvent mouseEvent) {
        String addr = validator4PubKeyInteger.getText();
        if (nonNull(addr)) {
            addr = addr.substring(0, addr.indexOf("(") - 1);
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.info(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation4PubKeyHexClicked(MouseEvent mouseEvent) {
        String addr = validator4PubKeyHex.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation4AdnlClickedNext(MouseEvent mouseEvent) {
        String addr = validator4AdnlAddressNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation4PubKeyHexClickedNext(MouseEvent mouseEvent) {
        String addr = validator4PubKeyHexNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation4PubKeyIntegerClickedNext(MouseEvent mouseEvent) {
        String addr = validator4PubKeyIntegerNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation5AdnlClicked(MouseEvent mouseEvent) {
        String addr = validator5AdnlAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation5WalletAddrClicked(MouseEvent mouseEvent) {
        String addr = validator5WalletAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation5PubKeyIntegerClicked(MouseEvent mouseEvent) {
        String addr = validator5PubKeyInteger.getText();
        if (nonNull(addr)) {
            addr = addr.substring(0, addr.indexOf("(") - 1);
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.info(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation5PubKeyHexClicked(MouseEvent mouseEvent) {
        String addr = validator5PubKeyHex.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation5AdnlClickedNext(MouseEvent mouseEvent) {
        String addr = validator5AdnlAddressNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation5PubKeyHexClickedNext(MouseEvent mouseEvent) {
        String addr = validator5PubKeyHexNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation5PubKeyIntegerClickedNext(MouseEvent mouseEvent) {
        String addr = validator5PubKeyIntegerNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation6AdnlClicked(MouseEvent mouseEvent) {
        String addr = validator6AdnlAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation6WalletAddrClicked(MouseEvent mouseEvent) {
        String addr = validator6WalletAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation6PubKeyIntegerClicked(MouseEvent mouseEvent) {
        String addr = validator6PubKeyInteger.getText();
        if (nonNull(addr)) {
            addr = addr.substring(0, addr.indexOf("(") - 1);
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.info(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation6PubKeyHexClicked(MouseEvent mouseEvent) {
        String addr = validator6PubKeyHex.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation6AdnlClickedNext(MouseEvent mouseEvent) {
        String addr = validator6AdnlAddressNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation6PubKeyHexClickedNext(MouseEvent mouseEvent) {
        String addr = validator6PubKeyHexNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation6PubKeyIntegerClickedNext(MouseEvent mouseEvent) {
        String addr = validator6PubKeyIntegerNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation7AdnlClicked(MouseEvent mouseEvent) {
        String addr = validator7AdnlAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation7WalletAddrClicked(MouseEvent mouseEvent) {
        String addr = validator7WalletAddress.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation7PubKeyIntegerClicked(MouseEvent mouseEvent) {
        String addr = validator7PubKeyInteger.getText();
        if (nonNull(addr)) {
            addr = addr.substring(0, addr.indexOf("(") - 1);
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.info(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation7PubKeyHexClicked(MouseEvent mouseEvent) {
        String addr = validator7PubKeyHex.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation7AdnlClickedNext(MouseEvent mouseEvent) {
        String addr = validator7AdnlAddressNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation7PubKeyHexClickedNext(MouseEvent mouseEvent) {
        String addr = validator7PubKeyHexNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void validation7PubKeyIntegerClickedNext(MouseEvent mouseEvent) {
        String addr = validator7PubKeyIntegerNext.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        String lightAddr = MyLocalTonUtils.getLightAddress(addr);
        App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void createNewNodeBtn() {

        ExecutorService newNodeExecutorService = Executors.newSingleThreadExecutor();

        newNodeExecutorService.execute(() -> {
            Thread.currentThread().setName("MyLocalTon - Creating validator");

            try {
                mainController.addValidatorBtn.setDisable(true);

                org.ton.settings.Node node = MyLocalTonUtils.getNewNode();
                if (nonNull(node)) {
                    log.info("creating validator {}", node.getNodeName());

                    //delete unfinished or failed node creation
                    FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + node.getNodeName()));

                    MyLocalTon.getInstance().createFullnode(node, true, true);

                    Tab newTab = MyLocalTonUtils.getNewNodeTab();
                    Platform.runLater(() -> validationTabs.getTabs().add(newTab));

                    settings.getActiveNodes().add(node.getNodeName());
                    MyLocalTon.getInstance().saveSettingsToGson();

                    showDialogMessage("Completed", "Validator " + node.getNodeName() + " has been cloned from genesis, now synchronizing and creating main wallet.");

                    log.info("Creating new validator controlling smart-contract (wallet) for node {}", node.getNodeName());
                    MyLocalTon.getInstance().createWalletEntity(node, null, WalletVersion.V3R2, -1L, settings.getWalletSettings().getDefaultSubWalletId(), node.getInitialValidatorWalletAmount(), true);
                    //node.setWalletAddress(walletEntity.getWallet()); // double check

                    mainController.addValidatorBtn.setDisable(false);
                    App.mainController.showInfoMsg("Main wallet for validator " + node.getNodeName() + " has been successfully created.", 5);
                } else {
                    showDialogMessage("The limit has been reached", "It is possible to have up to 6 additional validators. The first one is reserved, thus in total you may have 7 validators.");
                }
            } catch (Exception e) {
                log.error("Error creating validator: {}", e.getMessage());
                App.mainController.showErrorMsg("Error creating validator", 3);
            } finally {
                mainController.addValidatorBtn.setDisable(false);
            }

        });
        newNodeExecutorService.shutdown();
    }

    public void deleteValidator2Btn() {
        log.info("delete 2 validator");
        showDialogConfirmDeleteNode(settings.getNode2());
    }

    public void deleteValidator3Btn() {
        log.info("delete 3 validator");
        showDialogConfirmDeleteNode(settings.getNode3());
    }

    public void deleteValidator4Btn() {
        log.info("delete 4 validator");
        showDialogConfirmDeleteNode(settings.getNode4());
    }

    public void deleteValidator5Btn() {
        log.info("delete 5 validator");
        showDialogConfirmDeleteNode(settings.getNode5());
    }

    public void deleteValidator6Btn() {
        log.info("delete 6 validator");
        showDialogConfirmDeleteNode(settings.getNode6());
    }

    public void deleteValidator7Btn() {
        log.info("delete 7 validator");
        showDialogConfirmDeleteNode(settings.getNode7());
    }

    public void valLogDirBtnAction2() throws IOException {
        log.info("open validator log dir {}", validatorLogDir2.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start " + validatorLogDir2.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + validatorLogDir2.getFieldText());
        }
    }

    public void valLogDirBtnAction3() throws IOException {
        log.debug("open validator log dir {}", validatorLogDir3.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start " + validatorLogDir3.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + validatorLogDir3.getFieldText());
        }
    }

    public void valLogDirBtnAction4() throws IOException {
        log.debug("open validator log dir {}", validatorLogDir4.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start " + validatorLogDir4.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + validatorLogDir4.getFieldText());
        }
    }

    public void valLogDirBtnAction5() throws IOException {
        log.debug("open validator log dir {}", validatorLogDir5.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start " + validatorLogDir5.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + validatorLogDir5.getFieldText());
        }
    }

    public void valLogDirBtnAction6() throws IOException {
        log.debug("open validator log dir {}", validatorLogDir6.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start " + validatorLogDir6.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + validatorLogDir6.getFieldText());
        }
    }

    public void valLogDirBtnAction7() throws IOException {
        log.debug("open validator log dir {}", validatorLogDir7.getFieldText().trim());
        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("cmd /c start " + validatorLogDir7.getFieldText());
        } else {
            Runtime.getRuntime().exec("gio open " + validatorLogDir7.getFieldText());
        }
    }

    @FXML
    public void openTelegramLink() {
        hostServices.showDocument(TELEGRAM_NEODIX);
    }

//    public void copyDonationTonAddress(MouseEvent mouseEvent) {
//        String addr = tonDonationAddress.getText();
//        final Clipboard clipboard = Clipboard.getSystemClipboard();
//        final ClipboardContent content = new ClipboardContent();
//        content.putString(addr);
//        clipboard.setContent(content);
//        log.debug(addr + " copied");
//        App.mainController.showInfoMsg(addr + " copied to clipboard", 1);
//        mouseEvent.consume();
//    }

    public void BlockChainExplorerCheckBoxClick(MouseEvent mouseEvent) {
        if (enableBlockchainExplorer.isSelected()) {
            App.mainController.showInfoMsg("Native blockchain-explorer will be available on start", 5);
        }
    }

    public void TonHttpApiCheckBoxClick(MouseEvent mouseEvent) {
        log.debug("TonHttpApiCheckBoxClick {}", enableTonHttpApi.isSelected());
        if (enableTonHttpApi.isSelected()) {
            if (!python3Installed()) {
                log.info("python3 is not installed");
                enableTonHttpApi.setSelected(false);
                showDialogInstallPython();
                return;
            } else if (!pipInstalled()) {
                log.info("pip is not installed");
                enableTonHttpApi.setSelected(false);
                showDialogInstallPip();
                return;
            } else if (!tonHttpApiInstalled()) {
                log.info("ton-http-api is not installed");
                enableTonHttpApi.setSelected(false);
                showDialogInstallTonHttpApi();
                return;
            }
            App.mainController.showInfoMsg("ton-http-api will be available on start", 6);
        }
    }

    private boolean python3Installed() {
        try {
            Process p = Runtime.getRuntime().exec((SystemUtils.IS_OS_WINDOWS ? "python" : "python3") + " --version");
            p.waitFor(5, TimeUnit.SECONDS);
            if (SystemUtils.IS_OS_MAC) {
                String pythonVersion = IOUtils.toString(p.getInputStream(), Charset.defaultCharset()).strip();
                ComparableVersion v = new ComparableVersion(StringUtils.split(pythonVersion, " ")[1]);
                if (v.compareTo(new ComparableVersion("3.11")) < 0) {
                    return false;
                }
            }

            return (p.exitValue() == 0);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean pipInstalled() {
        try {
            Process p = Runtime.getRuntime().exec("pip3 --version");
            p.waitFor(6, TimeUnit.SECONDS);
            return (p.exitValue() == 0);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tonHttpApiInstalled() {
        try {
            String cmd;
            if (SystemUtils.IS_OS_WINDOWS) {
                cmd = "ton-http-api --version";
            } else if (SystemUtils.IS_OS_LINUX) {
                cmd = System.getenv("HOME") + "/.local/bin/ton-http-api --version";
            } else if (SystemUtils.IS_OS_MAC) {
                String locationCmd = "python3 -m site --user-base";
                Process p = Runtime.getRuntime().exec(locationCmd);
                String pythonLocation = IOUtils.toString(p.getInputStream(), Charset.defaultCharset()).strip();
                Optional<Path> hit = Files.walk(Path.of(pythonLocation).getParent())
                        .filter(file -> file.getFileName().endsWith("ton-http-api"))
                        .findAny();
                cmd = hit.get() + " --version";
            } else {
                log.error("unsupported OS");
                return false;
            }
            log.debug(cmd);
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor(6, TimeUnit.SECONDS);
            return (p.exitValue() == 0);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * New Methods
     */

    public void showLoadingPane(String line1, String line2) {
        FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/layout/loading-pane.fxml"));
        Parent parent = null;
        try {
            parent = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        CustomLoadingPaneController controller = loader.getController();
        controller.setLines(line1, line2);

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(parent);

        loadingDialog = new JFXDialog(superWindow, content, JFXDialog.DialogTransition.CENTER);
        loadingDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        loadingDialog.close();
                        loadingDialog = null;
                    }
                }
        );
        loadingDialog.setOnDialogClosed(e -> loadingDialog = null);

        loadingDialog.show();
    }

    public void removeLoadingPane() {
        Platform.runLater(() -> emit(new CustomActionEvent(CustomEvent.Type.START)));
        loadingDialog.close();
        loadingDialog = null;
    }

    public void handle(CustomEvent event) {
        switch (event.getEventType()) {
            case CLICK:
                try {
                    createNewAccountBtn();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                break;
            case DIALOG_SEND_CLOSE:
                sendDialog.close();
                sendDialog = null;
                break;
            case DIALOG_YES_NO_CLOSE:
                yesNoDialog.close();
                yesNoDialog = null;
                break;
            case DIALOG_CREATE_CLOSE:
                createDialog.close();
                createDialog = null;
                break;
            case SAVE_SETTINGS:
                saveSettings();
                break;
            case SEARCH_CLEAR:
                clearSearch();
                break;
        }
    }

    private void clearSearch() {
        foundBlockslistviewid.getItems().clear();
        foundTxsvboxid.getItems().clear();
        foundAccountsvboxid.getItems().clear();
        Platform.runLater(() -> {
            emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_BLOCKS, 0));
            emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_TXS, 0));
            emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_SIZE_ACCOUNTS, 0));
        });
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @FXML
    public void closeSearch(Event e) {
        clearSearch();
        foundAccountsTxsvboxid.getItems().clear();
        Platform.runLater(() -> {
            emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_REMOVE));
            emit(new CustomSearchEvent(CustomEvent.Type.ACCOUNTS_TXS_REMOVE));
        });
    }

    @FXML
    public void closeAccountsTxs(Event e) {
        foundAccountsTxsvboxid.getItems().clear();
        Platform.runLater(() -> emit(new CustomSearchEvent(CustomEvent.Type.ACCOUNTS_TXS_REMOVE)));
    }

    public void closeWindow(ActionEvent actionEvent) {
        App.primaryStage.fireEvent(new WindowEvent(
                App.primaryStage,
                WindowEvent.WINDOW_CLOSE_REQUEST
        ));
    }
}

