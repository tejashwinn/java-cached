package com.tejashwin.javacached.server.run;

import com.tejashwin.javacached.core.exception.StaticClassInitException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaCachedApplicationServer {

  private JavaCachedApplicationServer() {
    throw new StaticClassInitException(JavaCachedApplicationServer.class.getName());
  }

  public static void run(String[] args) {
    log.info("Started java cached");
    int port = 6379;
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
      do {
        try {
          // Wait for connection from client.
          Socket clientSocket = serverSocket.accept();
          doSomething(clientSocket);
          clientSocket.close();
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      } while (true);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void doSomething(Socket socket) {
    try {
      ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
      String str = (String) inputStream.readObject();
      log.info("Client({}) says: {}", socket.getInetAddress(), str);
      ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
      outputStream.writeObject("Hello!");
      inputStream.close();
      outputStream.close();
    } catch (IOException | ClassNotFoundException e) {
      log.error(e.getMessage(), e);
    }
  }
}
