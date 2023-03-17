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
import org.ton.enums.LiteClientEnum;
import org.ton.executors.liteclient.LiteClient;

import java.net.URL;
import java.util.ResourceBundle;

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
    JFXTextField subWalletId;

    @FXML
    JFXTextField workchain;

    @FXML
    JFXTextField seqno;

    @FXML
    JFXTextArea txtArea;

    @FXML
    VBox inputFields;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        workchain.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\.\\-]+")) {
                workchain.setText(newValue.replaceAll("[^\\d\\.\\-]", ""));
            }
        });

        subWalletId.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\.]+")) {
                subWalletId.setText(newValue.replaceAll("[^\\d\\.]", ""));
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
            case "transform":
                log.debug("do transform");
                mainController.yesNoDialog.close();
                break;
            default:
                log.debug("no action");
                mainController.yesNoDialog.close();
        }
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

}
