package org.ton.ui.custom.events.event;

import org.ton.ui.custom.events.CustomEvent;

public class CustomActionEvent implements CustomEvent {

    public enum Type {
        CLICK,
        SWITCH,
        CHANGE;
    }

    private Type eventType;

    public CustomActionEvent(Type eventType) {
        this.eventType = eventType;
    }

    public Type getEventType() {
        return eventType;
    }
}
