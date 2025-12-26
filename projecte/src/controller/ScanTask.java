package controller;

import java.util.ArrayList;
import java.util.List;
import model.ResultatHost;
import utils.NetworkUtil; 

public class ScanTask implements Runnable {

    private String ip;

    public ScanTask(String ip) {
        this.ip = ip;
    }

    @Override
    public void run() {
        // primer miro si hi ha ping (timeout 200ms)
        if (NetworkUtil.isReachable(ip, 200)) {
            
            // si respon creo l'objecte
            ResultatHost host = new ResultatHost(ip);
            host.setEsViu(true);
            
            // Aqui podrem afegir molts ports a provar
            // 21:ftp, 22:ssh, 23:telnet, 80:web, 443:https
            // 3306:mysql, 5432:postgres, 3389:escriptori remot windows, 8080:tomcat/web
            int[] llistaPorts = {21, 22, 23, 80, 443, 3306, 3389, 5432, 8080};
            
            List<Integer> portsTrobats = new ArrayList<>();

            // provo un per un amb la utilitat del nico
            for (int port : llistaPorts) {
                // li poso 200ms de timeout, si poses mes anira mes lent
                if (NetworkUtil.isPortOpen(ip, port, 200)) {
                    portsTrobats.add(port);
                }
            }
            
            // els guardo
            host.setPortsOberts(portsTrobats);

            System.out.println("host trobat: " + host.toString());
        }
    }
}