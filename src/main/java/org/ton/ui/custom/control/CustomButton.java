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

    public CustomButton(CustomButtonType type, double width) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-button.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();
        switch (type) {
            case CREATE:
                this.setText("Create wallet");
                this.setGraphicTextGap(10.0);
                this.setPrefWidth(width);
                this.setSvgText(
                        "M9.89189 3.93651C9.533 3.99094 9.26048 4.30332 9.26583 4.67632L9.26528 9.26518L4.67415 9.26584C4.41031 9.26209 4.16292 9.40182 4.02884 9.63155C3.89476 9.86127 3.89476 10.1454 4.02884 10.3751L4.09297 10.4679C4.23398 10.6418 4.4483 10.744 4.67627 10.7408L9.26528 10.7402L9.26582 15.3325C9.26207 15.5963 9.40181 15.8437 9.63153 15.9778C9.86126 16.1119 10.1454 16.1119 10.3751 15.9778L10.4679 15.9137C10.6418 15.7727 10.744 15.5584 10.7408 15.3304L10.7403 10.7402L15.3325 10.7408C15.5963 10.7446 15.8437 10.6048 15.9778 10.3751C16.1119 10.1454 16.1119 9.86127 15.9778 9.63155L15.9137 9.53876C15.7727 9.36483 15.5583 9.26263 15.3304 9.26585L10.7403 9.26518L10.7408 4.67417C10.7437 4.4769 10.6656 4.28481 10.5245 4.14387C10.3834 4.00296 10.1913 3.92517 9.99186 3.9282L9.89189 3.93651Z"
                );
                break;
            case REFRESH:
                break;
            case COMMON:
                break;
            default:
                break;
        }
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

    public enum CustomButtonType {
        CREATE, REFRESH, COMMON;
    }
}
