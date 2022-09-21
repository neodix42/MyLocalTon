package org.ton.ui.custom.control;

import com.jfoenix.controls.JFXTextField;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class CustomSearchBar extends AnchorPane {

    @FXML
    private JFXTextField textField;
    @FXML
    private Label clearButton;

    public CustomSearchBar() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("search-bar.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
        getChildren().remove(clearButton);
        textField.textProperty().addListener((ov, oldVal, newVal) -> {
            if(oldVal.trim().equals("") && newVal.trim().length() > 0) {
                getChildren().add(clearButton);
            }
        });
    }

    @FXML
    private void clearText(Event e) {
        textField.setText("");
        getChildren().remove(clearButton);
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
