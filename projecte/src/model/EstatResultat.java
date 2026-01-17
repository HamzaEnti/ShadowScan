package model;

// Enum per representar els diferents estats d'un resultat d'escaneig
public enum EstatResultat {
    
    PENDENT("Pendent"),          
    ONLINE("En linia"),         
    OFFLINE("Fora de linia"),     
    ERROR("Error");               
    
    // Atribut que guarda la descripcio llegible de cada estat
    private final String descripcio;
    
    EstatResultat(String descripcio) {
        this.descripcio = descripcio;
    }
    
    public String getDescripcio() {
        return descripcio;
    }
    
    @Override
    public String toString() {
        return descripcio;
    }
}