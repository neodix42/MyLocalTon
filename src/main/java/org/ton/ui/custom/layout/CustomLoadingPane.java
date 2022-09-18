package org.ton.ui.custom.layout;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class CustomLoadingPane implements Initializable {

    @FXML
    private SVGPath svg;

    private RotateTransition rotator;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        rotator = new RotateTransition(Duration.seconds(2), svg);
        //rotator.setAxis(Rotate.X_AXIS);
        rotator.setFromAngle(0);
        rotator.setToAngle(360);
        rotator.setCycleCount(Transition.INDEFINITE);
        rotator.setInterpolator(Interpolator.LINEAR);
        rotator.play();
    }

}
