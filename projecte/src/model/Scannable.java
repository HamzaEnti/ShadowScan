package model;

public interface Scannable {
    
    boolean checkInstalled();
    void executar(String target, int port);
    String getNomEina();
}