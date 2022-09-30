package org.ton.ui.custom.events;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CustomEventBus {

    static Map<Class, List<CustomEventListener>> listeners = new HashMap<>();

    public static void emit(CustomEvent event) {
        Class eventClass = event.getClass();
        List<CustomEventListener> eventListeners = listeners.get(eventClass);
        for (CustomEventListener eventListener : eventListeners) {
            eventListener.handle(event);
        }

    }

    public static <T extends CustomEvent> void listenFor(Class<T> eventClass, CustomEventListener<T> listener) {
        if(!listeners.containsKey(eventClass)) {
            listeners.put(eventClass, new LinkedList<>());
        }
        listeners.get(eventClass).add(listener);
    }
}
