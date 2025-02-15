package com.tejashwin.javacached.client;

import com.tejashwin.javacached.client.run.JavaCachedApplicationClient;
import com.tejashwin.javacached.core.exception.StaticClassInitException;

public class JavaCachedClient {

  private JavaCachedClient() {
    throw new StaticClassInitException(JavaCachedClient.class.getName());
  }

  public static void main(String[] args) {
    JavaCachedApplicationClient.run(args);
  }
}
