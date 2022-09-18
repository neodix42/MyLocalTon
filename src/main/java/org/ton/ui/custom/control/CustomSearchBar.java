package org.ton.ui.custom.control;

import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class CustomSearchBar extends HBox {

    @FXML
    private JFXTextField textField;

    public CustomSearchBar() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("search-bar.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    public JFXTextField getTextField() {
        return textField;
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
}
