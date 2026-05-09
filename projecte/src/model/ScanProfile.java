package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import utils.JsonParser;
import utils.JsonUtil;

/**
 * Perfil d'escaneig persistent: agrupa configuració reutilitzable per a
 * un context concret (xarxa interna, DMZ, escaneig ràpid d'inventari, etc.).
 *
 * Es serialitza com a JSON pla:
 * <pre>
 * {
 *   "name": "DMZ ràpida",
 *   "startIp": "10.0.1.1",
 *   "endIp":   "10.0.1.254",
 *   "mode":    "PARCIAL",
 *   "udp":     false,
 *   "ports":   [22, 80, 443]
 * }
 * </pre>
 */
public class ScanProfile {

    private String name;
    private String startIp;
    private String endIp;
    private String mode;            // PARCIAL / FULL / CUSTOM
    private boolean udp;
    private final List<Integer> customPorts;

    public ScanProfile(String name, String startIp, String endIp, String mode) {
        this.name = name;
        this.startIp = startIp;
        this.endIp = endIp;
        this.mode = mode;
        this.udp = false;
        this.customPorts = new ArrayList<>();
    }

    public String getName() { return name; }
    public String getStartIp() { return startIp; }
    public String getEndIp() { return endIp; }
    public String getMode() { return mode; }
    public boolean isUdp() { return udp; }
    public List<Integer> getCustomPorts() { return Collections.unmodifiableList(customPorts); }

    public void setName(String n) { this.name = n; }
    public void setStartIp(String s) { this.startIp = s; }
    public void setEndIp(String s) { this.endIp = s; }
    public void setMode(String m) { this.mode = m; }
    public void setUdp(boolean u) { this.udp = u; }

    public void setCustomPorts(List<Integer> ports) {
        this.customPorts.clear();
        if (ports != null) for (Integer p : ports) if (p != null) this.customPorts.add(p);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"").append(JsonUtil.escape(name)).append("\",");
        sb.append("\"startIp\":\"").append(JsonUtil.escape(startIp)).append("\",");
        sb.append("\"endIp\":\"").append(JsonUtil.escape(endIp)).append("\",");
        sb.append("\"mode\":\"").append(JsonUtil.escape(mode)).append("\",");
        sb.append("\"udp\":").append(udp).append(",");
        sb.append("\"ports\":").append(JsonUtil.intArray(customPorts));
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static ScanProfile fromJson(String src) {
        Map<String, Object> m = JsonParser.parseObject(src);
        ScanProfile p = new ScanProfile(
            (String) m.getOrDefault("name", "(sense nom)"),
            (String) m.getOrDefault("startIp", ""),
            (String) m.getOrDefault("endIp", ""),
            (String) m.getOrDefault("mode", "PARCIAL")
        );
        Object udp = m.get("udp");
        if (udp instanceof Boolean) p.setUdp((Boolean) udp);
        Object ports = m.get("ports");
        if (ports instanceof List) {
            List<Integer> ip = new ArrayList<>();
            for (Object v : (List<Object>) ports) {
                if (v instanceof Number) ip.add(((Number) v).intValue());
            }
            p.setCustomPorts(ip);
        }
        return p;
    }

    @Override
    public String toString() {
        return name + " [" + startIp + "→" + endIp + ", " + mode + (udp ? "+UDP" : "") + "]";
    }
}
