package org.ton.mylocalton.ui.controllers;

import static org.ton.mylocalton.actions.MyLocalTon.adnlLiteClient;
import static org.ton.mylocalton.utils.MyLocalTonUtils.PATTERN;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXButton;
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
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.ton.ton4j.tl.liteserver.responses.BlockId;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.db.entities.BlockEntity;
import org.ton.mylocalton.db.entities.BlockPk;
import org.ton.mylocalton.enums.LiteClientEnum;
import org.ton.mylocalton.executors.liteclient.LiteClient;
import org.ton.mylocalton.executors.liteclient.LiteClientParser;
import org.ton.mylocalton.executors.liteclient.api.ResultLastBlock;
import org.ton.mylocalton.executors.liteclient.api.block.Block;
import org.ton.mylocalton.main.App;
import org.ton.mylocalton.settings.Node;
import org.ton.mylocalton.utils.MyLocalTonUtils;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;

@Slf4j
public class BlockController {

  @FXML private Label seqno;
  @FXML private Label wc;
  @FXML private Label shard;
  @FXML private Label createdatDate;
  @FXML private Label createdatTime;
  @FXML private Label filehash;
  @FXML private BorderPane blockRowBorderPane;
  @FXML private Label roothash;

  @FXML
  void blockInfoBtn() {
    long createdAt =
        MyLocalTonUtils.datetimeToTimestamp(
            createdatDate.getText() + " " + createdatTime.getText());
    BlockPk blockPk =
        BlockPk.builder()
            .wc(Long.parseLong(wc.getText()))
            .shard(shard.getText())
            .seqno(new BigInteger(seqno.getText()))
            .createdAt(createdAt)
            .build();

    log.debug("loading block info {}", blockPk);
    String msg = String.format("Loading block info with seqno %s", seqno.getText());
    App.mainController.showInfoMsg(msg, 5);

    CompletableFuture.supplyAsync(
            () -> {
              try {
                Node node = MyLocalTon.getInstance().getSettings().getGenesisNode();
                // Block request and DB update
                Block block = getBlockFromServerAndUpdateDb(node, blockPk);
                BlockEntity blockEntity = App.dbPool.findBlock(blockPk);
                return new Pair<>(blockEntity, block);
              } catch (Exception e) {
                Thread.currentThread().interrupt();
                log.error("Failed to load block info", e);
                return null;
              }
            },
            ForkJoinPool.commonPool())
        .thenAccept(
            pair -> {
              // After finishing the background work, we update the UI
              Platform.runLater(
                  () -> {
                    if (pair == null) {
                      App.mainController.showErrorMsg("Failed to load block info", 5);
                    } else {
                      try {
                        showBlockDump(pair.getKey(), pair.getValue());
                      } catch (IOException e) {
                        log.error("Failed to show block dump", e);
                        App.mainController.showErrorMsg("Failed to show block dump", 5);
                      }
                    }
                  });
            });
  }

  private Block getBlockFromServerAndUpdateDb(Node node, BlockPk blockPk) throws Exception {
    // remove
    BlockId blockId =
        BlockId.builder()
            .seqno(Integer.parseInt(seqno.getText()))
            .workchain(Integer.parseInt(wc.getText()))
            .shard(new BigInteger(shard.getText(), 16).longValue())
            .build();
    BlockIdExt blockIdExt = adnlLiteClient.lookupBlock(blockId, 1, 0, 0).getId();
    ResultLastBlock lightBlock = MyLocalTonUtils.getLast(blockIdExt);

    LiteClient liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);

    return LiteClientParser.parseDumpblock(
        liteClient.executeDumpblock(node, lightBlock),
        MyLocalTon.getInstance().getSettings().getUiSettings().isShowShardStateInBlockDump(),
        MyLocalTon.getInstance().getSettings().getUiSettings().isShowBodyInMessage());
  }

  private void showBlockDump(BlockEntity blockEntity, Block block) throws IOException {
    Stage stage = new Stage();
    stage.initModality(Modality.NONE);
    stage.initStyle(StageStyle.DECORATED);
    stage.setTitle("Block " + blockEntity.getSeqno());

    try {
      Image icon =
          new Image(
              Objects.requireNonNull(
                  getClass()
                      .getClassLoader()
                      .getResourceAsStream("org/ton/mylocalton/images/logo.png")));
      stage.getIcons().add(icon);
    } catch (NullPointerException e) {
      log.error("Icon not found. Exception thrown {}", e.getMessage(), e);
    }

    FXMLLoader fxmlLoader =
        new FXMLLoader(
            BlockController.class
                .getClassLoader()
                .getResource("org/ton/mylocalton/main/rawdump.fxml"));
    Parent root = fxmlLoader.load();

    Scene scene = new Scene(root, 1000, 700);
    scene.setOnKeyPressed(
        keyEvent -> {
          if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
            stage.close();
          }
        });

    scene
        .getStylesheets()
        .add(
            TxController.class
                .getClassLoader()
                .getResource("org/ton/mylocalton/css/java-keywords.css")
                .toExternalForm());

    stage.setScene(scene);
    stage.show();

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(block);

    CodeArea codeArea = new CodeArea();
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea)); // нумерация строк
    codeArea.setEditable(false);

    codeArea
        .getVisibleParagraphs()
        .addModificationObserver(
            new MyLocalTonUtils.VisibleParagraphStyler<>(codeArea, this::computeHighlighting));

    codeArea.replaceText(0, 0, json);

    VirtualizedScrollPane<CodeArea> vsPane = new VirtualizedScrollPane<>(codeArea);

    VBox vbox = (VBox) root.lookup("#vboxid");
    vbox.getChildren().add(vsPane);
    VBox.setVgrow(vsPane, Priority.ALWAYS);
    vbox.setAlignment(Pos.CENTER);
    vbox.setFillWidth(true);

    JFXButton btn = (JFXButton) root.lookup("#showDumpBtn");
    btn.setUserData(
        "block#"
            + MyLocalTonUtils.constructFullBlockSeq(
                blockEntity.getWc(),
                blockEntity.getShard(),
                blockEntity.getSeqno(),
                blockEntity.getRoothash(),
                blockEntity.getFilehash()));
  }

  public StyleSpans<Collection<String>> computeHighlighting(String text) {
    Matcher matcher = PATTERN.matcher(text);
    int lastKwEnd = 0;
    StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
    while (matcher.find()) {
      String styleClass =
          matcher.group("KEYWORD") != null
              ? "keyword"
              : matcher.group("PAREN") != null
                  ? "paren"
                  : matcher.group("BRACE") != null
                      ? "brace"
                      : matcher.group("BRACKET") != null
                          ? "bracket"
                          : matcher.group("SEMICOLON") != null
                              ? "semicolon"
                              : matcher.group("STRING") != null
                                  ? "string"
                                  : matcher.group("COMMENT") != null
                                      ? "comment"
                                      : null; /* never happens */
      assert styleClass != null;
      spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
      spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
      lastKwEnd = matcher.end();
    }
    spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
    return spansBuilder.create();
  }

  @FXML
  public void blockRowSeqnoClick(MouseEvent mouseEvent) {
    String seq = seqno.getText();
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(seq);
    clipboard.setContent(content);
    log.info(seq + " copied");
    App.mainController.showInfoMsg(seq + " copied to clipboard", 2);
    mouseEvent.consume();
  }

  @FXML
  void blockRowClick(MouseEvent event) {
    String shortBlock = "(" + wc.getText() + "," + shard.getText() + "," + seqno.getText() + ")";
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(shortBlock);
    clipboard.setContent(content);
    log.info("{} copied", shortBlock);
    App.mainController.showInfoMsg(shortBlock + " copied to clipboard", 2);
  }

  @FXML
  public void blockRowHashClick(MouseEvent mouseEvent) {
    String fullBlock =
        "("
            + wc.getText()
            + ","
            + shard.getText()
            + ","
            + seqno.getText()
            + "):"
            + roothash.getText()
            + ":"
            + filehash.getText();
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(fullBlock);
    clipboard.setContent(content);
    log.info("{} copied", fullBlock);
    String lightfullBlock =
        MyLocalTonUtils.getLightAddress(roothash.getText() + ":" + filehash.getText());
    App.mainController.showInfoMsg(lightfullBlock + " copied to clipboard", 2);
    mouseEvent.consume();
  }
}
