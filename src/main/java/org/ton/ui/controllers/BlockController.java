package org.ton.ui.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXButton;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.ton.actions.MyLocalTon;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.BlockPk;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.executors.liteclient.api.block.Block;
import org.ton.main.App;
import org.ton.settings.Node;
import org.ton.utils.MyLocalTonUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;

import static org.ton.utils.MyLocalTonUtils.PATTERN;

@Slf4j
public class BlockController {

    @FXML
    public Label seqno;

    @FXML
    public Label wc;

    @FXML
    public Label shard;

    @FXML
    public Label createdatDate;

    @FXML
    public Label createdatTime;
    @FXML
    public Label filehash;

    @FXML
    public BorderPane blockRowBorderPane;

    @FXML
    public Label roothash;


    @FXML
    void blockInfoBtn() throws Exception {

        long createdAt = MyLocalTonUtils.datetimeToTimestamp(createdatDate.getText() + " " + createdatTime.getText());
        log.debug("click seqno {}, createdAt {}, formatted {}", seqno.getText(), createdAt, MyLocalTonUtils.toUtcNoSpace(createdAt));

        Node node = MyLocalTon.getInstance().getSettings().getGenesisNode();

        BlockPk blockPk = BlockPk.builder()
                .wc(Long.parseLong(wc.getText()))
                .shard(shard.getText())
                .seqno(new BigInteger(seqno.getText()))
                .createdAt(createdAt)
                .build();

        BlockEntity blockEntity = App.dbPool.findBlock(blockPk);
        Block block = getBlockFromServerAndUpdateDb(node, blockPk);
        /*
        Block block = blockEntity.getBlock();

        if (Objects.isNull(block)) {
            log.debug("get block dump from server");
            block = getBlockFromServerAndUpdateDb(liteClient, node, blockPk);
        }
        */
        showBlockDump(blockEntity, block);
    }

    private Block getBlockFromServerAndUpdateDb(Node node, BlockPk blockPk) throws Exception {
        LiteClient liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);
        ResultLastBlock lightBlock = LiteClientParser.parseBySeqno(liteClient.executeBySeqno(node,
                Long.parseLong(wc.getText()),
                shard.getText(),
                new BigInteger(seqno.getText())));

//        Block block = LiteClientParser.parseDumpblock(liteClient.executeDumpblock(node, lightBlock), MyLocalTon.getInstance().getSettings().getUiSettings().isShowShardStateInBlockDump(), MyLocalTon.getInstance().getSettings().getUiSettings().isShowBodyInMessage());
        // DB.updateBlockDump(blockPk, bloc
        return LiteClientParser.parseDumpblock(liteClient.executeDumpblock(node, lightBlock), MyLocalTon.getInstance().getSettings().getUiSettings().isShowShardStateInBlockDump(), MyLocalTon.getInstance().getSettings().getUiSettings().isShowBodyInMessage());
    }

    private void showBlockDump(BlockEntity blockEntity, Block block) throws IOException {
        if (!SystemUtils.IS_OS_MAC) {
            FXMLLoader fxmlLoader = new FXMLLoader(BlockController.class.getClassLoader().getResource("org/ton/main/rawdump.fxml"));
            Parent root = fxmlLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Block " + blockEntity.getSeqno());

            Scene scene = new Scene(root, 1000, 700);

            scene.setOnKeyPressed(keyEvent -> {
                        if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                            stage.close();
                        }
                    }
            );

            stage.setScene(scene);
            stage.show();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            RSyntaxTextArea textArea = new RSyntaxTextArea();
            textArea.setText(gson.toJson(block));
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            textArea.setCodeFoldingEnabled(true);
            textArea.setAntiAliasingEnabled(true);
            textArea.setEditable(false);

            RTextScrollPane sp = new RTextScrollPane(textArea);
            SwingNode sn = (SwingNode) root.lookup("#swingid");
            JFXButton btn = (JFXButton) root.lookup("#showDumpBtn");
            btn.setUserData("block#" + MyLocalTonUtils.constructFullBlockSeq(blockEntity.getWc(), blockEntity.getShard(), blockEntity.getSeqno(), blockEntity.getRoothash(), blockEntity.getFilehash()));
            sn.setContent(sp);
        } else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            CodeArea codeArea = new CodeArea();
            codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
            codeArea.setEditable(false);
            codeArea.getVisibleParagraphs().addModificationObserver
                    (
                            new MyLocalTonUtils.VisibleParagraphStyler<>(codeArea, this::computeHighlighting)
                    );

            codeArea.replaceText(0, 0, gson.toJson(block));

            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Block " + blockEntity.getSeqno());
            Scene scene = new Scene(new StackPane(new VirtualizedScrollPane<>(codeArea)), 1000, 700);
            scene.getStylesheets().add(TxController.class.getClassLoader().getResource("org/ton/css/java-keywords.css").toExternalForm());

            stage.setScene(scene);
            stage.show();
        }

    }

    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN") != null ? "paren" :
                                    matcher.group("BRACE") != null ? "brace" :
                                            matcher.group("BRACKET") != null ? "bracket" :
                                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                                            matcher.group("STRING") != null ? "string" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            null; /* never happens */
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
        String fullBlock = "(" + wc.getText() + "," + shard.getText() + "," + seqno.getText() + "):" + roothash.getText() + ":" + filehash.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(fullBlock);
        clipboard.setContent(content);
        log.info("{} copied", fullBlock);
        String lightfullBlock = MyLocalTonUtils.getLightAddress(roothash.getText() + ":" + filehash.getText());
        App.mainController.showInfoMsg(lightfullBlock + " copied to clipboard", 2);
        mouseEvent.consume();
    }
}