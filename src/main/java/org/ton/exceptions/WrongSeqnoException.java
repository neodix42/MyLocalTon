package org.ton.exceptions;

public class WrongSeqnoException extends Exception {
    public WrongSeqnoException(String errorMessage) {
        super(errorMessage);
    }
}
