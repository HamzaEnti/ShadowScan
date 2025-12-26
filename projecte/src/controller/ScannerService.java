package controller;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ScannerService {

    // Temps d'espera (en ms). Si va lent, pujar a 500
    private int timeout = 200;

    // Comprova si la maquina respon al Ping
    public boolean isReachable(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            // isReachable de Java a vegades falla si no ets admin, pero per xarxa local serveix
            return address.isReachable(timeout);
        } catch (Exception e) {
            return false;
        }
    }

    // Intenta connectar a un port concret (TCP)
    public boolean isPortOpen(String ip, int port) {
        Socket socket = new Socket();
        try {
            // Intentem connectar. Si passa del temps, salta al catch
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } catch (Exception e) {
            return false; // Esta tancat o filtrat
        }
    }

    // Metode per mirar els ports tipics d'una tirada
    // Aixo ho fare servir despres per passar-li la info al Nico
    public List<Integer> escanejarPortsComuns(String ip) {
        List<Integer> oberts = new ArrayList<>();
        // 21:FTP, 22:SSH, 80:Web, 443:HTTPS, 3306:MySQL, 8080:Alternativa web
        int[] ports = {21, 22, 80, 443, 3306, 8080}; 

        for (int p : ports) {
            if (isPortOpen(ip, p)) {
                oberts.add(p);
            }
        }
        return oberts;
    }
}



