package org.ton.mylocalton.ui.custom.layout;

import static org.ton.mylocalton.ui.custom.events.CustomEventBus.emit;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.ui.custom.control.CustomButton;
import org.ton.mylocalton.ui.custom.events.CustomEvent;
import org.ton.mylocalton.ui.custom.events.event.CustomActionEvent;
import org.ton.mylocalton.utils.MyLocalTonUtils;

@Slf4j
public class ConfirmPaneController implements Initializable {

  @FXML private VBox vboxBody;
  @FXML private AnchorPane anchorPane;
  @FXML private CustomButton okButton;
  @FXML private Label body;
  @FXML private Label header;

  @Setter private Action action;

  @Setter private String address;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    okButton.setText("OK");
  }

  @FXML
  private void confirm() {
    switch (action) {
      case DELETE_NODE:
        doDeleteNode();
        break;
      case CONFIRM:
        emit(new CustomActionEvent(CustomEvent.Type.DIALOG_YES_NO_CLOSE));
        break;
    }
  }

  private void doDeleteNode() {
    log.debug("do delete {}", address);
    Platform.runLater(
        () -> {
          emit(new CustomActionEvent(CustomEvent.Type.DIALOG_YES_NO_CLOSE));
        });
    MyLocalTonUtils.doDelete(address);
  }

  public void setBody(String body) {
    this.body.setText(body);
  }

  public void setHeader(String header) {
    this.header.setText(header);
  }

  public void setOkButtonText(String text) {
    this.okButton.setText(text);
  }

  public void setHeight(double h) {
    if (h > 270.0) {
      anchorPane.setPrefHeight(h);
      double y = h - 70.0;
      double h2 = h - 150.0;
      okButton.setLayoutY(y);
      vboxBody.setPrefHeight(h2);
      body.setPrefHeight(h2);
    }
  }

  public enum Action {
    DELETE_NODE,
    INSTALL_PYTHON,
    INSTALL_PIP,
    INSTALL_TON_HTTP_API,
    CONFIRM;
  }
}
