package org.ton.ui.custom.control;

import com.jfoenix.controls.JFXComboBox;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class CustomComboBox extends HBox {

    @FXML
    private JFXComboBox comboBox;

    public CustomComboBox() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-combobox.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();

        comboBox.getItems().addAll("Simple Wallet V3", "Simple Wallet V2", "Simple Wallet V1");
        comboBox.getSelectionModel().select(0);
    }

    public String getFieldPromptText() {
        return comboBox.getPromptText();
    }

    public void setFieldPromptText(String text) {
        comboBox.setPromptText(text);
    }
}
