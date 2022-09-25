package org.ton.ui.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.actions.MyLocalTon;
import org.ton.db.entities.WalletEntity;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.liteclient.LiteClient;
import org.ton.main.App;
import org.ton.ui.custom.control.CustomTextField;
import org.ton.utils.Utils;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.nonNull;
import static org.ton.main.App.fxmlLoader;

@Slf4j
public class YesNoController implements Initializable {

    @FXML
    Label header;

    @FXML
    Label body;

    @FXML
    JFXButton okBtn;

    @FXML
    Label action;

    @FXML
    Label address;

    @FXML
    CustomTextField subWalletId;

    @FXML
    CustomTextField workchain;

    @FXML
    JFXTextField seqno;

    @FXML
    JFXTextArea txtArea;

    @FXML
    VBox inputFields;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        workchain.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\.\\-]+")) {
                workchain.setFieldText(newValue.replaceAll("[^\\d\\.\\-]", ""));
            }
        });

        subWalletId.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\.]+")) {
                subWalletId.setFieldText(newValue.replaceAll("[^\\d\\.]", ""));
            }
        });

        seqno.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                doRunMethod();
            }
        });
    }

    public void okBtnAction() throws InterruptedException {
        log.debug("ok clicked, action {}", action.getText());
        MainController mainController = fxmlLoader.getController();

        switch (action.getText()) {
//            case "reset":
//                log.debug("do reset");
//                mainController.yesNoDialog.close();
//                //show msg
//                doReset();
//                break;
            case "transform":
                log.debug("do transform");
                mainController.yesNoDialog.close();
                break;
//            case "create":
//                log.debug("create");
//                mainController.yesNoDialog.close();
//                doCreateAccount();
//                break;
            case "runmethod":
                log.debug("runmethod");
                doRunMethod();
                break;
            case "showmsg":
                log.debug("showmsg");
                mainController.yesNoDialog.close();
                break;
            case "delnode":
                log.debug("delnode");
                mainController.yesNoDialog.close();
                doDelete();
                break;
            default:
                log.debug("no action");
                mainController.yesNoDialog.close();
        }
    }

    private void doDelete() throws InterruptedException {
        String nodeName = address.getText();
        log.debug("do delete {}", nodeName);

        Utils.doDelete(nodeName);
    }

    private void doRunMethod() {
        String smcAddress = address.getText();
        String methodId = seqno.getText();
        if (StringUtils.isEmpty(methodId)) {
            return;
        }
        String stdout = "";
        String parameters = "";
        if (methodId.contains(" ")) {
            parameters = methodId.substring(methodId.indexOf(" "));
        }

        txtArea.setVisible(true);

        log.info("run method {} against {} with parameters {}", methodId, smcAddress, parameters);
        try {
            stdout = LiteClient.getInstance(LiteClientEnum.GLOBAL).executeRunMethod(MyLocalTon.getInstance().getSettings().getGenesisNode(), smcAddress, methodId, parameters);
            if (stdout.contains("arguments")) {
                stdout = stdout.substring(stdout.indexOf("arguments")).trim();
            }
        } catch (Exception e) {
            stdout = e.getMessage();
        }
        txtArea.setText(stdout);
        txtArea.setVisible(true);
    }

//    private void doCreateAccount() {
//        App.mainController.showInfoMsg("Creating new wallet...", 3);
//
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        executorService.submit(() -> {
//            Thread.currentThread().setName("Create new wallet");
//            try {
//                long chain = Long.parseLong(StringUtils.isEmpty(workchain.getFieldText()) ? String.valueOf(MyLocalTon.getInstance().getSettings().getWalletSettings().getDefaultWorkChain()) : workchain.getFieldText());
//                long walletId = Long.parseLong(StringUtils.isEmpty(subWalletId.getFieldText()) ? String.valueOf(MyLocalTon.getInstance().getSettings().getWalletSettings().getDefaultSubWalletId()) : subWalletId.getFieldText());
//
//                WalletEntity walletEntity = MyLocalTon.getInstance().createWalletEntity(
//                        MyLocalTon.getInstance().getSettings().getGenesisNode(),
//                        null,
//                        chain,
//                        walletId,
//                        MyLocalTon.getInstance().getSettings().getWalletSettings().getInitialAmount(),
//                        false);
//
//                if (nonNull(walletEntity) && walletEntity.getSeqno() != -1L) {
//                    App.mainController.showSuccessMsg("Wallet " + walletEntity.getFullAddress() + " created", 3);
//                } else {
//                    App.mainController.showErrorMsg("Error creating wallet " + walletEntity.getFullAddress() + ". See logs for details.", 4);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }

//    private void doReset() {
//        try {
//            MainController mainController = fxmlLoader.getController();
//            mainController.saveSettings();
//            Thread.sleep(100);
//            if (isWindows()) {
//                if (Utils.doShutdown()) {
//                    log.info("restarting: cmd /c start java -jar {} restart", Utils.getMyPath());
//                    Runtime.getRuntime().exec("cmd /c start java -jar " + Utils.getMyPath() + " restart");
//                    System.exit(0);
//                }
//            } else {
//                if (Utils.doShutdown()) {
//                    // works on linux
//                    log.info("restarting: java -jar {}", Utils.getMyPath());
//                    Runtime.getRuntime().exec("java -jar " + Utils.getMyPath() + " restart");
//                    System.exit(0);
//                }
//            }
//        } catch (Exception e) {
//            log.error("Cannot restart MyLocalTon, error " + e.getMessage());
//        }
//    }
}
