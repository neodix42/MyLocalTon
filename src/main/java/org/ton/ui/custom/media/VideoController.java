package org.ton.ui.custom.media;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.net.URL;
import java.util.ResourceBundle;

public class VideoController implements Initializable {

    @FXML
    private MediaView mv;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        MediaPlayer player = new MediaPlayer(new Media(getClass().getResource("pulp-fiction-wtf.mp4").toExternalForm()));
        mv.setMediaPlayer(player);
        player.setMute(true);

        player.setCycleCount(MediaPlayer.INDEFINITE);
        player.play();
    }
}
