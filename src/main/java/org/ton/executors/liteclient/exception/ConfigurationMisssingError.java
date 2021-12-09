package org.ton.executors.liteclient.exception;

public class ConfigurationMisssingError extends Exception {
    public ConfigurationMisssingError(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ConfigurationMisssingError(String errorMessage) {
        super(errorMessage);
    }
}

