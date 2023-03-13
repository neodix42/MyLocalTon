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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.TxPk;
import org.ton.executors.liteclient.api.BlockShortSeqno;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.main.App;
import org.ton.utils.MyLocalTonUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;

import static org.ton.utils.MyLocalTonUtils.PATTERN;

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
    void txInfoBtn() throws Exception {

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

        log.debug("tx infobtn,  block {}, txPk {}, createdAt {}, seconds {}", blockShortSeqno, txPk, time.getText(), MyLocalTonUtils.datetimeToTimestamp(time.getText()));

        TxEntity txEntity = App.dbPool.findTx(txPk);
        Transaction tx = txEntity.getTx();

        showTxDump(txEntity, tx);
    }

    private void showTxDump(TxEntity txEntity, Transaction tx) throws IOException {
        if (!SystemUtils.IS_OS_MAC) {
            FXMLLoader fxmlLoader = new FXMLLoader(TxController.class.getClassLoader().getResource("org/ton/main/rawdump.fxml"));
            Parent root = fxmlLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Tx hash " + txEntity.getTxHash());

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
            textArea.setText(gson.toJson(tx));

            if (txEntity.getTypeTx().equals("Message")) {
                textArea.setText(gson.toJson(txEntity.getMessage()));
            } else {
                textArea.setText(gson.toJson(tx));
            }

            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            textArea.setCodeFoldingEnabled(true);
            textArea.setAntiAliasingEnabled(true);
            textArea.setEditable(false);

            RTextScrollPane sp = new RTextScrollPane(textArea);
            SwingNode sn = (SwingNode) root.lookup("#swingid");
            JFXButton btn = (JFXButton) root.lookup("#showDumpBtn");
            btn.setUserData("tx#" + txEntity.getFullBlock() + " " + txEntity.getWc() + ":" + tx.getAccountAddr() + " " + tx.getLt());
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

            if (txEntity.getTypeTx().equals("Message")) {
                codeArea.replaceText(0, 0, gson.toJson(txEntity.getMessage()));
            } else {
                codeArea.replaceText(0, 0, gson.toJson(tx));
            }

            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Tx hash " + txEntity.getTxHash());
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

    public void txRowSeqnoClick(MouseEvent mouseEvent) {
        String seq = block.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(seq);
        clipboard.setContent(content);
        log.info(seq + " copied");
        App.mainController.showInfoMsg(seq + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void txRowHashClick(MouseEvent mouseEvent) {
        String hash = txidHidden.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(hash);
        clipboard.setContent(content);
        log.info(hash + " copied");
        String lightHash = MyLocalTonUtils.getLightAddress(hash);
        App.mainController.showInfoMsg(lightHash + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void txRowSrcAddrClick(MouseEvent mouseEvent) {
        String src = from.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(src);
        clipboard.setContent(content);
        log.info(src + " copied");
        String lightSrc = MyLocalTonUtils.getLightAddress(src);
        App.mainController.showInfoMsg(lightSrc + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void txRowDestAddrClick(MouseEvent mouseEvent) {
        String src = to.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(src);
        clipboard.setContent(content);
        log.info(src + " copied");
        String lightSrc = MyLocalTonUtils.getLightAddress(src);
        App.mainController.showInfoMsg(lightSrc + " copied to clipboard", 2);
        mouseEvent.consume();
    }

    public void txRowAmountClick(MouseEvent mouseEvent) {
        String src = amount.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(src);
        clipboard.setContent(content);
        log.info(src + " copied");
        App.mainController.showInfoMsg(src + " copied to clipboard", 2);
        mouseEvent.consume();
    }
}
