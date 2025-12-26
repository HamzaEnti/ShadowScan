package utils;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;


public class NetworkUtil {
    

    public static boolean isReachable(String ip, int timeout){
        try{
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(timeout);
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean isPortOpen(String ip, int port, int timeout){
        try(Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip,port), timeout);
            return true;
        } catch (IOException e){
            return false;
        }
    }
}
