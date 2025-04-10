package org.ton.mylocalton.ui.custom.control;

import static org.ton.mylocalton.ui.custom.events.CustomEventBus.emit;
import static org.ton.mylocalton.ui.custom.events.CustomEventBus.listenFor;

import com.jfoenix.controls.JFXTextField;
import java.io.IOException;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import org.ton.mylocalton.ui.custom.events.CustomEvent;
import org.ton.mylocalton.ui.custom.events.event.CustomSearchEvent;

public class CustomSearchBar extends AnchorPane {

  @FXML private JFXTextField textField;
  @FXML private Label clearButton;

  public CustomSearchBar() throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("search-bar.fxml"));
    fxmlLoader.setRoot(this);
    fxmlLoader.setController(this);
    fxmlLoader.load();
    listenFor(CustomSearchEvent.class, this::handle);
    getChildren().remove(clearButton);
    textField
        .textProperty()
        .addListener(
            (ov, oldVal, newVal) -> {
              if (oldVal.trim().equals("") && newVal.trim().length() > 0) {
                getChildren().add(clearButton);
              }
            });
  }

  @FXML
  private void clearText(Event e) {
    clearTextField();
    emit(new CustomSearchEvent(CustomEvent.Type.SEARCH_CLEAR));
  }

  private void clearTextField() {
    textField.setText("");
    getChildren().remove(clearButton);
  }

  public void handle(CustomEvent event) {
    switch (event.getEventType()) {
      case SEARCH_REMOVE:
        clearTextField();
        break;
    }
  }

  public String getSearchPromptText() {
    return textField.getPromptText();
  }

  public void setSearchPromptText(String text) {
    textField.setPromptText(text);
  }

  public String getSearchText() {
    return textField.getText();
  }

  public JFXTextField getTextField() {
    return textField;
  }
}
