package com.tejashwin.javacached;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class JavaCached {

    public static void main(String[] args) {
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
