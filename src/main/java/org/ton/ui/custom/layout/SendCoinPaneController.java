package org.ton.ui.custom.layout;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.actions.MyLocalTon;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.java.utils.Utils;
import org.ton.main.App;
import org.ton.parameters.SendToncoinsParam;
import org.ton.ui.custom.control.CustomTextField;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.ui.custom.events.event.CustomNotificationEvent;
import org.ton.wallet.MyWallet;
import org.ton.wallet.WalletAddress;

import java.math.BigInteger;
import java.net.URL;
import java.util.ResourceBundle;

import static org.ton.ui.custom.events.CustomEventBus.emit;

@Slf4j
public class SendCoinPaneController implements Initializable {


    @FXML
    private CustomTextField destinationAddress, amount, message;

    @FXML
    private CheckBox bounceFlag;

    private String hiddenWalletAddr;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        amount.getTextField().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\.]+")) {
                amount.setFieldText(newValue.replaceAll("[^\\d\\.]", ""));
            }
        });

        amount.getTextField().setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                send();
            }
        });
    }

    @FXML
    private void sendAction() {
        send();
    }

    private void send() {

        String destAddress = destinationAddress.getFieldText();
        String strAmount = amount.getFieldText();
        log.debug("btn clicked, dest {}, amount {}, from wallet {}", destAddress, strAmount, hiddenWalletAddr);

        try {
            if ((destAddress.length() == 48) || (destAddress.length() > 65)) {

                String[] wcAddr = hiddenWalletAddr.split(":");
                WalletPk walletPk = WalletPk.builder()
                        .wc(Long.parseLong(wcAddr[0]))
                        .hexAddress(wcAddr[1])
                        .build();
                WalletEntity fromWallet = App.dbPool.findWallet(walletPk);
                WalletAddress fromWalletAddress = fromWallet.getWallet();

                BigInteger amount = Utils.toNano(strAmount);
//                BigDecimal amount = new BigDecimal(strAmount);
                log.info("Sending {} Toncoins from {} ({}) to {}", amount, fromWalletAddress.getFullWalletAddress(), fromWallet.getWalletVersion(), destAddress);

                SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                        .executionNode(MyLocalTon.getInstance().getSettings().getGenesisNode())
                        .fromWallet(fromWalletAddress)
                        .fromWalletVersion(fromWallet.getWalletVersion())
                        .fromSubWalletId(fromWallet.getWallet().getSubWalletId())
                        .destAddr(destAddress)
                        .amount(amount)
                        .forceBounce(bounceFlag.isSelected())
                        .comment(message.getFieldText())
                        .build();

                boolean sentOK = new MyWallet().sendTonCoins(sendToncoinsParam);

                emit(new CustomActionEvent(CustomEvent.Type.DIALOG_SEND_CLOSE));

                if (sentOK) {
                    emit(new CustomNotificationEvent(CustomEvent.Type.SUCCESS, String.format("Sent %s Toncoins to %s", strAmount, destAddress), 3));
                } else {
                    emit(new CustomNotificationEvent(CustomEvent.Type.ERROR, String.format("Failed to send %s Toncoins to %s", strAmount, destAddress), 3));
                }
            } else {
                log.error("Sending error, wrong address");
                emit(new CustomNotificationEvent(CustomEvent.Type.ERROR, "Wrong address length! Should be 48 or of format wc:addr", 5));
            }
        } catch (Exception e) {
            log.error("Sending error {}", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
            emit(new CustomNotificationEvent(CustomEvent.Type.ERROR, String.format("Error sending Toncoins %s", e.getMessage()), 5));
        }
    }

    public void requestFocusToDestinationAddress() {
        destinationAddress.getTextField().requestFocus();
    }

    public String getHiddenWalletAddr() {
        return hiddenWalletAddr;
    }

    public void setHiddenWalletAddr(String hiddenWalletAddr) {
        this.hiddenWalletAddr = hiddenWalletAddr;
    }
}
