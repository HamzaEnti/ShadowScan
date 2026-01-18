package model;

import java.time.LocalDateTime;

// Classe pare abstracta per tots els tipus de resultats d'escaneig
// L'he feta abstracta perque no te sentit crear un resultat generic
public abstract class AbstractResultat {
    
    protected LocalDateTime dataEscaneig;  
    protected EstatResultat estat;         
    

    // Cada vegada que creem un resultat, automàticament agafa la data/hora actual
    public AbstractResultat() {
        this.dataEscaneig = LocalDateTime.now();  
        this.estat = EstatResultat.PENDENT;        
    }

    public LocalDateTime getDataEscaneig() {
        return dataEscaneig;
    }
    
    public EstatResultat getEstat() {
        return estat;
    }
    
    public void setEstat(EstatResultat estat) {
        this.estat = estat;
    }
    
    public abstract String toDisplayString();

    public abstract String toJson();
}