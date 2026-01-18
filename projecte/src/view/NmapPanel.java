package view;

import java.awt.*;
import javax.swing.*;
import services.NmapWrapper;

//Panel per a l'escaneig amb Nmap.

public class NmapPanel extends BasePanel {
    
    // components
    private JTextField txtIp;
    private JButton btnExecutar;
    private JLabel lblEstat;
    
    // servei
    private NmapWrapper nmapService;
    
    public NmapPanel(MainFrame parent) {
        super(parent);
    }
    
    @Override
    protected void initComponents() {
        // inicialitzem el servei
        try {
            this.nmapService = new NmapWrapper();
        } catch (Exception e) {
            System.err.println(">>> [ERROR] No s'ha pogut crear NmapWrapper");
        }
        
        // layout amb GridBagLayout per centrar tot
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // titol
        JLabel lblTitol = crearTitol("Nmap Service Detector (-sV)");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        this.add(lblTitol, gbc);
        
        // camp IP
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        this.add(new JLabel("IP Objectiu:"), gbc);
        
        gbc.gridx = 1;
        txtIp = crearCampText(15);
        this.add(txtIp, gbc);
        
        // boto executar
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        btnExecutar = new JButton("EXECUTAR ANALISI");
        btnExecutar.setPreferredSize(new Dimension(200, 40));
        btnExecutar.setFont(FONT_BOTO);
        this.add(btnExecutar, gbc);
        
        // etiqueta d'estat
        gbc.gridy = 3;
        lblEstat = new JLabel("Estat: Esperant ordre...");
        this.add(lblEstat, gbc);
        
        // listener del boto
        btnExecutar.addActionListener(e -> executarNmap());
    }
    
    // Executa l'escaneig amb Nmap.
    private void executarNmap() {
        String ip = txtIp.getText().trim();
        
        if (ip.isEmpty()) {
            System.out.println(">>> [ERROR] Has de posar una IP!");
            return;
        }
        
        // actualitzem estat
        lblEstat.setText("Estat: Escanejant " + ip + "...");
        lblEstat.setForeground(Color.RED);
        
        // desactivem boto mentre s'executa
        btnExecutar.setEnabled(false);
        
        // executem en thread separat per no bloquejar UI
        new Thread(() -> {
            System.out.println("\n>>> [NMAP] Executant comanda externa...");
            
            if (nmapService != null) {
                try {
                    nmapService.escanearConNmap(ip);
                } catch (Exception ex) {
                    System.err.println(">>> [ERROR] Nmap: " + ex.getMessage());
                }
            } else {
                System.err.println(">>> [ERROR] Servei Nmap no disponible.");
            }
            
            // actualitzem UI quan acabi
            SwingUtilities.invokeLater(() -> {
                lblEstat.setText("Estat: Finalitzat.");
                lblEstat.setForeground(COLOR_VERD_FOSC);
                btnExecutar.setEnabled(true);
            });
        }).start();
    }

    //Estableix la IP des de fora (per autodeteccio).
    public void setIp(String ip) {
        txtIp.setText(ip);
    }
    
    //Comprova si Nmap esta instalat i actualitza la UI.

    public void verificarInstalacio() {
        if (nmapService == null || !nmapService.checkInstalled()) {
            btnExecutar.setEnabled(false);
            btnExecutar.setText("NMAP NO INSTALAT");
            System.err.println(">>> [MISSING] Nmap no trobat.");
        } else {
            System.out.println(">>> [OK] Nmap detectat.");
        }
    }
    
    //Retorna el servei per si es necessita des de fora.
    public NmapWrapper getNmapService() {
        return nmapService;
    }
}
