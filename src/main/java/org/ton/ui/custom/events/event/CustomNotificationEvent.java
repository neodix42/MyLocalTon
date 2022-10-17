package org.ton.ui.custom.events.event;

import org.ton.ui.custom.events.CustomEvent;

public class CustomNotificationEvent extends CustomEvent {

    private String message;
    private double seconds;

    public CustomNotificationEvent(Type eventType, String message, double seconds) {
        super(eventType);
        this.message = message;
        this.seconds = seconds;
    }

    public String getMessage() {
        return message;
    }

    public double getSeconds() {
        return seconds;
    }
}
