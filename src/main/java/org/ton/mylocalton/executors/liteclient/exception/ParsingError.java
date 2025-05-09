package org.ton.mylocalton.executors.liteclient.exception;

public class ParsingError extends Exception {
  public ParsingError(String errorMessage, Throwable err) {
    super(errorMessage, err);
  }

  public ParsingError(String errorMessage) {
    super(errorMessage);
  }
}
