package org.ton.ui.custom.layout;

import com.jfoenix.controls.JFXTextArea;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.actions.MyLocalTon;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.liteclient.LiteClient;
import org.ton.ui.custom.control.CustomTextField;

import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class RunMethodPaneController implements Initializable {

    @FXML
    private JFXTextArea txtArea;
    @FXML
    private CustomTextField methodIdField;
    @FXML
    private Label header;

    private String address;

    private void doRunMethod() {
        String methodId = methodIdField.getFieldText();
        if (StringUtils.isEmpty(methodId)) {
            return;
        }
        String stdout = "";
        String parameters = "";
        if (methodId.contains(" ")) {
            parameters = methodId.substring(methodId.indexOf(" "));
        }

        txtArea.setVisible(true);

        log.info("run method {} against {} with parameters {}", methodId, address, parameters);
        try {
            stdout = LiteClient.getInstance(LiteClientEnum.GLOBAL).executeRunMethod(
                    MyLocalTon.getInstance().getSettings().getGenesisNode(), address, methodId, parameters);
            if (stdout.contains("arguments")) {
                stdout = stdout.substring(stdout.indexOf("arguments")).trim();
            }
        } catch (Exception e) {
            stdout = e.getMessage();
        }
        txtArea.setText(stdout);
        txtArea.setVisible(true);
    }

    public void setHeader(String header) {
        this.header.setText(header);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @FXML
    private void runMethod(ActionEvent actionEvent) {
        doRunMethod();
    }

    public void requestFocusToMethodId() {
        this.methodIdField.requestFocus();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        methodIdField.getTextField().setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                doRunMethod();
            }
        });
    }
}
