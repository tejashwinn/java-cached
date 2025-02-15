package com.tejashwin.javacached.server;

import com.tejashwin.javacached.core.exception.StaticClassInitException;
import com.tejashwin.javacached.server.run.JavaCachedApplicationServer;

public class JavaCachedServer {

  private JavaCachedServer() {
    throw new StaticClassInitException(JavaCachedServer.class.getName());
  }

  public static void main(String[] args) {
    JavaCachedApplicationServer.run(args);
  }
}
