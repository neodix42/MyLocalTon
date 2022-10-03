package org.ton.ui.custom.control;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXMLLoader;

import java.io.IOException;

public class CustomSubMenuButton extends JFXButton {

    public CustomSubMenuButton() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-sub-menu-button.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }
}
