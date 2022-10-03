package org.ton.ui.custom.control;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.io.IOException;

public class CustomDirectoryButton extends HBox {

    @FXML
    private SVGPath svg;

    public CustomDirectoryButton() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-directory-button.fxml"));
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
