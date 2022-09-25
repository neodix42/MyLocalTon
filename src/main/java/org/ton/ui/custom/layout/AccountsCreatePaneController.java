package org.ton.ui.custom.layout;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import org.apache.commons.lang3.StringUtils;
import org.ton.actions.MyLocalTon;
import org.ton.db.entities.WalletEntity;
import org.ton.ui.custom.control.CustomTextField;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomNotificationEvent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.nonNull;
import static org.ton.ui.custom.events.CustomEventBus.emit;

public class AccountsCreatePaneController implements Initializable {


    @FXML
    private CustomTextField workchain, subWalletID, walletVersion;

    @FXML
    private JFXButton createBtn;

    @FXML
    private AnchorPane anchorPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @FXML
    private void doCreateAccount() {
        emit(new CustomNotificationEvent(CustomEvent.Type.INFO, "Creating new wallet...", 3));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Thread.currentThread().setName("Create new wallet");
            try {
                long chain = Long.parseLong(StringUtils.isEmpty(workchain.getFieldText()) ? String.valueOf(MyLocalTon.getInstance().getSettings().getWalletSettings().getDefaultWorkChain()) : workchain.getFieldText());
                long walletId = Long.parseLong(StringUtils.isEmpty(subWalletID.getFieldText()) ? String.valueOf(MyLocalTon.getInstance().getSettings().getWalletSettings().getDefaultSubWalletId()) : subWalletID.getFieldText());

                WalletEntity walletEntity = MyLocalTon.getInstance().createWalletEntity(
                        MyLocalTon.getInstance().getSettings().getGenesisNode(),
                        null,
                        chain,
                        walletId,
                        MyLocalTon.getInstance().getSettings().getWalletSettings().getInitialAmount(),
                        false);

                if (nonNull(walletEntity) && walletEntity.getSeqno() != -1L) {
                    emit(new CustomNotificationEvent(CustomEvent.Type.SUCCESS, "Wallet " + walletEntity.getFullAddress() + " created", 3));
                } else {
                    emit(new CustomNotificationEvent(CustomEvent.Type.ERROR,"Error creating wallet " + walletEntity.getFullAddress() + ". See logs for details.", 4));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setWalletVersionText(String walletVersion) {
        this.walletVersion.setFieldText(walletVersion);
    }

    public void hideSubWalletID() {
        subWalletID.setVisible(false);
        walletVersion.setLayoutY(140.0);
        createBtn.setLayoutY(210.0);
        anchorPane.setPrefHeight(280.0);
    }
}
