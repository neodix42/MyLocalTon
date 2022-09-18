package org.ton.ui.custom.control;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.shape.SVGPath;

import java.io.IOException;

public class CustomButton extends JFXButton {

    @FXML
    private SVGPath svg;

    public CustomButton() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-button.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
    }

    public SVGPath getSvg() {
        return svg;
    }

    public void setSvg(SVGPath svg) {
        this.svg = svg;
    }

    public String getSvgText() {
        return svg.getContent();
    }

    public void setSvgText(String svgText) {
        svg.setContent(svgText);
    }
}
