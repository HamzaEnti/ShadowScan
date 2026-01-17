package controller;

public enum PortScanMode {
    
    // mode rapid: nomes els ports mes comuns
    // son els que normalment trobaras en un servidor tipic
    PARCIAL("Escaneig rapid", new int[]{
        21,     // FTP
        22,     // SSH  
        23,     // Telnet
        25,     // SMTP
        53,     // DNS
        80,     // HTTP
        110,    // POP3
        139,    // NetBIOS
        143,    // IMAP
        443,    // HTTPS
        445,    // SMB
        3306,   // MySQL
        3389,   // RDP
        8080    // HTTP alternatiu
    }),
    
    // mode complet: escaneja tot
    // aviso: tarda una estona considerable
    FULL("Escaneig complet", null);
    
  
    private final String descripcio;
    private final int[] ports;
    
    PortScanMode(String descripcio, int[] ports) {
        this.descripcio = descripcio;
        this.ports = ports;
    }
    
    public String getDescripcio() {
        return descripcio;
    }
    
    
    public int[] getPorts() {
        return ports;
    }
    
    // metode per saber si es mode parcial
    public boolean esParcial() {
        return this == PARCIAL;
    }
}
