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
            
            // Bucle de 1 a 65535 per afegir tots els ports possibles i saber si estan oberts o no
            List<Integer> portsTrobats = new ArrayList<>();

            for (int port = 1; port <= 65535; port++) {
                // He posat 20ms de timeout perque vagi rapid amb tants ports
                if (NetworkUtil.isPortOpen(ip, port, 20)) {
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