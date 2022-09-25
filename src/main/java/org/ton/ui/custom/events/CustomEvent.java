package org.ton.ui.custom.events;

public abstract class CustomEvent {

    private Type eventType;

    public CustomEvent(Type eventType) {
        this.eventType = eventType;
    }

    public Type getEventType() {
        return eventType;
    }

    public enum Type {
        CLICK,
        SWITCH,
        CHANGE,
        DIALOG_SEND_CLOSE,
        DIALOG_RESET_CLOSE,
        SAVE_SETTINGS,
        START,
        INFO,
        SUCCESS,
        WARNING,
        ERROR;
    }
}
