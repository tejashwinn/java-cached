package com.tejashwin.javacached.exception;

public class StaticClassInitException extends RuntimeException {

  public StaticClassInitException(String clazzName) {
    throw new IllegalStateException("Cannot create object for " + clazzName);
  }
}
