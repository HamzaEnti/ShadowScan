package utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class NetworkUtil {

    /** Ports usats com a fallback TCP quan ICMP està bloquejat. */
    private static final int[] FALLBACK_PORTS = {80, 443, 22, 445, 3389, 139};

    /**
     * Comprova si una IP respon a ICMP.
     *
     * Atenció: en molts sistemes (Windows sense privilegis, hosts amb
     * firewall, contenidors sense capabilities) això retorna false fins i
     * tot per hosts ben actius. Cal complementar amb {@link #isHostAlive}.
     */
    public static boolean isReachable(String ip, int timeout) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(timeout);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Detecció robusta: intenta primer ICMP i, si falla, prova de connectar
     * a una llista curta de ports comuns. Resol el problema de hosts amb
     * ping bloquejat que apareixien com a "morts" tot i tenir serveis oberts.
     */
    public static boolean isHostAlive(String ip, int icmpTimeout, int tcpTimeout) {
        if (isReachable(ip, icmpTimeout)) return true;
        for (int port : FALLBACK_PORTS) {
            if (isPortOpen(ip, port, tcpTimeout)) return true;
        }
        return false;
    }

    /** Comprova si un port està obert en una IP amb timeout. */
    public static boolean isPortOpen(String ip, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Resol el hostname d'una IP. Usa getCanonicalHostName que pot ser lent
     * en xarxes sense PTR; els callers haurien de fer-ho fora del thread UI.
     * Si no es pot resoldre, retorna la pròpia IP.
     */
    public static String resolveHostname(String ip) {
        if (ip == null || ip.isEmpty()) return ip;
        try {
            String name = InetAddress.getByName(ip).getCanonicalHostName();
            return (name == null || name.isEmpty()) ? ip : name;
        } catch (UnknownHostException e) {
            return ip;
        }
    }

    /**
     * Genera la llista d'IPs entre dues adreces IPv4 (inclusiu).
     *
     * Suporta rangs personalitzats com 192.168.1.10 → 192.168.1.50, o
     * subxarxes diferents de /24 com 10.0.0.1 → 10.0.255.254.
     *
     * @return llista d'IPs en ordre, o llista buida si l'entrada és invàlida
     */
    public static List<String> rangIps(String startIp, String endIp) {
        List<String> ips = new ArrayList<>();
        long ini = ipToLong(startIp);
        long fi  = ipToLong(endIp);
        if (ini < 0 || fi < 0 || fi < ini) return ips;
        // Límit de seguretat: evitem rangs absurds (>65k hosts)
        if (fi - ini > 65535) return ips;
        for (long n = ini; n <= fi; n++) {
            ips.add(longToIp(n));
        }
        return ips;
    }

    /** Converteix una IPv4 en un long sense signe. -1 si és invàlida. */
    public static long ipToLong(String ip) {
        if (ip == null) return -1;
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) return -1;
        long result = 0;
        for (String part : parts) {
            try {
                int n = Integer.parseInt(part);
                if (n < 0 || n > 255) return -1;
                result = (result << 8) | n;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return result;
    }

    public static String longToIp(long n) {
        return ((n >> 24) & 0xFF) + "." + ((n >> 16) & 0xFF) + "."
             + ((n >> 8)  & 0xFF) + "." + ( n        & 0xFF);
    }
}
