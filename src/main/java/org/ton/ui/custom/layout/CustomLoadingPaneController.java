package org.ton.ui.custom.layout;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class CustomLoadingPaneController implements Initializable {

    @FXML
    private SVGPath svg;

    @FXML
    private Label line1, line2;

    private RotateTransition rotator;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        rotator = new RotateTransition(Duration.seconds(1), svg);
        rotator.setFromAngle(0);
        rotator.setToAngle(360);
        rotator.setCycleCount(Transition.INDEFINITE);
        rotator.setInterpolator(Interpolator.LINEAR);
        rotator.play();
    }

    public void setLines(String l1, String l2) {
        line1.setText(l1);
        line2.setText(l2);
    }

}
