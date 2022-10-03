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
        DIALOG_CREATE_CLOSE,
        DIALOG_SEND_CLOSE,
        DIALOG_YES_NO_CLOSE,
        SAVE_SETTINGS,
        START,
        SEARCH_SIZE_BLOCKS,
        SEARCH_SIZE_ACCOUNTS,
        SEARCH_SIZE_TXS,
        SEARCH_SIZE_ACCOUNTS_TXS,
        SEARCH_SHOW,
        SEARCH_CLEAR,
        SEARCH_REMOVE,
        ACCOUNTS_TXS_REMOVE,
        REFRESH,
        BLOCKCHAIN_READY,
        WALLETS_READY,
        INFO,
        SUCCESS,
        WARNING,
        ERROR;
    }
}
