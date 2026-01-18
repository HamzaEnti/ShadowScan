package services;

import model.Scannable;

public abstract class AbstractScanService implements Scannable {
    
    protected String nomEina;
    
    public AbstractScanService(String nom) {
        this.nomEina = nom;
    }
    
    @Override
    public String getNomEina() {
        return this.nomEina;
    }
    
    protected void log(String missatge) {
        System.out.println(">>> [" + nomEina + "] " + missatge);
    }
    
  
    protected void logError(String missatge) {
        System.err.println(">>> [ERROR " + nomEina + "] " + missatge);
    }
    
   
    protected boolean fitxerExisteix(String ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return false;
        }
        java.io.File f = new java.io.File(ruta);
        return f.exists() && f.isFile();
    }
    
   
    @Override
    public abstract boolean checkInstalled();
    
    @Override
    public abstract void executar(String target, int port);
}
