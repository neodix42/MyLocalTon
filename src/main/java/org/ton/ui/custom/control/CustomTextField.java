package org.ton.ui.custom.control;

import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class CustomTextField extends HBox {

    @FXML
    private JFXTextField textField;

    public CustomTextField() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-textfield.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    public String getFieldPromptText() {
        return textField.getPromptText();
    }

    public void setFieldPromptText(String text) {
        textField.setPromptText(text);
    }

    public String getFieldText() {
        return textField.getText();
    }

    public void setFieldText(String text) {
        textField.setText(text);
    }

    public boolean getEditableField() {
        return textField.isEditable();
    }

    public void setEditableField(boolean editable) {
        textField.setEditable(editable);
    }

    public JFXTextField getTextField() {
        return textField;
    }
}
