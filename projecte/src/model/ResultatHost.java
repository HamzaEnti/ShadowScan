package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import utils.JsonUtil;

// Classe que representa el resultat d'escanejar un host concret de la xarxa
public class ResultatHost extends AbstractResultat {

    private String ip;
    private String hostname;
    private boolean esViu;
    // LinkedHashSet: deduplicació O(1) i ordre d'inserció estable
    private final LinkedHashSet<Integer> portsOberts;

    public ResultatHost(String ip) {
        super();
        this.ip = ip;
        this.hostname = null;
        this.esViu = false;
        this.portsOberts = new LinkedHashSet<>();
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public boolean isEsViu() {
        return esViu;
    }

    // Setter especial per esViu: actualitza l'estat heretat del pare
    public void setEsViu(boolean esViu) {
        this.esViu = esViu;
        this.setEstat(esViu ? EstatResultat.ONLINE : EstatResultat.OFFLINE);
    }

    /**
     * Retorna una còpia immutable dels ports oberts en ordre d'inserció.
     */
    public List<Integer> getPortsOberts() {
        return new ArrayList<>(portsOberts);
    }

    /**
     * Substitueix els ports actuals per una nova col·lecció.
     * Accepta qualsevol Collection (List, Set...) i deduplicat automàticament.
     */
    public void setPortsOberts(Collection<Integer> ports) {
        this.portsOberts.clear();
        if (ports != null) {
            for (Integer p : ports) {
                if (p != null) this.portsOberts.add(p);
            }
        }
    }

    /** Afegeix un port; deduplicació O(1). */
    public void afegirPort(int port) {
        this.portsOberts.add(port);
    }

    /**
     * Categoria de risc d'aquest host. La classificació viu al model,
     * compartida entre vista, exportadors i tests.
     */
    public RiskLevel getRiskLevel() {
        return RiskLevel.classificar(portsOberts);
    }

    @Override
    public String toDisplayString() {
        String estatStr = esViu ? "ONLINE" : "OFFLINE";
        String hn = (hostname != null && !hostname.equals(ip)) ? " (" + hostname + ")" : "";
        return ip + hn + " - " + estatStr + " - Ports: " + portsOberts;
    }

    /**
     * Serialització JSON segura. Tots els camps de tipus String passen per
     * JsonUtil.escape, i els ports s'escriuen via JsonUtil.intArray. Així
     * un hostname amb cometes o caràcters de control no trenca la sortida.
     */
    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"ip\":\"").append(JsonUtil.escape(ip)).append("\", ");
        sb.append("\"hostname\":");
        if (hostname == null) {
            sb.append("null, ");
        } else {
            sb.append("\"").append(JsonUtil.escape(hostname)).append("\", ");
        }
        sb.append("\"esViu\":").append(esViu).append(", ");
        sb.append("\"estat\":\"").append(JsonUtil.escape(String.valueOf(getEstat()))).append("\", ");
        sb.append("\"risc\":\"").append(getRiskLevel().name()).append("\", ");
        sb.append("\"ports\":").append(JsonUtil.intArray(portsOberts));
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
