package org.ton.mylocalton.ui.custom.events;

public interface CustomEventListener<T extends CustomEvent> {

  void handle(T event);
}
