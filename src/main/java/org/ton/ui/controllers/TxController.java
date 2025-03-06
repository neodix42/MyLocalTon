package org.ton.ui.controllers;

import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.actions.DynamicTreeLayout;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.TxPk;
import org.ton.executors.liteclient.api.BlockShortSeqno;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.main.App;
import org.ton.utils.MyLocalTonUtils;

@Slf4j
public class TxController {

  @FXML private Label block;
  @FXML private Label txid;
  @FXML private Label txidHidden;
  @FXML private Label typeMsg;
  @FXML private Label typeTx;
  @FXML private Label time;
  @FXML private Label from;
  @FXML private Label to;
  @FXML private Label amount;
  @FXML private Label fees;
  @FXML private Label status;
  @FXML private BorderPane txRowBorderPane;
  @FXML private Label txAccAddrHidden;
  @FXML private Label txLt;

  @FXML
  void txTraceBtn() {
    String shortseqno = block.getText();

    BlockShortSeqno blockShortSeqno = BlockShortSeqno.builder()
            .wc(Long.valueOf(StringUtils.substringBetween(shortseqno, "(", ",")))
            .shard(StringUtils.substringBetween(shortseqno, ",", ","))
            .seqno(new BigInteger(StringUtils.substring(StringUtils.substringAfterLast(shortseqno, ","), 0, -1)))
            .build();

    TxPk txPk = TxPk.builder()
            .createdAt(MyLocalTonUtils.datetimeToTimestamp(time.getText()))
            .seqno(blockShortSeqno.getSeqno())
            .wc(blockShortSeqno.getWc())
            .shard(blockShortSeqno.getShard())
            .accountAddress(txAccAddrHidden.getText())
            .txLt(new BigInteger(txLt.getText()))
            .txHash(txidHidden.getText())
            .typeTx(typeTx.getText())
            .typeMsg(typeMsg.getText())
            .build();

    log.debug("txTraceBtn,  block {}, txPk {}, createdAt {}, seconds {}", blockShortSeqno, txPk, time.getText(), MyLocalTonUtils.datetimeToTimestamp(time.getText()));

    CompletableFuture.supplyAsync(
            () -> App.dbPool.findTx(txPk), ForkJoinPool.commonPool()
    ).thenAccept(txEntity -> {
      if (txEntity == null) {
        Platform.runLater(() -> App.mainController.showErrorMsg("Transaction not found", 3));
        return;
      }
      Platform.runLater(() -> {
        try {
          new DynamicTreeLayout().showTree(txEntity, txEntity.getTx());
        } catch (Exception e) {
          log.error("Error showing tx dump", e);
          App.mainController.showErrorMsg("Error showing transaction dump", 3);
        }
      });
    });

  }

  @FXML
  void txInfoBtn() {

    String shortSeqno = block.getText();
    String inside = StringUtils.substringBetween(shortSeqno, "(", ")");
    if (inside == null) {
      log.error("Wrong block format: {}", shortSeqno);
      return;
    }
    String[] parts = inside.split(",");
    if (parts.length < 3) {
      log.error("Wrong block format, parts < 3: {}", shortSeqno);
      return;
    }

    long wcVal = Long.parseLong(parts[0].trim());
    String shardVal = parts[1].trim();
    BigInteger seqnoVal = new BigInteger(parts[2].trim());

    BlockShortSeqno blockShortSeqno = BlockShortSeqno.builder()
        .wc(wcVal)
        .shard(shardVal)
        .seqno(seqnoVal)
        .build();

    long createdAtSeconds = MyLocalTonUtils.datetimeToTimestamp(time.getText());

    TxPk txPk = TxPk.builder()
        .createdAt(createdAtSeconds)
        .seqno(blockShortSeqno.getSeqno())
        .wc(blockShortSeqno.getWc())
        .shard(blockShortSeqno.getShard())
        .accountAddress(txAccAddrHidden.getText())
        .txLt(new BigInteger(txLt.getText()))
        .txHash(txidHidden.getText())
        .typeTx(typeTx.getText())
        .typeMsg(typeMsg.getText())
        .build();

    log.debug("tx infobtn, block {}, txPk {}, createdAt {}, seconds {}", blockShortSeqno, txPk,
        time.getText(), createdAtSeconds);

    String msg = String.format("Loading block info with seqno %s", seqnoVal);
    App.mainController.showInfoMsg(msg, 5);

    CompletableFuture.supplyAsync(
        () -> App.dbPool.findTx(txPk), ForkJoinPool.commonPool()
    ).thenAccept(txEntity -> {
      if (txEntity == null) {
        Platform.runLater(() -> App.mainController.showErrorMsg("Transaction not found", 3));
        return;
      }
      Platform.runLater(() -> {
        try {
          if (txEntity.getTypeTx().equals("Message")) {
            showMsgDetails(txEntity);
          } else {
            showTxDetailsView(txEntity);
          }
        } catch (IOException e) {
          log.error("Error showing tx dump", e);
          App.mainController.showErrorMsg("Error showing transaction dump", 3);
        }
      });
    });
  }

  private void showMsgDetails(TxEntity txEntity) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/ton/main/msginfoview.fxml"));
    Parent root = loader.load();

    MsgInfoController controller = loader.getController();

    String msgJson = new GsonBuilder().setPrettyPrinting().create().toJson(txEntity.getMessage());

    controller.initData(msgJson, getRawDumpContent(txEntity, txEntity.getTx()));

    generateStage(root, txEntity, 700, 850);
  }


  private void showTxDetailsView(TxEntity txEntity) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/ton/main/txinfoview.fxml"));
    Parent root = loader.load();

    TxInfoController controller = loader.getController();

    String txJson = new GsonBuilder().setPrettyPrinting().create().toJson(txEntity.getTx());

    controller.initData(txJson, getRawDumpContent(txEntity, txEntity.getTx()));

    generateStage(root, txEntity, 800, 850);
  }

  private Parent getRawDumpContent(TxEntity txEntity, Transaction tx) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(
        TxController.class
            .getClassLoader()
            .getResource("org/ton/main/rawdump.fxml")
    );

    Parent root = fxmlLoader.load();
    JFXButton btn = (JFXButton) root.lookup("#showDumpBtn");
    btn.setUserData("tx#" + txEntity.getFullBlock() + " " + txEntity.getWc() + ":" + tx.getAccountAddr() + " " + tx.getLt());

    return root;
  }

  private void generateStage(Parent root, TxEntity txEntity, int width, int height) {
    Stage stage = new Stage();
    stage.setTitle("Tx hash " + txEntity.getTxHash());
    stage.initModality(Modality.NONE);
    stage.initStyle(StageStyle.DECORATED);

    try {
      Image icon = new Image(Objects.requireNonNull(
          getClass().getClassLoader().getResourceAsStream("org/ton/images/logo.png")
      ));
      stage.getIcons().add(icon);
    } catch (NullPointerException e) {
      log.error("Icon not found. Exception thrown {}", e.getMessage(), e);
    }

    Scene scene = new Scene(root, width, height);
    scene.getStylesheets().add(
        TxController.class
            .getClassLoader()
            .getResource("org/ton/css/global-font.css")
            .toExternalForm()
    );

    scene.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
        stage.close();
      }
    });

    stage.setScene(scene);
    stage.setResizable(false);
    stage.show();
  }

  public void txRowSeqnoClick(MouseEvent mouseEvent) {
    copyToClipboard(block.getText(), "Block seqno");
    mouseEvent.consume();
  }

  public void txRowHashClick(MouseEvent mouseEvent) {
    String hash = txidHidden.getText();
    copyToClipboard(hash, "Tx hash");
    mouseEvent.consume();
  }

  public void txRowSrcAddrClick(MouseEvent mouseEvent) {
    copyToClipboard(from.getText(), "From address");
    mouseEvent.consume();
  }

  public void txRowDestAddrClick(MouseEvent mouseEvent) {
    copyToClipboard(to.getText(), "To address");
    mouseEvent.consume();
  }

  public void txRowAmountClick(MouseEvent mouseEvent) {
    copyToClipboard(amount.getText(), "Amount");
    mouseEvent.consume();
  }

  private void copyToClipboard(String text, String desc) {
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(text);
    clipboard.setContent(content);
    log.info("{} {} copied", desc, text);
    App.mainController.showInfoMsg(text + " copied to clipboard", 2);
  }
}
