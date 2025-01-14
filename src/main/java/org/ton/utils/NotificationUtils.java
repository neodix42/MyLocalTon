package org.ton.utils;

import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NotificationUtils {

  public static void showRawDumpNotification(Stage ownerStage, String message) {
    Popup popup = new Popup();

    Label label = new Label(message);
    label.setWrapText(true);
    label.setStyle("-fx-text-fill: black; -fx-font-size: 14;");

    ImageView imageView = null;
    try {
      Image img = new Image(Objects.requireNonNull(
          NotificationUtils.class.getClassLoader().getResourceAsStream("org/ton/images/logo.png")
      ));
      imageView = new ImageView(img);
      imageView.setFitWidth(24);
      imageView.setFitHeight(24);
    } catch (Exception e) {
      System.err.println("Could not load icon: " + e.getMessage());
    }

    HBox hbox = new HBox();
    hbox.setAlignment(Pos.CENTER_LEFT);
    hbox.setSpacing(10);
    hbox.setPadding(new Insets(10));
    hbox.setStyle(
        "-fx-background-color: white;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;"
    );
    if (imageView != null) {
      hbox.getChildren().add(imageView);
    }
    hbox.getChildren().add(label);

    hbox.setEffect(new DropShadow(10, Color.color(0, 0, 0, 0.2)));

    popup.getContent().add(hbox);

    double x = ownerStage.getX() + ownerStage.getWidth() - 320;
    double y = ownerStage.getY() + 80;
    popup.show(ownerStage, x, y);

    PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
    delay.setOnFinished(event -> popup.hide());
    delay.play();
  }

}
