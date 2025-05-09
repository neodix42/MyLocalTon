package org.ton.mylocalton.executors.liteclient.exception;

public class IncompleteDump extends Exception {
  public IncompleteDump(String errorMessage, Throwable err) {
    super(errorMessage, err);
  }

  public IncompleteDump(String errorMessage) {
    super(errorMessage);
  }
}
