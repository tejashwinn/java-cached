package com.tejashwin.javacached.client.run;

import com.tejashwin.javacached.core.exception.StaticClassInitException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaCachedApplicationClient {

  private JavaCachedApplicationClient() {
    throw new StaticClassInitException(JavaCachedApplicationClient.class.getName());
  }

  public static void run(String[] args) {
    log.info("Started java cached client");
    int port = 6379;
    try {
      Socket socket = new Socket("127.0.0.1", port);
      doSomething(socket);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  private static void doSomething(Socket socket) {
    Arrays.stream(new String[] {"Hello!", "Hi!", "Adios!"}).forEach(e -> sendRequest(socket, e));
  }

  private static void sendRequest(Socket socket, String message) {
    try {
      ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
      outputStream.writeObject(message);
      ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
      String str = (String) inputStream.readObject();
      log.info("Server({}) says: {}", socket.getInetAddress(), str);
      outputStream.close();
      inputStream.close();
    } catch (IOException | ClassNotFoundException e) {
      log.error(e.getMessage(), e);
    }
  }
}
