package org.ton.ui.custom.control;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class CustomInfoLabel extends HBox {

    @FXML
    private Label primaryLabel, secondaryLabel;

    public CustomInfoLabel() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("info-label.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    public String getPrimaryText() {
        return primaryLabel.getText();
    }

    public void setPrimaryText(String primaryText) {
        primaryLabel.setText(primaryText);
    }

    public double getPrimaryWidth() {
        return primaryLabel.getPrefWidth();
    }

    public void setPrimaryWidth(double primaryWidth) {
        primaryLabel.setPrefWidth(primaryWidth);
    }

    public String getSecondaryText() {
        return secondaryLabel.getText();
    }

    public void setSecondaryText(String secondaryText) {
        secondaryLabel.setText(secondaryText);
    }

    public double getSecondaryWidth() {
        return secondaryLabel.getPrefWidth();
    }

    public void setSecondaryWidth(double secondaryWidth) {
        secondaryLabel.setPrefWidth(secondaryWidth);
    }

}
