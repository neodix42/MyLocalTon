package org.ton.mylocalton.ui.controllers;

import static org.ton.mylocalton.main.App.fxmlLoader;

import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.db.entities.WalletEntity;
import org.ton.mylocalton.db.entities.WalletPk;
import org.ton.mylocalton.main.App;
import org.ton.mylocalton.parameters.SendGramsParam;
import org.ton.mylocalton.wallet.MyWallet;
import org.ton.mylocalton.wallet.WalletAddress;

@Slf4j
public class SendController implements Initializable {
  @FXML JFXTextField destAddr;

  @FXML JFXTextField sendAmount;

  @FXML Label hiddenWalletAddr;

  @FXML JFXTextField comment;

  @FXML JFXToggleButton clearBounceFlag;

  @FXML JFXToggleButton forceBounceFlag;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    sendAmount
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (!newValue.matches("[\\d\\.]+")) {
                sendAmount.setText(newValue.replaceAll("[^\\d\\.]", ""));
              }
            });

    sendAmount.setOnKeyPressed(
        keyEvent -> {
          if (keyEvent.getCode() == KeyCode.ENTER) {
            send();
          }
        });
  }

  public void sendBtn() {
    send();
  }

  private void send() {
    String destAddress = destAddr.getText();
    log.debug(
        "btn clicked, dest {}, amount {}, from wallet {}",
        destAddress,
        sendAmount.getText(),
        hiddenWalletAddr.getText());

    try {
      if ((destAddress.length() == 48) || (destAddress.length() > 65)) {

        String[] wcAddr = hiddenWalletAddr.getText().split(":");
        WalletPk walletPk =
            WalletPk.builder().wc(Long.parseLong(wcAddr[0])).hexAddress(wcAddr[1]).build();
        WalletEntity fromWallet = App.dbPool.findWallet(walletPk);
        WalletAddress fromWalletAddress = fromWallet.getWallet();

        BigInteger amount =
            (new BigDecimal(sendAmount.getText()))
                .multiply(BigDecimal.valueOf(1_000_000_000))
                .toBigInteger();
        log.info(
            "Sending {} nanograms from {} ({}) to {}",
            amount,
            fromWalletAddress.getFullWalletAddress(),
            fromWallet.getWalletVersion(),
            destAddress);

        SendGramsParam sendGramsParam =
            SendGramsParam.builder()
                .executionNode(MyLocalTon.getInstance().getSettings().getGenesisNode())
                .workchain(fromWalletAddress.getWc())
                .fromWallet(fromWalletAddress)
                .fromWalletVersion(fromWallet.getWalletVersion())
                .fromSubWalletId(fromWallet.getWallet().getSubWalletId())
                .destAddr(destAddress)
                .amount(amount)
                //                        .clearBounce(clearBounceFlag.isSelected())
                .forceBounce(forceBounceFlag.isSelected())
                .comment(comment.getText())
                .build();

        boolean sentOK = new MyWallet().sendGrams(sendGramsParam);

        MainController c = fxmlLoader.getController();
        c.sendDialog.close();

        if (sentOK) {
          App.mainController.showSuccessMsg(
              String.format("Sent %s Grams to %s", sendAmount.getText(), destAddress), 3);
        } else {
          log.debug("Failed to send {} Grams to {}", sendAmount.getText(), destAddress);
          App.mainController.showErrorMsg(
              String.format("Failed to send %s Grams to %s", sendAmount.getText(), destAddress),
              3);
        }
      } else {
        log.error("Sending error, wrong address");
        App.mainController.showErrorMsg(
            "Wrong address length! Should be 48 or of format wc:addr", 5);
      }
    } catch (Exception e) {
      log.error("Sending error {}", e.getMessage());
      log.error(ExceptionUtils.getStackTrace(e));
      App.mainController.showErrorMsg(
          String.format("Error sending Grams %s", e.getMessage()), 5);
    }
  }
}
