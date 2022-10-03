package org.ton.ui.custom.media;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.media.MediaView;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.ResourceBundle;

public class NoBlocksTransactionsController implements Initializable {

    @FXML
    private JFXButton button;
    @FXML
    private Label label;
    @FXML
    private MediaView mediaView;

    @FXML
    private SVGPath svg;

    private ViewType viewType;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
//        MediaPlayer player = new MediaPlayer(new Media(getClass().getResource("pulp-fiction-wtf.mp4").toExternalForm()));
//        mediaView.setMediaPlayer(player);
//        player.setMute(true);
//        player.setCycleCount(MediaPlayer.INDEFINITE);
//        player.play();
    }

    @FXML
    private void refresh() {
//        RotateTransition rotator = new RotateTransition(Duration.seconds(0.3), svg);
//        rotator.setFromAngle(0);
//        rotator.setToAngle(360);
//        rotator.setCycleCount(3);
//        rotator.setInterpolator(Interpolator.LINEAR);
//        rotator.play();
//        emit(new CustomRefreshEvent(CustomEvent.Type.REFRESH, viewType));
    }

    public void setViewType(ViewType viewType) {
        this.viewType = viewType;
        switch (viewType) {
            case BLOCKS:
                label.setText("Starting...");
                break;
            case TRANSACTIONS:
                label.setText("Starting...");
                break;
            case ACCOUNTS:
                label.setText("Starting...");
                break;
        }

    }

    public ViewType getViewType() {
        return viewType;
    }

    public enum ViewType {
        BLOCKS,
        TRANSACTIONS,
        ACCOUNTS;
    }

}
