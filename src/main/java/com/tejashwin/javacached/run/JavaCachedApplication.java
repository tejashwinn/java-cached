package com.tejashwin.javacached.run;

import com.tejashwin.javacached.exception.StaticClassInitException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaCachedApplication {

  private JavaCachedApplication() {
    throw new StaticClassInitException(JavaCachedApplication.class.getName());
  }

  public static void run(String[] args) {
    log.info("Started java cached");
    int port = 6379;
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
      // Wait for connection from client.
      Socket clientSocket = serverSocket.accept();

    } catch (IOException e) {
      log.error(e.getMessage(), e);
    } finally {
      log.info("Stopped java cached");
    }
  }
}
