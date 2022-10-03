package org.ton.ui.custom.control;


import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.io.IOException;

public class CustomMenuButton extends HBox {

    @FXML
    private Label mainLabel;

    private Node view;

    public CustomMenuButton() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-menu-button.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        view = fxmlLoader.load();
    }

    public String getMainSvgPath() {
        SVGPath svg = (SVGPath) mainLabel.getGraphic();
        return svg.getContent();
    }

    public void setMainSvgPath(String mainSvgPath) {
        SVGPath svg = (SVGPath) mainLabel.getGraphic();

        svg.setContent(mainSvgPath);
    }

    public void setSvgScaleX(double scaleX) {
        SVGPath svg = (SVGPath) mainLabel.getGraphic();
        svg.setScaleX(scaleX);
    }

    public double getSvgScaleX() {
        SVGPath svg = (SVGPath) mainLabel.getGraphic();
        return svg.getScaleX();
    }

    public void setSvgScaleY(double scaleY) {
        SVGPath svg = (SVGPath) mainLabel.getGraphic();
        svg.setScaleY(scaleY);
    }

    public double getSvgScaleY() {
        SVGPath svg = (SVGPath) mainLabel.getGraphic();
        return svg.getScaleY();
    }

    public String getLabelText() {
        return mainLabel.getText();
    }

    public void setLabelText(String labelText) {
        mainLabel.setText(labelText);
    }

    public Node getView() {
        return view;
    }

    public void activate() {
        this.requestFocus();
        this.pseudoClassStateChanged(PseudoClass.getPseudoClass("activated"), true);
    }

    public void deactivate() {
        this.pseudoClassStateChanged(PseudoClass.getPseudoClass("activated"), false);
    }

}
