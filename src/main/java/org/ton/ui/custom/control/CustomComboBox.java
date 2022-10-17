package org.ton.ui.custom.control;

import com.jfoenix.controls.JFXComboBox;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class CustomComboBox extends HBox {

    @FXML
    private JFXComboBox<String> comboBox;

    public CustomComboBox() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-combobox.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    public String getFieldPromptText() {
        return comboBox.getPromptText();
    }

    public void setFieldPromptText(String text) {
        comboBox.setPromptText(text);
    }

    public void addItem(String item) {
        comboBox.getItems().add(item);
    }

    public void selectItem(String item) {
        comboBox.getSelectionModel().select(item);
    }

    public String getValue() {
        return comboBox.getValue();
    }
}
