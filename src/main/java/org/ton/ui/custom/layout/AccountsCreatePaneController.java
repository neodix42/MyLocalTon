package org.ton.ui.custom.layout;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import org.apache.commons.lang3.StringUtils;
import org.ton.actions.MyLocalTon;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.ui.custom.events.event.CustomNotificationEvent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.ton.ui.custom.events.CustomEventBus.emit;

public class AccountsCreatePaneController implements Initializable {

    @FXML
    private JFXTextField workchain, subWalletID;

    @FXML
    private JFXButton createBtn;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private JFXComboBox<String> newWalletVersion;

    EventHandler<KeyEvent> onlyDigits = keyEvent -> {
        if (!((TextField) keyEvent.getSource()).getText().matches("[\\d\\-]+")) {
            ((TextField) keyEvent.getSource()).setText(((TextField) keyEvent.getSource()).getText().replaceAll("[^\\d\\-]", ""));
        }
    };

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ObservableList<String> versions = FXCollections.observableArrayList(
                WalletVersion.V1R1.getValue(),
                WalletVersion.V1R2.getValue(),
                WalletVersion.V1R3.getValue(),
                WalletVersion.V2R1.getValue(),
                WalletVersion.V2R2.getValue(),
                WalletVersion.V3R1.getValue(),
                WalletVersion.V3R2.getValue(),
                WalletVersion.V4R2.getValue()
        );

        newWalletVersion.setItems(versions);

        newWalletVersion.valueProperty().addListener((value, oldValue, newValue) -> {
            if (newValue.contains("V3") || newValue.contains("V4")) {
                showSubWalletID();
            } else {
                hideSubWalletID();
            }
            newWalletVersion.setValue(newValue);
        });

        workchain.setOnKeyTyped(onlyDigits);
        subWalletID.setOnKeyTyped(onlyDigits);
        newWalletVersion.setValue(WalletVersion.V3R2.getValue()); // default
    }

    @FXML
    private void doCreateAccount() {
        emit(new CustomNotificationEvent(CustomEvent.Type.INFO, "Creating new wallet...", 3));
        emit(new CustomActionEvent(CustomEvent.Type.DIALOG_CREATE_CLOSE));

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Thread.currentThread().setName("Create new wallet");
            try {
                long chain = Long.parseLong(StringUtils.isEmpty(workchain.getText()) ? String.valueOf(MyLocalTon.getInstance().getSettings().getWalletSettings().getDefaultWorkChain()) : workchain.getText());
                long walletId = Long.parseLong(StringUtils.isEmpty(subWalletID.getText()) ? String.valueOf(MyLocalTon.getInstance().getSettings().getWalletSettings().getDefaultSubWalletId()) : subWalletID.getText());
                WalletVersion walletVersion = WalletVersion.getKeyByValue(newWalletVersion.getValue());

                MyLocalTon.getInstance().createWalletEntity(
                        MyLocalTon.getInstance().getSettings().getGenesisNode(),
                        null,
                        walletVersion,
                        chain,
                        walletId,
                        MyLocalTon.getInstance().getSettings().getWalletSettings().getInitialAmount(),
                        false);


            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void hideSubWalletID() {
        subWalletID.setEditable(false);
        subWalletID.setDisable(true);
    }

    public void showSubWalletID() {
        subWalletID.setEditable(true);
        subWalletID.setDisable(false);
    }
}
