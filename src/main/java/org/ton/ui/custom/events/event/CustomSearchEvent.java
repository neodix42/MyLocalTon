package org.ton.ui.custom.events.event;

import org.ton.ui.custom.events.CustomEvent;

public class CustomSearchEvent extends CustomEvent {

    private int size;
    private String accountAddr;

    public CustomSearchEvent(Type eventType) {
        super(eventType);
    }

    public CustomSearchEvent(Type eventType, int size) {
        super(eventType);
        this.size = size;
    }

    public CustomSearchEvent(Type eventType, int size, String accountAddr) {
        super(eventType);
        this.size = size;
        this.accountAddr = accountAddr;
    }

    public int getSize() {
        return size;
    }

    public String getAccountAddr() {
        return accountAddr;
    }
}
