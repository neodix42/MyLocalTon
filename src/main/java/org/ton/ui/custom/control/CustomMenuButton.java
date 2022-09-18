package org.ton.ui.custom.control;


import com.jfoenix.controls.JFXButton;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.shape.SVGPath;

import java.io.IOException;

public class CustomMenuButton extends JFXButton {

    private String mainSvgPath;
    private String labelText;
    private String secondarySvgPath;

    private Node view;

    public CustomMenuButton() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-menu-button.fxml"));
        try {
            view = (Node) fxmlLoader.load();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        setGraphic(view);
    }

    public String getMainSvgPath() {
        return mainSvgPath;
    }

    public void setMainSvgPath(String mainSvgPath) {
        Label lb = (Label) view.lookup("#mainLabel");
        SVGPath svg = (SVGPath) lb.getGraphic();
        svg.setContent(mainSvgPath);
        this.mainSvgPath = mainSvgPath;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        Label lb = (Label) view.lookup("#mainLabel");
        lb.setText(labelText);
        this.labelText = labelText;
    }

    public String getSecondarySvgPath() {
        return secondarySvgPath;
    }

    public void setSecondarySvgPath(String secondarySvgPath) {
        Label lb = (Label) view.lookup("#secondLabel");
        SVGPath svg = (SVGPath) lb.getGraphic();
        svg.setContent(secondarySvgPath);
        this.secondarySvgPath = secondarySvgPath;
    }

    public StringProperty mainSvgPathProperty() {
        return new SimpleStringProperty(mainSvgPath);
    }

    public StringProperty labelTextProperty() {
        return new SimpleStringProperty(labelText);
    }

    public StringProperty secondarySvgPathProperty() {
        return new SimpleStringProperty(secondarySvgPath);
    }

    public Node getView() {
        return view;
    }

    public boolean isOpened() {
        Label lb = (Label) this.getView().lookup("#secondLabel");
        SVGPath svg = (SVGPath) lb.getGraphic();
        double r = svg.getRotate();
        return svg.getRotate() != 0.0;
    }

}
