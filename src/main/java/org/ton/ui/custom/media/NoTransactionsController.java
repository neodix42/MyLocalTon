package org.ton.ui.custom.media;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import org.ton.ui.custom.layout.CustomMainLayout;

import java.net.URL;
import java.util.ResourceBundle;

public class NoTransactionsController implements Initializable {

    @FXML
    private MediaView mediaView;

    @FXML
    private SVGPath svg;

    private CustomMainLayout main;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        MediaPlayer player = new MediaPlayer(new Media(getClass().getResource("pulp-fiction-wtf.mp4").toExternalForm()));
        mediaView.setMediaPlayer(player);
        player.setMute(true);

        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.play();
    }

    public void refresh(ActionEvent actionEvent) {
        RotateTransition rotator = new RotateTransition(Duration.seconds(0.3), svg);
        //rotator.setAxis(Rotate.X_AXIS);
        rotator.setFromAngle(0);
        rotator.setToAngle(360);
        rotator.setCycleCount(3);
        rotator.setInterpolator(Interpolator.LINEAR);
        rotator.setOnFinished(e -> main.removeNoTransactionsView());
        rotator.play();
    }

    public void setMain(CustomMainLayout main) {
        this.main = main;
    }
}
