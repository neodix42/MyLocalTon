package org.ton.ui.controllers;

import static org.ton.utils.MyLocalTonUtils.PATTERN;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import javafx.application.Platform;
import javafx.fxml.FXML;
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
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.TxPk;
import org.ton.executors.liteclient.api.BlockShortSeqno;
import org.ton.main.App;
import org.ton.utils.MyLocalTonUtils;

@Slf4j
public class TxController {

  @FXML
  Label block;

  @FXML
  Label txid;

  @FXML
  Label txidHidden;

  @FXML
  Label typeMsg;

  @FXML
  Label typeTx;

  @FXML
  Label time;

  @FXML
  Label from;

  @FXML
  Label to;

  @FXML
  Label amount;

  @FXML
  Label fees;

  @FXML
  Label status;

  @FXML
  BorderPane txRowBorderPane;

  @FXML
  Label txAccAddrHidden;

  @FXML
  Label txLt;

  @FXML
  void txInfoBtn() {

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
          showTxDump(txEntity, txEntity.getTx());
        } catch (IOException e) {
          log.error("Error showing tx dump", e);
          App.mainController.showErrorMsg("Error showing transaction dump", 3);
        }
      });
    });
  }

  private void showTxDump(TxEntity txEntity, Transaction tx) throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    String jsonToShow = txEntity.getTypeTx().equals("Message")
        ? gson.toJson(txEntity.getMessage())
        : gson.toJson(tx);

    CodeArea codeArea = new CodeArea();
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    codeArea.setEditable(false);

    codeArea.getVisibleParagraphs().addModificationObserver(
        new MyLocalTonUtils.VisibleParagraphStyler<>(codeArea, this::computeHighlighting)
    );

    codeArea.replaceText(0, 0, jsonToShow);

    Stage stage = new Stage();
    stage.initModality(Modality.NONE);
    stage.initStyle(StageStyle.DECORATED);
    stage.setTitle("Tx hash " + txEntity.getTxHash());

    try {
      Image icon = new Image(Objects.requireNonNull(
          getClass().getClassLoader().getResourceAsStream("org/ton/images/logo.png")
      ));
      stage.getIcons().add(icon);
    } catch (NullPointerException e) {
      log.error("Icon not found. Exception thrown {}", e.getMessage(), e);
    }

    Scene scene = new Scene(new StackPane(new VirtualizedScrollPane<>(codeArea)), 1000, 700);

    scene.getStylesheets().add(
        TxController.class
            .getClassLoader()
            .getResource("org/ton/css/java-keywords.css")
            .toExternalForm()
    );

    scene.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
        stage.close();
      }
    });

    stage.setScene(scene);
    stage.show();
  }


  public StyleSpans<Collection<String>> computeHighlighting(String text) {
    Matcher matcher = PATTERN.matcher(text);
    int lastKwEnd = 0;
    StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
    while (matcher.find()) {
      String styleClass =
          matcher.group("KEYWORD") != null ? "keyword" :
              matcher.group("PAREN") != null ? "paren" :
                  matcher.group("BRACE") != null ? "brace" :
                      matcher.group("BRACKET") != null ? "bracket" :
                          matcher.group("SEMICOLON") != null ? "semicolon" :
                              matcher.group("STRING") != null ? "string" :
                                  matcher.group("COMMENT") != null ? "comment" :
                                      null;
      assert styleClass != null;
      spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
      spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
      lastKwEnd = matcher.end();
    }
    spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
    return spansBuilder.create();
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
