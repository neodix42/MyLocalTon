package org.ton.ui.custom.animation;

import javafx.animation.Transition;
import javafx.scene.layout.Region;
import javafx.util.Duration;


public class ResizeHeightTranslation extends Transition {

    protected Region region;
    protected double startHeight;
    protected double newHeight;
    protected double heightDiff;

    public ResizeHeightTranslation(Duration duration, Region region, double newHeight ) {
        setCycleDuration(duration);
        this.region = region;
        this.newHeight = newHeight;
        this.startHeight = region.getHeight();
        this.heightDiff = newHeight - startHeight;
    }

    @Override
    protected void interpolate(double fraction) {
        region.setPrefHeight( startHeight + ( heightDiff * fraction ) );
    }
}
