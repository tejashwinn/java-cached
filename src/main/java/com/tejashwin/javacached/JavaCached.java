package com.tejashwin.javacached;

import com.tejashwin.javacached.exception.StaticClassInitException;
import com.tejashwin.javacached.run.JavaCachedApplication;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaCached {

  private JavaCached() {
    throw new StaticClassInitException(JavaCached.class.getName());
  }

  public static void main(String[] args) {
    JavaCachedApplication.run(args);
  }
}
