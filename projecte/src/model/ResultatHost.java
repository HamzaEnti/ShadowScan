package model;

import java.util.ArrayList;
import java.util.List;

// Classe que representa el resultat d'escanejar un host concret de la xarxa
public class ResultatHost extends AbstractResultat {
    

    private String ip;                      
    private boolean esViu;                 
    private List<Integer> portsOberts;      

  
    public ResultatHost(String ip) {
        super(); 
        this.ip = ip;
        this.esViu = false;  
        this.portsOberts = new ArrayList<>();  
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isEsViu() {
        return esViu;
    }

    // Setter especial per esViu: quan marquem si esta viu o no,
    // automàticament actualitzem l'estat heretat del pare
    // Fet amb IA
    public void setEsViu(boolean esViu) {
        this.esViu = esViu;
        if (esViu) {
            this.setEstat(EstatResultat.ONLINE);   
        } else {
            this.setEstat(EstatResultat.OFFLINE);  
        }
    } // Fi IA

    public List<Integer> getPortsOberts() {
        return portsOberts;
    }

    public void setPortsOberts(List<Integer> portsOberts) {
        this.portsOberts = portsOberts;
    }
    
    // Metode auxiliar per afegir ports un a un
    public void afegirPort(int port) {
        if (!this.portsOberts.contains(port)) {  
            this.portsOberts.add(port);
        }
    }

    @Override
    public String toDisplayString() {
        String estatStr = esViu ? "ONLINE" : "OFFLINE";  
        return ip + " - " + estatStr + " - Ports: " + portsOberts;
    }
    
    // Implementacio obligatoria per exportar a JSON
    // Utilitzem StringBuilder per anar construint el JSON manualment
    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"ip\":\"").append(ip).append("\", ");         
        sb.append("\"esViu\":").append(esViu).append(", ");        
        sb.append("\"estat\":\"").append(getEstat()).append("\", ");  
        sb.append("\"ports\":").append(portsOberts);              
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}