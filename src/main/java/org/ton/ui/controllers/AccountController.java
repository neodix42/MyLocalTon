package org.ton.ui.controllers;

import static org.ton.main.App.mainController;
import static org.ton.utils.MyLocalTonUtils.PATTERN;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXToggleButton;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import javafx.application.Platform;
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
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.main.App;
import org.ton.ui.custom.layout.RunMethodPaneController;
import org.ton.utils.MyLocalTonUtils;

@Slf4j
public class AccountController {

  public static JFXDialog yesNoDialog;
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

  public void accSendBtnAction() throws IOException {
    log.info("acc send {}", hexAddr.getText());
    App.mainController.showSendDialog(hexAddr.getText());
  }

  /**
   * Called when "Account Info" button is clicked. This fetches the wallet entity info
   * asynchronously and then shows a detailed dump.
   */
  @FXML
  void accInfoBtn() {
    log.info("clicked acc btn {}", hexAddr.getText());

    String[] wcAddr = hexAddr.getText().split(":");
    WalletPk walletPk = WalletPk.builder()
        .wc(Long.parseLong(wcAddr[0]))
        .hexAddress(wcAddr[1])
        .build();

    CompletableFuture.supplyAsync(() -> App.dbPool.findWallet(walletPk), ForkJoinPool.commonPool())
        .thenAccept(walletEntity -> {
          Platform.runLater(() -> {
            if (walletEntity != null) {
              try {
                showAccountDump(walletEntity);
              } catch (IOException e) {
                log.error("Error showing account dump", e);
                App.mainController.showErrorMsg("Failed to load account info", 5);
              }
            } else {
              App.mainController.showErrorMsg("Account not found", 3);
            }
          });
        });
  }

  private void showAccountDump(WalletEntity walletEntity) throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    CodeArea codeArea = new CodeArea();
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    codeArea.setEditable(false);
    codeArea.getVisibleParagraphs().addModificationObserver(
        new MyLocalTonUtils.VisibleParagraphStyler<>(codeArea, this::computeHighlighting)
    );

    codeArea.replaceText(0, 0, gson.toJson(walletEntity));

    Stage stage = new Stage();
    stage.initModality(Modality.NONE);
    stage.initStyle(StageStyle.DECORATED);
    stage.setTitle("Account " + walletEntity.getFullAddress());

    VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
    Scene scene = new Scene(new StackPane(scrollPane), 1000, 700);

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
                                      null; // Should never happen
      assert styleClass != null;
      spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
      spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
      lastKwEnd = matcher.end();
    }
    spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
    return spansBuilder.create();
  }

  public void accRowHexAddrClick(MouseEvent mouseEvent) {
    copyToClipboard(hexAddr.getText());
    mouseEvent.consume();
  }

  public void accRowB64AddrClick(MouseEvent mouseEvent) {
    copyToClipboard(b64Addr.getText());
    mouseEvent.consume();
  }

  public void accRowB64UrlAddrClick(MouseEvent mouseEvent) {
    copyToClipboard(b64urlAddr.getText());
    mouseEvent.consume();
  }

  public void accRowNb64AddrClick(MouseEvent mouseEvent) {
    copyToClipboard(nb64Addr.getText());
    mouseEvent.consume();
  }

  public void accRowNb64UrlAddrClick(MouseEvent mouseEvent) {
    copyToClipboard(nb64urlAddr.getText());
    mouseEvent.consume();
  }

  public void accRowBalanceClick(MouseEvent mouseEvent) {
    copyToClipboard(balance.getText());
    mouseEvent.consume();
  }

  public void walletDeleteBtnAction() {
    log.debug("deleting wallet");
    MyLocalTonUtils.deleteWalletByFullAddress(hexAddr.getText());
  }

  public void runMethodBtn() throws IOException {
    log.info("runMethodBtn");
    FXMLLoader loader = new FXMLLoader(
        App.class.getClassLoader().getResource("org/ton/ui/custom/layout/run-pane.fxml"));
    Parent parent = loader.load();
    RunMethodPaneController controller = loader.getController();
    controller.setAddress(hexAddr.getText());
    controller.setHeader("Execute runmethod");

    JFXDialogLayout content = new JFXDialogLayout();
    content.setBody(parent);

    yesNoDialog = new JFXDialog(App.root, content, JFXDialog.DialogTransition.CENTER);
    yesNoDialog.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
        yesNoDialog.close();
      }
    });
    yesNoDialog.setOnDialogOpened(jfxDialogEvent -> controller.requestFocusToMethodId());
    yesNoDialog.show();
  }

  public void showSrcBtn(ActionEvent actionEvent) {

  }

  public void showAccTxsBtn(ActionEvent actionEvent) {
    mainController.showAccTxs(hexAddr.getText());
  }

  private void copyToClipboard(String text) {
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(text);
    clipboard.setContent(content);
    log.debug("{} copied", text);
    String lightAddr = MyLocalTonUtils.getLightAddress(text);
    App.mainController.showInfoMsg(lightAddr + " copied to clipboard", 2);
  }
}
