package org.ton.ui.custom.control;


import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import org.ton.ui.custom.animation.ResizeHeightTranslation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CustomExpandButton extends AnchorPane {

    @FXML
    private Label mainLabel;

    @FXML
    private Label secondLabel;

    @FXML
    private HBox hBoxButton, buttons;

    private ObservableList<Node> ObservableListButtons;

    private List<Node> listbuttons = new ArrayList<>();


    private double buttonsHeight = 0.0;

    private double gapBetweenButtons = 5.0;

    private final double TRANSITION_TIME = 0.25;

    private double mainButtonHeight = 0.0;

    private int index = 0;

    public CustomExpandButton() throws IOException {
        super();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("custom-expand-button.fxml"));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        fxmlLoader.load();

        ObservableListButtons = buttons.getChildren();

        buttons.getChildren().addListener(new ListChangeListener<Node>() {

            @Override
            public void onChanged(Change<? extends Node> change) {
                if(change.next()) {
                    Node node = change.getList().get(change.getFrom());
                    node.setLayoutX(0.0);
                    node.setLayoutY(0.0);
                    buttonsHeight += ((Region) node).getPrefHeight() + gapBetweenButtons;
                    getChildren().add(index, node);
                    listbuttons.add(index, node);
                    index++;
                }
            }

        });
        mainButtonHeight = getPrefHeight();
    }

    public void addButton(CustomSubMenuButton button) {
        buttons.getChildren().add(button);
    }

    public void removeButton(CustomSubMenuButton button) {
        if(buttons.getChildren().contains(button)) {
            buttons.getChildren().remove(button);
        }
        if(getChildren().contains(button)) {
            index--;
            getChildren().remove(button);
            listbuttons.remove(button);
            buttonsHeight -= (button.getPrefHeight() + gapBetweenButtons);
        }
    }

    public String getMainSvgPath() {
        SVGPath svg = (SVGPath) mainLabel.getGraphic();

        return svg.getContent();
    }

    public void setMainSvgPath(String mainSvgPath) {
        SVGPath svg = (SVGPath) mainLabel.getGraphic();
        svg.setContent(mainSvgPath);

    }

    public String getLabelText() {
        return mainLabel.getText();
    }

    public void setLabelText(String labelText) {
        mainLabel.setText(labelText);
    }

    private void rotate(double angle) {
        SVGPath svg = (SVGPath) secondLabel.getGraphic();
        RotateTransition transition = new RotateTransition();
        transition.setNode(svg);
        transition.setDuration(Duration.seconds(TRANSITION_TIME));
        transition.setToAngle(angle);

        transition.play();

    }

    public boolean isOpened() {
        SVGPath svg = (SVGPath) secondLabel.getGraphic();
        double r = svg.getRotate();
        return svg.getRotate() != 0.0;
    }

    private void slideButtonsDown(double toY) {
        double decreasePosition = 0.0;
        for(int i = listbuttons.size() -1; i >=0 ;i--) {
            Node node = listbuttons.get(i);
            TranslateTransition tr =
                    new TranslateTransition(Duration.seconds((TRANSITION_TIME + 0.01)), node);
            decreasePosition += ((Region) node).getPrefHeight() + gapBetweenButtons;
            double y = toY - decreasePosition;
            tr.setToY(y);
            tr.play();
        }
    }

    private void slideButtonsUp() {
        listbuttons.forEach(node -> {
            TranslateTransition tr =
                    new TranslateTransition(Duration.seconds((TRANSITION_TIME - 0.01)), node);
            tr.setToY(0.0);
            tr.play();
        });
    }

    public void rotateIcon() {
        if(this.isOpened()) {
            rotate(0.0);
            ResizeHeightTranslation t =
                    new ResizeHeightTranslation(Duration.seconds(TRANSITION_TIME), this, mainButtonHeight);
            slideButtonsUp();
            t.play();
        } else {
            rotate(90.0);
            double newHeight = mainButtonHeight + buttonsHeight + gapBetweenButtons;
            ResizeHeightTranslation t =
                    new ResizeHeightTranslation(Duration.seconds(TRANSITION_TIME), this, newHeight);
            slideButtonsDown(newHeight);
            t.play();
        }
    }

    public ObservableList<Node> getButtons() {
        return buttons.getChildren();
    }

    public double getMainButtonHeight() {
        return this.mainButtonHeight;
    }

    public void activate() {
        hBoxButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("activated"), true);
        this.requestFocus();
    }

    public void deactivate() {
        hBoxButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("activated"), false);
    }

}

