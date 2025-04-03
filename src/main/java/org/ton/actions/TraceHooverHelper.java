package org.ton.actions;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraceHooverHelper {

    private Popup popup;
    private final PauseTransition hideTransition = new PauseTransition(Duration.millis(200));

    public void assignTwoLabels(Pane pane,
                                double midX,
                                double midY,
                                String topText,
                                String bottomText,
                                String contentForPopup) {
        var group = new Group();

        var topLabel = createLabel(topText, -15);
        var bottomLabel = createLabel(bottomText, 5);
        
        group.getChildren().addAll(topLabel, bottomLabel);
        
        group.setLayoutX(midX);
        group.setLayoutY(midY);

        group.setOnMouseEntered(e -> {
            if (popup == null || !popup.isShowing()) {
                showPopupWithScrollPane(group, contentForPopup);
            }
            hideTransition.stop();
        });

        group.setOnMouseExited(e -> hideTransition.playFromStart());

        pane.getChildren().add(group);
    }

    private Label createLabel(String text, double offsetY) {
        var label = new Label(text);
        label.setFont(Font.font(10));
        label.setPrefWidth(80);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setLayoutX(0);
        label.setLayoutY(offsetY);
        return label;
    }

    private void showPopupWithScrollPane(Node owner, String longText) {
        popup = new Popup();
        popup.setAutoHide(false);

        var scrollPane = getScrollPane(longText);

        popup.getContent().add(scrollPane);

        hideTransition.setOnFinished(e -> hidePopup());

        var bounds = owner.localToScreen(owner.getBoundsInLocal());
        popup.show(owner, bounds.getMinX(), bounds.getMaxY());
    }

    private ScrollPane getScrollPane(String longText) {
        var contentLabel = new Label(longText);
        contentLabel.setWrapText(true);

        var contentBox = new VBox();
        contentBox.setMaxWidth(400);
        contentBox.setPadding(new Insets(5, 5, 5, 5));
        contentBox.getChildren().add(contentLabel);

        var scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(400, 200);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
                "-fx-background: #f8f8f8; " +
                "-fx-padding: 5; " +
                "-fx-border-color: #cccccc; " +
                "-fx-border-radius: 4; " +
                "-fx-background-radius: 4;"
        );

        scrollPane.setOnMouseEntered(e -> hideTransition.stop());
        scrollPane.setOnMouseExited(e -> hideTransition.playFromStart());

        return scrollPane;
    }

    private void hidePopup() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }
}