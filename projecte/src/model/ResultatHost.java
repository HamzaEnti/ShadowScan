package model;

import java.util.ArrayList;
import java.util.List;

public class ResultatHost {

    private String ip;
    private boolean esViu;
    private List<Integer> portsOberts;

    // Constructor
    public ResultatHost(String _ip) {
        this.ip = _ip;
        this.esViu = false;
        this.portsOberts = new ArrayList<>();
    }

    // Getters y setters
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isEsViu() {
        return esViu;
    }

    public void setEsViu(boolean esViu) {
        this.esViu = esViu;
    }

    public List<Integer> getPortsOberts() {
        return portsOberts;
    }

    public void setPortsOberts(List<Integer> portsOberts) {
        this.portsOberts = portsOberts;
    }

    // toString llegible
    public String toString() {
        String estat = esViu ? "ONLINE" : "OFFLINE";
        return ip + " - " + estat + " - Ports: " + portsOberts;
    }
}
