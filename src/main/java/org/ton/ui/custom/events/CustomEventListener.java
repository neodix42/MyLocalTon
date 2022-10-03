package org.ton.ui.custom.events;

public interface CustomEventListener<T extends CustomEvent> {

    void handle(T event);
}
