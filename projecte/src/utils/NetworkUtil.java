package utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkUtil {
  //comprovem si una ip és reachable amb timeout
  public static boolean isReachable(String ip, int timeout) {
    try {
      InetAddress address = InetAddress.getByName(ip);
      return address.isReachable(timeout);
    } catch (IOException e) {
      return false;
    }
  }

  //comprovem si un port està obert en una ip amb timeout
  public static boolean isPortOpen(String ip, int port, int timeout) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(ip, port), timeout);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
