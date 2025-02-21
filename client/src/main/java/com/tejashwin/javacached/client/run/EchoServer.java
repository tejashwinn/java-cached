package com.tejashwin.javacached.client.run;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class EchoServer {

  private static final int K_MAX_MSG = 32 << 20; // 32MB
  private static final int SERVER_PORT = 6379;

  static void msg(String msg) {
    System.err.println(msg);
  }

  static void msgErrno(String msg) {
    System.err.println(msg); // In Java, exceptions provide error info.  Simplified here.
  }

  static void die(String msg) {
    System.err.println(msg);
    System.exit(1); // Equivalent of abort()
  }

  static void fdSetNb(SocketChannel fd) {
    try {
      fd.configureBlocking(false);
    } catch (IOException e) {
      die("Could not set non-blocking mode: " + e.getMessage());
    }
  }

  static Conn handleAccept(ServerSocketChannel serverSocket) {
    try {
      SocketChannel connfd = serverSocket.accept();
      if (connfd == null) {
        return null; // non-blocking, no connection available
      }

      SocketAddress clientAddress = connfd.getRemoteAddress();
      if (clientAddress instanceof InetSocketAddress) {
        InetSocketAddress inetAddress = (InetSocketAddress) clientAddress;
        InetAddress clientIpAddress = inetAddress.getAddress();
        int port = inetAddress.getPort();
        msg("New client from " + clientIpAddress.getHostAddress() + ":" + port);
      } else {
        msg("New client from unknown address");
      }

      fdSetNb(connfd);

      Conn conn = new Conn();
      conn.fd = connfd;
      conn.wantRead = true;
      return conn;

    } catch (IOException e) {
      msgErrno("Accept error: " + e.getMessage());
      return null;
    }
  }

  static boolean tryOneRequest(Conn conn) {
    conn.incoming.flip(); // Prepare for reading
    if (conn.incoming.remaining() < 4) {
      conn.incoming.compact(); // Prepare for writing, retain unprocessed data
      return false; // Want read
    }

    int len = conn.incoming.getInt();

    if (len > K_MAX_MSG || len < 0) { // Len < 0 protects from malicious clients
      msg("Too long or negative length");
      conn.wantClose = true;
      conn.incoming.compact(); // Prepare for writing
      return false; // Want close
    }

    if (conn.incoming.remaining() < len) {
      conn.incoming.rewind(); // Reset position to read length again next time
      conn.incoming.compact(); // Prepare for writing, retain unprocessed data
      return false; // Want read
    }

    byte[] request = new byte[len];
    conn.incoming.get(request, 0, len);

    // Application logic
    String clientMessage = new String(request);
    System.out.println(
        "Client says: len:"
            + len
            + " data:"
            + (len < 100 ? clientMessage : clientMessage.substring(0, 100) + "..."));

    // Generate response (echo)
    conn.outgoing.putInt(len);
    conn.outgoing.put(request, 0, len);

    // Clear 'incoming' buffer by compacting
    ByteBuffer temp = conn.incoming;
    conn.incoming = ByteBuffer.allocate(K_MAX_MSG);
    conn.incoming.put(temp);

    conn.outgoing.flip(); // Prepare outgoing buffer for sending
    return true; // Success
  }

  static void handleWrite(Conn conn) {
    try {
      if (conn.outgoing.remaining() > 0) {
        int rv = conn.fd.write(conn.outgoing);
        if (rv < 0) {
          msgErrno("Write error");
          conn.wantClose = true;
          return;
        }
        if (conn.outgoing.remaining() == 0) {
          conn.wantRead = true;
          conn.wantWrite = false;
          conn.outgoing.clear(); // reset outgoing buffer
        }
      } else {
        conn.wantWrite = false; // nothing to write
        conn.wantRead = true;
      }
    } catch (IOException e) {
      msgErrno("Write error: " + e.getMessage());
      conn.wantClose = true;
    }
  }

  static void handleRead(Conn conn) {
    try {
      if (conn.incoming.position() == conn.incoming.limit()) {
        conn.incoming.clear();
      }
      int rv = conn.fd.read(conn.incoming);

      if (rv < 0) {
        msgErrno("Read error");
        conn.wantClose = true;
        return;
      }

      if (rv == 0) {
        if (conn.incoming.position() == 0) {
          msg("Client closed");
        } else {
          msg("Unexpected EOF");
        }
        conn.wantClose = true;
        return;
      }

      conn.incoming.flip();
      while (tryOneRequest(conn)) {}

      if (conn.outgoing.position() > 0) {
        conn.wantRead = false;
        conn.wantWrite = true;
        handleWrite(conn);
      } else {
        conn.wantRead = true;
      }
    } catch (IOException e) {
      msgErrno("Read error: " + e.getMessage());
      conn.wantClose = true;
    }
  }

  public static void main(String[] args) {
    List<Conn> fd2conn = new ArrayList<>();

    try {
      // the listening socket
      ServerSocketChannel serverSocket = ServerSocketChannel.open();
      serverSocket.socket().bind(new InetSocketAddress(SERVER_PORT));
      fdSetNb(serverSocket.accept());

      Selector selector = Selector.open();
      serverSocket.register(selector, SelectionKey.OP_ACCEPT);

      msg("Server started, listening on port " + SERVER_PORT);

      while (true) {
        selector.select(); // Blocking wait for event
        Set<SelectionKey> keys = selector.selectedKeys();

        for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {
          SelectionKey key = it.next();
          it.remove();


          try {
            if (key.isValid() && key.isAcceptable()) {
              Conn conn = handleAccept(serverSocket);
              if (conn != null) {
                conn.fd.register(selector, SelectionKey.OP_READ, conn);
                fd2conn.add(conn);
              }
            } else if (key.isValid() && key.isReadable()) {
              Conn conn = (Conn) key.attachment();
              handleRead(conn);
              if (conn.wantWrite) {
                key.interestOps(SelectionKey.OP_WRITE);
              } else if (conn.wantClose) {
                key.cancel();
                conn.fd.close();
                fd2conn.remove(conn);
              } else {
                key.interestOps(SelectionKey.OP_READ);
              }
            } else if (key.isValid() && key.isWritable()) {
              Conn conn = (Conn) key.attachment();
              handleWrite(conn);

              if (conn.wantRead) {
                key.interestOps(SelectionKey.OP_READ);
              } else if (conn.wantClose) {
                key.cancel();
                conn.fd.close();
                fd2conn.remove(conn);
              } else {
                key.interestOps(SelectionKey.OP_WRITE);
              }
            }
          } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
            key.cancel();
            SelectableChannel ch = key.channel();
            try {
              ch.close();
            } catch (IOException ex) {
              System.err.println("Error closing channel: " + ex.getMessage());
            }
            Conn conn = (Conn) key.attachment();
            if (conn != null) {
              fd2conn.remove(conn);
            }
          }
        }
      }
    } catch (IOException e) {
      die("Fatal error: " + e.getMessage());
    }
  }

  static class Conn {
    SocketChannel fd;
    boolean wantRead = false;
    boolean wantWrite = false;
    boolean wantClose = false;
    ByteBuffer incoming = ByteBuffer.allocate(K_MAX_MSG); // Data to be parsed
    ByteBuffer outgoing = ByteBuffer.allocate(K_MAX_MSG); // Responses
  }
}
