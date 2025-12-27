package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.InetAddress; // <--- import important per trobar la teva ip
import java.util.ArrayList;
import java.util.List;

import controller.ScanController;
import model.ResultatHost;
import utils.JsonExporter;

public class MainFrame extends JFrame {

    private JTextField txtIp;
    private JButton btnStart, btnStop, btnExport;
    private JTable taula;
    private DefaultTableModel modelTaula;
    
    private ScanController controller;
    private List<ResultatHost> resultats;

    public MainFrame() {
        this.setTitle("Scanner Automatic - Grup Hamza");
        this.setSize(700, 500);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        this.resultats = new ArrayList<>();
        this.controller = new ScanController(this); 

        initPantalla();
    }

    private void initPantalla() {
        // --- ZONA DE DALT ---
        JPanel panelDalt = new JPanel();
        
        panelDalt.add(new JLabel("La teva Xarxa:"));
        
        // AQUI ESTA EL TRUC:
        // En lloc de posar text fix, crido a la funció que la busca sola
        String laMevaXarxa = trobarLaMevaXarxa(); 
        txtIp = new JTextField(laMevaXarxa, 15);
        panelDalt.add(txtIp);
        
        btnStart = new JButton("Escanear");
        btnStart.setBackground(Color.GREEN);
        
        btnStop = new JButton("Stop");
        btnStop.setBackground(Color.RED);
        btnStop.setForeground(Color.WHITE);

        // --- ACCIONS ---
        btnStart.addActionListener(e -> {
            String ipEscrita = txtIp.getText();
            
            // si l'usuari ha borrat el punt, l'arreglo
            if (!ipEscrita.endsWith(".")) {
                ipEscrita = ipEscrita + ".";
                txtIp.setText(ipEscrita); 
            }

            resultats.clear();
            modelTaula.setRowCount(0);
            
            // crido al teu controlador
            controller.escanearRang(ipEscrita);
        });

        btnStop.addActionListener(e -> controller.aturar());

        panelDalt.add(btnStart);
        panelDalt.add(btnStop);
        this.add(panelDalt, BorderLayout.NORTH);

        // --- ZONA DEL MIG ---
        String[] titols = {"IP Adreça", "Estat", "Ports Oberts"};
        modelTaula = new DefaultTableModel(titols, 0);
        taula = new JTable(modelTaula);
        
        this.add(new JScrollPane(taula), BorderLayout.CENTER);

        // --- ZONA DE BAIX ---
        JPanel panelBaix = new JPanel();
        btnExport = new JButton("Guardar JSON");
        
        btnExport.addActionListener(e -> {
            boolean ok = JsonExporter.saveToJSON(resultats, "resultats.json");
            if (ok) JOptionPane.showMessageDialog(this, "Guardat!");
            else JOptionPane.showMessageDialog(this, "Error!");
        });
        
        panelBaix.add(btnExport);
        this.add(panelBaix, BorderLayout.SOUTH);
    }

    // --- METODE NOU: AUTO DETECTAR IP ---
    private String trobarLaMevaXarxa() {
        try {
            // pregunto al sistema qui soc
            InetAddress jo = InetAddress.getLocalHost();
            String laMevaIp = jo.getHostAddress(); // ex: 192.168.1.33
            
            // vull treure l'ultim numero per tenir la xarxa
            // busco on esta l'ultim punt
            int ultimPunt = laMevaIp.lastIndexOf(".");
            
            // tallo l'string fins al punt
            // em quedara "192.168.1."
            return laMevaIp.substring(0, ultimPunt + 1);
            
        } catch (Exception e) {
            // si falla per lo que sigui, poso la tipica
            return "192.168.1.";
        }
    }

    public synchronized void afegirResultat(ResultatHost host) {
        resultats.add(host);
        Object[] fila = {
            host.getIp(),
            "ONLINE",
            host.getPortsOberts().toString()
        };
        modelTaula.addRow(fila);
    }
}