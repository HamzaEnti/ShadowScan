package view;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import services.BruteForceService;
import services.FuzzingService;
import services.WebDiscoveryService;

//Panel per a les eines de seguretat (Hydra, Dirb, Ffuf).
public class SecurityPanel extends BasePanel {
    
    // components del formulari
    private JComboBox<String> cmbMode;
    private JTextField txtTargetIp;
    private JTextField txtTargetPort;
    
    // botons
    private JButton btnLoadWordlist;
    private JButton btnExportCsv;
    private JButton btnExecutar;
    
    // etiqueta d'estat
    private JLabel lblEstat;
    
    // fitxer wordlist seleccionat
    private File selectedWordlist = null;
    
    // serveis
    private BruteForceService bruteForceService;
    private WebDiscoveryService webDiscoveryService;
    private FuzzingService fuzzingService;
    
    public SecurityPanel(MainFrame parent) {
        super(parent);
    }
    
    @Override
    protected void initComponents() {
        // inicialitzem serveis
        inicialitzarServeis();
        
        // layout vertical
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        
        // afegim components
        afegirTitol();
        afegirSelectorMode();
        afegirFormulari();
        afegirBotonsAuxiliars();
        afegirBotoPrincipal();
        afegirEstatLabel();
        
        // configurem listeners
        configurarListeners();
    }
    
    private void inicialitzarServeis() {
        try {
            this.bruteForceService = new BruteForceService();
            this.webDiscoveryService = new WebDiscoveryService();
            this.fuzzingService = new FuzzingService();
        } catch (Exception e) {
            System.err.println(">>> [ERROR] Error inicialitzant serveis: " + e.getMessage());
        }
    }
    
    private void afegirTitol() {
        JLabel lblTitol = crearTitol("Advanced Security Tools");
        lblTitol.setFont(new Font("Arial", Font.BOLD, 22));
        this.add(Box.createVerticalStrut(20));
        this.add(lblTitol);
    }
    
    private void afegirSelectorMode() {
        String[] modes = {
            "Modo 1: Brute Force (Hydra)", 
            "Modo 2: Web Enumeration (Dirb)", 
            "Modo 3: Web Fuzzing (Ffuf)"
        };
        cmbMode = new JComboBox<>(modes);
        cmbMode.setMaximumSize(new Dimension(400, 40));
        cmbMode.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        this.add(Box.createVerticalStrut(20));
        this.add(cmbMode);
    }
    
    private void afegirFormulari() {
        JPanel pnlForm = new JPanel(new GridLayout(2, 2, 10, 10));
        pnlForm.setBorder(BorderFactory.createTitledBorder("Configuracio Objectiu"));
        pnlForm.setMaximumSize(new Dimension(600, 100));
        
        pnlForm.add(new JLabel("IP / Domini Objectiu:"));
        txtTargetIp = new JTextField();
        pnlForm.add(txtTargetIp);
        
        pnlForm.add(new JLabel("Port:"));
        txtTargetPort = new JTextField("22");
        pnlForm.add(txtTargetPort);
        
        this.add(Box.createVerticalStrut(20));
        this.add(pnlForm);
    }
    
    private void afegirBotonsAuxiliars() {
        JPanel pnlFiles = new JPanel(new FlowLayout());
        btnLoadWordlist = crearBoto("Cargar Wordlist");
        btnExportCsv = crearBoto("Exportar CSV (Web)");
        pnlFiles.add(btnLoadWordlist);
        pnlFiles.add(btnExportCsv);
        
        this.add(pnlFiles);
    }
    
    private void afegirBotoPrincipal() {
        btnExecutar = new JButton("INICIAR ATAC");
        btnExecutar.setFont(new Font("Arial", Font.BOLD, 14));
        btnExecutar.setBackground(COLOR_VERMELL);
        btnExecutar.setForeground(Color.WHITE);
        btnExecutar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnExecutar.setMaximumSize(new Dimension(300, 50));
        
        this.add(Box.createVerticalStrut(20));
        this.add(btnExecutar);
    }
    
    private void afegirEstatLabel() {
        lblEstat = new JLabel("Selecciona un mode per comencar.");
        lblEstat.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        this.add(Box.createVerticalStrut(10));
        this.add(lblEstat);
    }
    
    private void configurarListeners() {
        // quan canvia el mode, actualitzem els camps
        cmbMode.addActionListener(e -> actualitzarModeSelecionat());
        
        // carrega wordlist
        btnLoadWordlist.addActionListener(e -> carregarWordlist());
        
        // exportar CSV
        btnExportCsv.addActionListener(e -> exportarCsv());
        
        // boto principal
        btnExecutar.addActionListener(e -> executarAtac());
    }
    
    //Actualitza els camps segons el mode seleccionat.
    private void actualitzarModeSelecionat() {
        int mode = cmbMode.getSelectedIndex();
        
        switch (mode) {
            case 0: // Hydra
                txtTargetPort.setText("22");
                txtTargetPort.setEnabled(true);
                btnExecutar.setText("INICIAR HYDRA");
                btnExecutar.setBackground(COLOR_VERMELL);
                break;
            case 1: // Dirb
                txtTargetPort.setText("80");
                txtTargetPort.setEnabled(false);
                btnExecutar.setText("INICIAR DIRB");
                btnExecutar.setBackground(COLOR_BLAU);
                break;
            case 2: // Ffuf
                txtTargetPort.setText("80");
                txtTargetPort.setEnabled(false);
                btnExecutar.setText("INICIAR FFUF");
                btnExecutar.setBackground(COLOR_TARONJA);
                break;
        }
    }
    
    //Carrega un fitxer wordlist.
    private void carregarWordlist() {
        File f = seleccionarFitxer("Selecciona Wordlist");
        if (f != null) {
            this.selectedWordlist = f;
            System.out.println(">>> [UI] Wordlist carregada: " + f.getName());
            
            // si estem en mode Dirb, passem la wordlist al servei
            int mode = cmbMode.getSelectedIndex();
            if (mode == 1 && webDiscoveryService != null) {
                webDiscoveryService.loadCustomDictionary(f);
            }
        }
    }
    
    //Exporta resultats de Dirb a CSV.
    private void exportarCsv() {
        File f = guardarFitxer("Guardar CSV", "resultats_web.csv");
        if (f != null && webDiscoveryService != null) {
            if (webDiscoveryService.exportReportToCSV(f)) {
                JOptionPane.showMessageDialog(this, "CSV exportat correctament!");
            }
        }
    }
    
    //Executa l'atac segons el mode seleccionat.
    private void executarAtac() {
        String target = txtTargetIp.getText().trim();
        
        if (target.isEmpty()) {
            System.out.println(">>> [ERROR] Has de posar un objectiu!");
            return;
        }
        
        // parsejem el port
        int port = 80;
        try {
            port = Integer.parseInt(txtTargetPort.getText().trim());
        } catch (NumberFormatException ex) {
            System.out.println(">>> [WARN] Port invalid, usant 80 per defecte");
        }
        
        int mode = cmbMode.getSelectedIndex();
        int finalPort = port;
        
        lblEstat.setText("Executant...");
        btnExecutar.setEnabled(false);
        
        // thread separat per no bloquejar UI
        new Thread(() -> {
            String wordlistPath = (selectedWordlist != null) ? 
                                  selectedWordlist.getAbsolutePath() : null;
            
            try {
                switch (mode) {
                    case 0: // HYDRA
                        executarHydra(target, finalPort, wordlistPath);
                        break;
                    case 1: // DIRB
                        executarDirb(target, finalPort, wordlistPath);
                        break;
                    case 2: // FFUF
                        executarFfuf(target, finalPort, wordlistPath);
                        break;
                }
            } catch (Exception ex) {
                System.err.println(">>> [ERROR] " + ex.getMessage());
            }
            
            SwingUtilities.invokeLater(() -> {
                lblEstat.setText("Finalitzat. Mira la consola.");
                btnExecutar.setEnabled(true);
            });
        }).start();
    }
    
    private void executarHydra(String target, int port, String wordlistPath) {
        if (wordlistPath == null) {
            System.err.println(">>> [ERROR] Hydra necessita wordlist!");
            return;
        }
        // assumim mateix diccionari per user i pass (simplificat)
        bruteForceService.atacar(target, port, wordlistPath, wordlistPath);
    }
    
    private void executarDirb(String target, int port, String wordlistPath) {
        webDiscoveryService.discoverWebPaths(target, port, wordlistPath);
    }
    
    private void executarFfuf(String target, int port, String wordlistPath) {
        if (wordlistPath == null) {
            System.err.println(">>> [ERROR] Ffuf necessita wordlist!");
            return;
        }
        fuzzingService.lanzarFuzzing(target, port, wordlistPath);
    }
    
    //Estableix la IP des de fora.
    public void setTargetIp(String ip) {
        txtTargetIp.setText(ip);
    }
    
    // Verifica quines eines estan instalades i ho mostra.
    public void verificarInstalacions() {
        if (bruteForceService != null && bruteForceService.checkInstalled()) {
            System.out.println(">>> [OK] Hydra detectat.");
        } else {
            System.err.println(">>> [MISSING] Hydra no trobat.");
        }
        
        if (webDiscoveryService != null && webDiscoveryService.checkInstalled()) {
            System.out.println(">>> [OK] Dirb detectat.");
        } else {
            System.err.println(">>> [MISSING] Dirb no trobat.");
        }
        
        if (fuzzingService != null && fuzzingService.checkInstalled()) {
            System.out.println(">>> [OK] Ffuf detectat.");
        } else {
            System.err.println(">>> [MISSING] Ffuf no trobat.");
        }
    }
    
    // Getters pels serveis
    public BruteForceService getBruteForceService() { return bruteForceService; }
    public WebDiscoveryService getWebDiscoveryService() { return webDiscoveryService; }
    public FuzzingService getFuzzingService() { return fuzzingService; }
}
