package utils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

    /**
     * Genera un rang d'IPs detectant automàticament IPv4 vs IPv6.
     * Si les dues IPs no són de la mateixa família retorna llista buida i
     * loga l'error en lloc de fallar de manera obscura.
     */
    public static List<String> smartRange(String startIp, String endIp) {
        boolean s6 = isIPv6(startIp);
        boolean e6 = isIPv6(endIp);
        if (s6 != e6) {
            System.err.println(">>> [RANGE] Mescla d'IPv4 i IPv6 no suportada: "
                + startIp + " → " + endIp);
            return new ArrayList<>();
        }
        return s6 ? rangIpsV6(startIp, endIp) : rangIps(startIp, endIp);
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

    /* ─── Suport IPv6 ─────────────────────────────────────────────────── */

    public static boolean isIPv6(String addr) {
        if (addr == null) return false;
        try {
            return InetAddress.getByName(addr) instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Genera un rang d'IPs IPv6. Usa BigInteger perquè 128 bits no caben
     * en cap primitiu Java. Limitem a 4096 hosts per protegir-nos d'abusos.
     */
    public static List<String> rangIpsV6(String startIp, String endIp) {
        List<String> ips = new ArrayList<>();
        try {
            BigInteger ini = ipv6ToBigInt(startIp);
            BigInteger fi  = ipv6ToBigInt(endIp);
            if (ini == null || fi == null || fi.compareTo(ini) < 0) return ips;
            BigInteger diff = fi.subtract(ini);
            if (diff.compareTo(BigInteger.valueOf(4096)) > 0) return ips;
            for (BigInteger n = ini; n.compareTo(fi) <= 0; n = n.add(BigInteger.ONE)) {
                ips.add(bigIntToIpv6(n));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return ips;
    }

    public static BigInteger ipv6ToBigInt(String addr) {
        try {
            InetAddress a = InetAddress.getByName(addr);
            byte[] bytes = a.getAddress();
            if (bytes.length != 16) return null;
            return new BigInteger(1, bytes);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static String bigIntToIpv6(BigInteger n) {
        byte[] full = new byte[16];
        byte[] raw = n.toByteArray();
        // BigInteger pot afegir un byte de signe; copiem alineat a la dreta
        int len = Math.min(raw.length, 16);
        System.arraycopy(raw, raw.length - len, full, 16 - len, len);
        try {
            return InetAddress.getByAddress(full).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /* ─── Suport UDP ──────────────────────────────────────────────────── */

    /**
     * UDP scan amb heurística per defecte:
     *   - Enviem un datagrama buit (o magic bytes per a serveis coneguts)
     *   - Esperem la resposta
     *   - Sense resposta dins del timeout → "open|filtered" (UDP no garanteix retorn)
     *   - Excepció PortUnreachable (ICMP) → tancat
     *
     * Java no exposa ICMP directament, així que basem la detecció en si
     * arriba alguna resposta. Hi ha falsos positius — cal documentar-ho a
     * l'usuari. Retorna true en cas de "open" o "open|filtered".
     */
    public static boolean isUdpPortOpen(String ip, int port, int timeoutMs) {
        try (DatagramSocket s = new DatagramSocket()) {
            s.setSoTimeout(timeoutMs);
            InetAddress addr = InetAddress.getByName(ip);
            byte[] payload = udpProbe(port);
            s.send(new DatagramPacket(payload, payload.length, addr, port));
            byte[] buf = new byte[512];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            s.receive(resp);
            return true; // resposta rebuda → port obert segur
        } catch (SocketTimeoutException e) {
            // No resposta: UDP open|filtered, classifiquem com a probable obert
            return true;
        } catch (java.net.PortUnreachableException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * UDP estricte: només retorna true si rebem alguna resposta. Útil quan
     * volem reduir falsos positius a costa de perdre serveis silenciosos.
     */
    public static boolean isUdpPortRespondingStrict(String ip, int port, int timeoutMs) {
        try (DatagramSocket s = new DatagramSocket()) {
            s.setSoTimeout(timeoutMs);
            InetAddress addr = InetAddress.getByName(ip);
            byte[] payload = udpProbe(port);
            s.send(new DatagramPacket(payload, payload.length, addr, port));
            byte[] buf = new byte[512];
            s.receive(new DatagramPacket(buf, buf.length));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Probes minimalistes per a alguns ports UDP comuns. Per a la resta,
     * un payload buit (que la majoria de serveis ignoraran).
     */
    private static byte[] udpProbe(int port) {
        switch (port) {
            case 53: // DNS query: id=0x1234, qd=1, query "version.bind" CHAOS TXT
                return new byte[]{
                    0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x07, 'v','e','r','s','i','o','n', 0x04, 'b','i','n','d', 0x00,
                    0x00, 0x10, 0x00, 0x03
                };
            case 161: // SNMP GetRequest minimal (community="public")
                return new byte[]{
                    0x30, 0x26, 0x02, 0x01, 0x00, 0x04, 0x06, 'p','u','b','l','i','c',
                    (byte) 0xA0, 0x19, 0x02, 0x04, 0x71, 0x44, 0x42, 0x42,
                    0x02, 0x01, 0x00, 0x02, 0x01, 0x00, 0x30, 0x0B, 0x30, 0x09,
                    0x06, 0x05, 0x2B, 0x06, 0x01, 0x02, 0x01, 0x05, 0x00
                };
            default:
                return new byte[0];
        }
    }
}
