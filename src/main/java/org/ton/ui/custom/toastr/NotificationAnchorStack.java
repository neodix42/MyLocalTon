package org.ton.ui.custom.toastr;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Timer;
import java.util.TimerTask;

public class NotificationAnchorStack extends AnchorPane {

    private IntegerProperty defShow;

    private IntegerProperty defFadeIn;

    private IntegerProperty defFadeOut;

    private IntegerProperty defCollapse;

    private VBox notificationStack;

    public NotificationAnchorStack() {
        defShow = new SimpleIntegerProperty(2000);
        defFadeIn = new SimpleIntegerProperty(200);
        defFadeOut = new SimpleIntegerProperty(150);
        defCollapse = new SimpleIntegerProperty(150);
        notificationStack = new VBox(10);

        getChildren().add(notificationStack);

        setRightAnchor(notificationStack, 0.0);
        setTopAnchor(notificationStack, 0.0);
        setBottomAnchor(notificationStack, 0.0);

        notificationStack.setStyle(buildNotificationStackStyle());
        notificationStack.getChildren().addListener(this::childrenListener);
    }

    public String buildNotificationStackStyle() {
        return "-fx-pref-width: 200px;"
                + "-fx-padding: 10px;";
    }

    public IntegerProperty defaultShowProperty() {
        return defShow;
    }

    public IntegerProperty defaultFadeInProperty() {
        return defFadeIn;
    }

    public IntegerProperty defaultFadeOutProperty() {
        return defFadeOut;
    }

    public IntegerProperty defaultCollapseProperty() {
        return defCollapse;
    }

    public int getDefaultShow() {
        return defShow.get();
    }

    public int getDefaultFadeIn() {
        return defFadeIn.get();
    }

    public int getDefaultFadeOut() {
        return defFadeOut.get();
    }

    public int getDefaultCollapse() {
        return defCollapse.get();
    }

    public void setDefaultShow(int mil) {
        this.defShow.set(mil);
    }

    public void setDefaultFadeIn(int mil) {
        this.defFadeIn.set(mil);
    }

    public void setDefaultFadeOut(int mil) {
        this.defFadeOut.set(mil);
    }

    public void setDefaultCollapse(int mil) {
        this.defCollapse.set(mil);
    }

    public VBox getNotificationStack() {
        return notificationStack;
    }

    private void childrenListener(ListChangeListener.Change change) {
        if (change.getList().isEmpty()) {
            toBack();
        } else {
            toFront();
        }
    }

    public void notify(Notification notification) {
        notify(
                notification,
                defFadeIn.get(),
                defShow.get(),
                defFadeOut.get(),
                defCollapse.get()
        );
    }

    public void notify(Notification note, int show) {
        notify(
                note,
                defFadeIn.get(),
                show,
                defFadeOut.get(),
                defCollapse.get()
        );
    }

    public void notify(Notification note, int in, int show, int out, int col) {
        FadeTransition transition;

        notificationStack.getChildren().add(note);

        transition = new FadeTransition(
                Duration.millis(in),
                note
        );

        transition.setByValue(1);
        transition.play();

        new Timer().schedule(buildFadeOut(note, out, col), show);
    }

    private TimerTask buildFadeOut(Notification note, int out, int col) {
        return new TimerTask() {
            @Override
            public void run() {
                FadeTransition fade;
                TranslateTransition trans;

                trans = new TranslateTransition(
                        Duration.millis(col),
                        notificationStack
                );
                trans.setByY(0 - (note.getHeight() + notificationStack.getSpacing()));
                trans.setOnFinished(event -> {
                    notificationStack.getChildren().remove(note);
                    notificationStack.setTranslateY(0);
                });

                fade = new FadeTransition(
                        Duration.millis(out),
                        note
                );
                fade.setByValue(-1);
                fade.setOnFinished(event -> trans.play());
                fade.play();
            }
        };
    }
}