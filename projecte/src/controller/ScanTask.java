package controller;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities; // necessari per tocar la interficie

import model.ResultatHost;
import utils.NetworkUtil;
import view.MainFrame; // la vista de l'oscar

public class ScanTask implements Runnable {

    private String ip;
    private MainFrame vista; // variable per guardar la finestra

    // constructor actualitzat
    public ScanTask(String ip, MainFrame v) {
        this.ip = ip;
        this.vista = v;
    }

    @Override
    public void run() {
        if (NetworkUtil.isReachable(ip, 200)) {
            
            ResultatHost host = new ResultatHost(ip);
            host.setEsViu(true);
            
            // Aqui podrem afegir molts ports a provar
            // 21:ftp, 22:ssh, 23:telnet, 80:web, 443:https
            // 3306:mysql, 5432:postgres, 3389:escriptori remot windows, 8080:tomcat/web
            int[] llistaPorts = {21, 22, 23, 80, 443, 3306, 3389, 5432, 8080};
            List<Integer> portsTrobats = new ArrayList<>();

            for (int port : llistaPorts) {
                if (NetworkUtil.isPortOpen(ip, port, 200)) {
                    portsTrobats.add(port);
                }
            }
            
            host.setPortsOberts(portsTrobats);

            // ZONA VISUAL: avisem a la pantalla que hem trobat algo
            // es fa amb invokeLater perque swing no peti amb els fils
            SwingUtilities.invokeLater(() -> {
                vista.afegirResultat(host);
            });
            
            System.out.println("trobat: " + ip);
        }
    }
}