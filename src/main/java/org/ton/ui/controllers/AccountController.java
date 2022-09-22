package org.ton.ui.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXToggleButton;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.main.App;
import org.ton.utils.Utils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;

import static com.sun.javafx.PlatformUtil.isMac;
import static org.ton.main.App.mainController;
import static org.ton.utils.Utils.PATTERN;

@Slf4j
public class AccountController {

    @FXML
    public JFXToggleButton toggleBtn;
    @FXML
    public GridPane grid;

    @FXML
    public Label hexAddrLabel;

    @FXML
    public Label hexAddr;

    @FXML
    public Label b64AddrLabel;

    @FXML
    public Label b64Addr;

    @FXML
    public Label b64urlAddrLabel;
    @FXML
    public Label b64urlAddr;

    @FXML
    public Label nb64AddrLabel;
    @FXML
    public Label nb64Addr;

    @FXML
    public Label nb64urlAddrLabel;
    @FXML
    public Label nb64urlAddr;

    @FXML
    public Label balance;

    @FXML
    public Label type;

    @FXML
    public Label seqno;

    @FXML
    public Label createdat;

    @FXML
    Label status;

    @FXML
    BorderPane accRowBorderPane;

    @FXML
    JFXButton accSendBtn;

    @FXML
    JFXButton createSubWalletBtn;

    @FXML
    Label walledId;

    @FXML
    JFXButton walletDeleteBtn;

    public static JFXDialog yesNoDialog;

    public void accSendBtnAction() throws IOException {
        log.info("acc send {}", hexAddr.getText());
        App.mainController.showSendDialog(hexAddr.getText());
    }

//    String style;
//
//    @FXML
//    void handleMouseEnter() {
//        style = accRowBorderPane.getStyle();
//        accRowBorderPane.setStyle("-fx-background-color: bedef4; -fx-padding: 10 0 0 5;");
//    }
//
//    @FXML
//    void handleMouseExit() {
//        accRowBorderPane.setStyle(style);
//    }

    @FXML
    void accInfoBtn() throws IOException {
        log.debug("clicked acc btn {}", hexAddr.getText());
        String[] wcAddr = hexAddr.getText().split(":");
        WalletPk walletPk = WalletPk.builder()
                .wc(Long.parseLong(wcAddr[0]))
                .hexAddress(wcAddr[1])
                .build();
        WalletEntity walletEntity = App.dbPool.findWallet(walletPk);
        showAccountDump(walletEntity);
    }

    private void showAccountDump(WalletEntity walletEntity) throws IOException {
        if (!isMac()) {
            FXMLLoader fxmlLoader = new FXMLLoader(BlockController.class.getClassLoader().getResource("org/ton/main/rawdump.fxml"));
            Parent root = fxmlLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Account " + walletEntity.getFullAddress());

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
            textArea.setText(gson.toJson(walletEntity));
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            textArea.setCodeFoldingEnabled(true);
            textArea.setAntiAliasingEnabled(true);
            textArea.setEditable(false);

            RTextScrollPane sp = new RTextScrollPane(textArea);
            SwingNode sn = (SwingNode) root.lookup("#swingid");
            JFXButton btn = (JFXButton) root.lookup("#showDumpBtn");
            btn.setUserData("account#" + walletEntity.getFullAddress());
            sn.setContent(sp);
        } else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            CodeArea codeArea = new CodeArea();
            codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
            codeArea.setEditable(false);
            codeArea.getVisibleParagraphs().addModificationObserver
                    (
                            new Utils.VisibleParagraphStyler<>(codeArea, this::computeHighlighting)
                    );

            codeArea.replaceText(0, 0, gson.toJson(walletEntity));

            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.initStyle(StageStyle.DECORATED);
            stage.setTitle("Account " + walletEntity.getFullAddress());
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

    public void accRowHexAddrClick(MouseEvent mouseEvent) {
        String hex = hexAddr.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(hex);
        clipboard.setContent(content);
        log.debug(hex + " copied");
        App.mainController.showInfoMsg(hex + " copied to clipboard", 0.5);
        mouseEvent.consume();
    }

    public void accRowB64AddrClick(MouseEvent mouseEvent) {
        String addr = b64Addr.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        App.mainController.showInfoMsg(addr + " copied to clipboard", 0.5);
        mouseEvent.consume();
    }

    public void accRowB64UrlAddrClick(MouseEvent mouseEvent) {
        String addr = b64urlAddr.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        App.mainController.showInfoMsg(addr + " copied to clipboard", 0.5);
        mouseEvent.consume();
    }

    public void accRowNb64AddrClick(MouseEvent mouseEvent) {
        String addr = nb64Addr.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        App.mainController.showInfoMsg(addr + " copied to clipboard", 0.5);
        mouseEvent.consume();
    }

    public void accRowNb64UrlAddrClick(MouseEvent mouseEvent) {
        String addr = nb64urlAddr.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(addr);
        clipboard.setContent(content);
        log.debug(addr + " copied");
        App.mainController.showInfoMsg(addr + " copied to clipboard", 0.5);
        mouseEvent.consume();
    }

    public void accRowBalanceClick(MouseEvent mouseEvent) {
        String coins = balance.getText();
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(coins);
        clipboard.setContent(content);
        log.debug(coins + " copied");
        App.mainController.showInfoMsg(coins + " copied to clipboard", 0.5);
        mouseEvent.consume();
    }

    public void walletDeleteBtnAction() {
        log.debug("deleting wallet");
        Utils.deleteWalletByFullAddress(hexAddr.getText());
    }

    public void runMethodBtn() throws IOException {
        log.info("runMethodBtn");

        Parent dialog = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/main/yesnodialog.fxml")).load();
        dialog.lookup("#header").setVisible(true);
        dialog.lookup("#body").setVisible(false);
        ((Label) dialog.lookup("#action")).setText("runmethod");
        ((Label) dialog.lookup("#address")).setText(hexAddr.getText());
        ((Label) dialog.lookup("#header")).setText("Execute runmethod");
        ((Label) dialog.lookup("#body")).setText("For example, enter seqno or get_public_key against wallet contract");

        dialog.lookup("#inputFields").setVisible(true);
        dialog.lookup("#seqno").setVisible(true);
        dialog.lookup("#workchain").setVisible(false);
        dialog.lookup("#subWalletId").setVisible(false);

        dialog.lookup("#okBtn").setDisable(false);

        JFXDialogLayout content = new JFXDialogLayout();
        content.setBody(dialog);

        yesNoDialog = new JFXDialog(App.root, content, JFXDialog.DialogTransition.CENTER);
        yesNoDialog.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                        yesNoDialog.close();
                    }
                }
        );
        yesNoDialog.setOnDialogOpened(jfxDialogEvent -> {
            dialog.lookup("#seqno").requestFocus();
        });
        yesNoDialog.show();
    }

    public void showSrcBtn(ActionEvent actionEvent) {

    }

    public void showaAccTxsBtn(ActionEvent actionEvent) throws IOException {
        mainController.showAccTxs(hexAddr.getText());
    }
}
