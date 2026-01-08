package view;

import controller.PortScanMode;
import controller.ScanController;
import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableModel;
import model.ResultatHost;
import services.BruteForceService;
import services.FuzzingService;
import services.NmapWrapper;
import services.WebDiscoveryService;
import utils.JsonExporter;

public class MainFrame extends JFrame {
    // --- COMPONENTS VISUALS GLOBALS ---
    private JTabbedPane tabbedPane;
    private JTextArea txtConsole;
    private JButton btnNavDiscovery, btnNavNmap, btnNavSecurity;

    // --- PESTANYA 1: DISCOVERY ---
    private JRadioButton rbPortsComuns, rbTotsPorts;
    private JTextField txtIpInici, txtIpFi;
    private JButton btnStart, btnStop, btnExport;
    private JTable taula;
    private DefaultTableModel modelTaula;

    // --- PESTANYA 2: NMAP ---
    private JTextField txtNmapIp;
    private JButton btnRunNmap;
    private JLabel lblNmapStatus;

    // --- PESTANYA 3: SECURITY TOOLS ---
    private JComboBox<String> cmbMode;
    private JTextField txtTargetIp, txtTargetPort;
    private JButton btnLoadWordlist, btnExportCsv, btnRunSecurity;
    private JLabel lblSecStatus;
    private File selectedWordlist = null;

    // --- CONTROLADORS I SERVEIS ---
    private ScanController controller;
    private List<ResultatHost> resultats;

    // INSTÀNCIES DELS 4 SERVEIS
    private NmapWrapper nmapService;
    private BruteForceService bruteForceService;
    private WebDiscoveryService webDiscoveryService; // Dirb
    private FuzzingService fuzzingService;           // Ffuf

    public MainFrame() {
        this.setTitle("Scanner Security Suite - Integració Total v4.0 (4 Serveis)");
        this.setSize(1100, 750);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        // 1. Inicialitzem dades
        this.resultats = new ArrayList<>();
        this.controller = new ScanController(this);

        // 2. Inicialitzem els 4 serveis (Try-catch per seguretat)
        try {
            this.nmapService = new NmapWrapper();
            this.bruteForceService = new BruteForceService();
            this.webDiscoveryService = new WebDiscoveryService();
            this.fuzzingService = new FuzzingService();
        } catch (Exception e) {
            System.err.println(">>> [ERROR CRÍTIC] Error inicialitzant serveis: " + e.getMessage());
        }

        // 3. Components Visuals
        initConsole();   
        initPantalla();  
        configurarLogs(); 

        // 4. Verificacions d'entorn
        autoDetectarIp();
        checkDependenciesAsync(); 
    }

    // --- CONFIGURACIÓ VISUAL ---
    private void initPantalla() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
                return 0; // Ocultar tabs superiors
            }
        });

        tabbedPane.addTab("Discovery", createDiscoveryPanel());
        tabbedPane.addTab("Nmap", createNmapPanel());
        tabbedPane.addTab("Security", createAttackPanel());

        this.add(tabbedPane, BorderLayout.CENTER);
        createSideMenu(); 
    }

    private void createSideMenu() {
        JPanel panelDreta = new JPanel();
        panelDreta.setLayout(new BoxLayout(panelDreta, BoxLayout.Y_AXIS));
        panelDreta.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        panelDreta.setPreferredSize(new Dimension(220, 0));
        panelDreta.setBackground(new Color(245, 245, 245));

        JLabel lblMenu = new JLabel("MENÚ PRINCIPAL");
        lblMenu.setFont(new Font("Arial", Font.BOLD, 14));
        lblMenu.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnNavDiscovery = createStyledButton("1. NETWORK SCAN");
        btnNavNmap = createStyledButton("2. NMAP ANALYZER");
        btnNavSecurity = createStyledButton("3. SECURITY TOOLS");

        panelDreta.add(Box.createVerticalStrut(20)); panelDreta.add(lblMenu);
        panelDreta.add(Box.createVerticalStrut(20)); panelDreta.add(btnNavDiscovery);
        panelDreta.add(Box.createVerticalStrut(15)); panelDreta.add(btnNavNmap);
        panelDreta.add(Box.createVerticalStrut(15)); panelDreta.add(btnNavSecurity);
        panelDreta.add(Box.createVerticalGlue());

        btnNavDiscovery.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        btnNavNmap.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        btnNavSecurity.addActionListener(e -> tabbedPane.setSelectedIndex(2));

        this.add(panelDreta, BorderLayout.EAST);
    }

    // --- PESTANYA 1: DISCOVERY ---
    private JPanel createDiscoveryPanel() {
        JPanel pnlBase = new JPanel(new BorderLayout());
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTop.setBorder(BorderFactory.createTitledBorder("Configuració de Xarxa"));

        pnlTop.add(new JLabel("IP Inici:"));
        txtIpInici = new JTextField(10);
        pnlTop.add(txtIpInici);
        pnlTop.add(new JLabel("IP Fi:"));
        txtIpFi = new JTextField(10);
        pnlTop.add(txtIpFi);

        btnStart = new JButton("Escanejar");
        btnStart.setBackground(new Color(144, 238, 144));
        btnStop = new JButton("Aturar");
        btnStop.setBackground(new Color(255, 160, 122));
        btnExport = new JButton("Guardar JSON");

        pnlTop.add(btnStart); pnlTop.add(btnStop); pnlTop.add(btnExport);

        rbPortsComuns = new JRadioButton("Ports comuns (ràpid)", true);
        rbTotsPorts = new JRadioButton("Tots els ports (lent)");
        ButtonGroup grpPorts = new ButtonGroup();
        grpPorts.add(rbPortsComuns); grpPorts.add(rbTotsPorts);
        
        JPanel pnlPorts = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlPorts.setBorder(BorderFactory.createTitledBorder("Mode d'escaneig"));
        pnlPorts.add(rbPortsComuns); pnlPorts.add(rbTotsPorts);

        JPanel pnlNord = new JPanel(new BorderLayout());
        pnlNord.add(pnlTop, BorderLayout.NORTH);
        pnlNord.add(pnlPorts, BorderLayout.SOUTH);
        pnlBase.add(pnlNord, BorderLayout.NORTH);

        String[] titols = {"IP Adreça", "Estat", "Ports Detectats"};
        modelTaula = new DefaultTableModel(titols, 0);
        taula = new JTable(modelTaula);
        pnlBase.add(new JScrollPane(taula), BorderLayout.CENTER);

        btnStart.addActionListener(e -> {
            String ip = txtIpInici.getText();
            if (!ip.contains(".")) return;
            String xarxa = ip.substring(0, ip.lastIndexOf(".") + 1);
            PortScanMode mode = rbPortsComuns.isSelected() ? PortScanMode.PARCIAL : PortScanMode.FULL;
            
            System.out.println(">>> [SCAN] Iniciant escombrat a " + xarxa + "0/24...");
            resultats.clear();
            modelTaula.setRowCount(0);
            controller.escanearRang(xarxa, mode);
        });

        btnStop.addActionListener(e -> controller.aturar());
        btnExport.addActionListener(e -> JsonExporter.saveToJSON(resultats, "resultats.json"));

        return pnlBase;
    }

    // --- PESTANYA 2: NMAP ---
    private JPanel createNmapPanel() {
        JPanel pnlBase = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titol = new JLabel("Nmap Service Detector (-sV)");
        titol.setFont(new Font("Arial", Font.BOLD, 18));
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        pnlBase.add(titol, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1;
        pnlBase.add(new JLabel("IP Objectiu:"), gbc);

        gbc.gridx = 1;
        txtNmapIp = new JTextField(15);
        pnlBase.add(txtNmapIp, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        btnRunNmap = new JButton("EXECUTAR ANÀLISI");
        btnRunNmap.setPreferredSize(new Dimension(200, 40));
        pnlBase.add(btnRunNmap, gbc);

        gbc.gridy = 3;
        lblNmapStatus = new JLabel("Estat: Esperant ordre...");
        pnlBase.add(lblNmapStatus, gbc);

        btnRunNmap.addActionListener(e -> {
            String ip = txtNmapIp.getText();
            if (ip.isEmpty()) return;
            lblNmapStatus.setText("Estat: Escanejant " + ip + "...");
            lblNmapStatus.setForeground(Color.RED);

            new Thread(() -> {
                System.out.println("\n>>> [NMAP] Executant comanda externa...");
                if(nmapService != null) {
                    try {
                        nmapService.escanearConNmap(ip);
                    } catch (Exception ex) {
                         System.err.println("Error Nmap: " + ex.getMessage());
                    }
                } else {
                    System.err.println("Servei Nmap no disponible.");
                }
                SwingUtilities.invokeLater(() -> {
                    lblNmapStatus.setText("Estat: Finalitzat.");
                    lblNmapStatus.setForeground(new Color(0, 128, 0));
                });
            }).start();
        });

        return pnlBase;
    }

    // --- PESTANYA 3: SECURITY TOOLS (ARA AMB 3 MODES) ---
    private JPanel createAttackPanel() {
        JPanel pnlBase = new JPanel();
        pnlBase.setLayout(new BoxLayout(pnlBase, BoxLayout.Y_AXIS));
        pnlBase.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel lblTitol = new JLabel("Advanced Security Tools");
        lblTitol.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitol.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // --- AQUÍ ESTÀ EL CANVI: 3 OPCIONS ---
        String[] modes = {
            "Modo 1: Brute Force (Hydra)", 
            "Modo 2: Web Enumeration (Dirb)", 
            "Modo 3: Web Fuzzing (Ffuf)"
        };
        cmbMode = new JComboBox<>(modes);
        cmbMode.setMaximumSize(new Dimension(400, 40));
        cmbMode.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pnlForm = new JPanel(new GridLayout(2, 2, 10, 10));
        pnlForm.setBorder(BorderFactory.createTitledBorder("Configuració Objectiu"));
        pnlForm.setMaximumSize(new Dimension(600, 100));
        
        pnlForm.add(new JLabel("IP / Domini Objectiu:"));
        txtTargetIp = new JTextField();
        pnlForm.add(txtTargetIp);
        
        pnlForm.add(new JLabel("Port:"));
        txtTargetPort = new JTextField("22");
        pnlForm.add(txtTargetPort);

        JPanel pnlFiles = new JPanel(new FlowLayout());
        btnLoadWordlist = new JButton("Cargar Wordlist");
        btnExportCsv = new JButton("Exportar CSV (Web)");
        pnlFiles.add(btnLoadWordlist);
        pnlFiles.add(btnExportCsv);

        btnRunSecurity = new JButton("INICIAR ATAC");
        btnRunSecurity.setFont(new Font("Arial", Font.BOLD, 14));
        btnRunSecurity.setBackground(new Color(255, 69, 0));
        btnRunSecurity.setForeground(Color.WHITE);
        btnRunSecurity.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRunSecurity.setMaximumSize(new Dimension(300, 50));

        lblSecStatus = new JLabel("Selecciona un mode per començar.");
        lblSecStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- LÒGICA DEL COMBOBOX ---
        cmbMode.addActionListener(e -> {
            int selected = cmbMode.getSelectedIndex();
            if (selected == 0) { // Hydra
                txtTargetPort.setText("22");
                txtTargetPort.setEnabled(true);
                btnRunSecurity.setText("INICIAR HYDRA");
                btnRunSecurity.setBackground(new Color(255, 69, 0)); // Vermell
            } else if (selected == 1) { // Dirb
                txtTargetPort.setText("80");
                txtTargetPort.setEnabled(false);
                btnRunSecurity.setText("INICIAR DIRB");
                btnRunSecurity.setBackground(new Color(0, 153, 204)); // Blau
            } else if (selected == 2) { // Ffuf
                txtTargetPort.setText("80");
                txtTargetPort.setEnabled(false);
                btnRunSecurity.setText("INICIAR FFUF");
                btnRunSecurity.setBackground(new Color(255, 165, 0)); // Taronja
            }
        });

        // --- LÒGICA CARREGA WORDLIST ---
        btnLoadWordlist.addActionListener(e -> {
            File f = seleccionarFitxer("Selecciona Wordlist");
            if (f != null) {
                this.selectedWordlist = f;
                System.out.println(">>> [UI] Wordlist carregada: " + f.getName());
                
                // Passem la wordlist al servei corresponent segons el mode actiu
                int mode = cmbMode.getSelectedIndex();
                try {
                    if (mode == 1 && webDiscoveryService != null) {
                         webDiscoveryService.loadCustomDictionary(f);
                    }
                    // Si és Hydra o Ffuf, la guardem a 'selectedWordlist' i la passem en executar
                } catch (Exception ex) {
                   System.out.println(">>> [INFO] Error assignant wordlist: " + ex.getMessage());
                }
            }
        });

        // --- LÒGICA EXPORT ---
        btnExportCsv.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try {
                    // Exportem nomes del WebDiscoveryService per ara (Dirb)
                    if(webDiscoveryService != null) webDiscoveryService.exportReportToCSV(f);
                } catch (Exception ex) {
                     System.err.println(">>> [ERROR] Error exportant: " + ex.getMessage());
                }
            }
        });

        // --- LÒGICA START (EL MÉS IMPORTANT) ---
        btnRunSecurity.addActionListener(e -> {
            String target = txtTargetIp.getText();
            int port = 80;
            try {
                port = Integer.parseInt(txtTargetPort.getText());
            } catch (NumberFormatException ex) { }
            
            int mode = cmbMode.getSelectedIndex();
            int finalPort = port;

            new Thread(() -> {
                String wordlistPath = (selectedWordlist != null) ? selectedWordlist.getAbsolutePath() : null;

                if (mode == 0) { // HYDRA
                    if (wordlistPath == null) {
                        System.err.println(">>> [ERROR] Hydra necessita wordlist!");
                        return;
                    }
                    try {
                         // Assumim mateix diccionari per user i pass per la demo
                         bruteForceService.atacar(target, finalPort, wordlistPath, wordlistPath);
                    } catch (Exception ex) {
                         System.err.println(">>> [ERROR] Hydra fallit: " + ex.getMessage());
                    }

                } else if (mode == 1) { // DIRB
                     try {
                        webDiscoveryService.discoverWebPaths(target, finalPort, wordlistPath);
                    } catch (Exception ex) {
                        System.err.println(">>> [ERROR] Dirb fallit: " + ex.getMessage());
                    }

                } else if (mode == 2) { // FFUF
                    if (wordlistPath == null) {
                        System.err.println(">>> [ERROR] Ffuf necessita wordlist!");
                        return;
                    }
                    try {
                        fuzzingService.lanzarFuzzing(target, finalPort, wordlistPath);
                    } catch (Exception ex) {
                        System.err.println(">>> [ERROR] Ffuf fallit: " + ex.getMessage());
                    }
                }
            }).start();
        });

        pnlBase.add(Box.createVerticalStrut(20));
        pnlBase.add(lblTitol);
        pnlBase.add(Box.createVerticalStrut(20));
        pnlBase.add(cmbMode);
        pnlBase.add(Box.createVerticalStrut(20));
        pnlBase.add(pnlForm);
        pnlBase.add(pnlFiles);
        pnlBase.add(Box.createVerticalStrut(20));
        pnlBase.add(btnRunSecurity);
        pnlBase.add(Box.createVerticalStrut(10));
        pnlBase.add(lblSecStatus);

        return pnlBase;
    }

    private void initConsole() {
        txtConsole = new JTextArea(10, 50);
        txtConsole.setBackground(Color.BLACK);
        txtConsole.setForeground(new Color(50, 205, 50));
        txtConsole.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtConsole.setEditable(false);
        JScrollPane scrollConsole = new JScrollPane(txtConsole);
        scrollConsole.setBorder(BorderFactory.createTitledBorder("Logs del Sistema"));
        this.add(scrollConsole, BorderLayout.SOUTH);
    }

    private void configurarLogs() {
        ConsoleRedirector redirector = new ConsoleRedirector(txtConsole);
        PrintStream outStream = new PrintStream(redirector);
        System.setOut(outStream);
        System.setErr(outStream);
    }

    private File seleccionarFitxer(String titol) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(titol);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(190, 45));
        btn.setPreferredSize(new Dimension(190, 45));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(Color.WHITE);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { btn.setBackground(new Color(220, 230, 255)); }
            public void mouseExited(java.awt.event.MouseEvent evt) { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    private void autoDetectarIp() {
        try {
            String myIp = InetAddress.getLocalHost().getHostAddress();
            txtIpInici.setText(myIp);
            txtIpFi.setText(myIp.substring(0, myIp.lastIndexOf(".") + 1) + "254");
            txtNmapIp.setText(myIp);
            txtTargetIp.setText(myIp);
            System.out.println(">>> [INIT] IP Local detectada: " + myIp);
        } catch (Exception e) {
            System.err.println(">>> [ERROR] No s'ha pogut detectar IP.");
        }
    }

    // --- CHECK DEPENDENCIES PER A 4 EINES ---
    private void checkDependenciesAsync() {
        new Thread(() -> {
            System.out.println(">>> [BOOT] Verificant 4 eines externes...");

            boolean nmapOk = false, hydraOk = false, dirbOk = false, ffufOk = false;

            try { if (nmapService != null) nmapOk = nmapService.checkNmapInstalled(); } catch(Exception e){}
            try { if (bruteForceService != null) hydraOk = bruteForceService.checkInstalled(); } catch(Exception e){}
            try { if (webDiscoveryService != null) dirbOk = webDiscoveryService.checkInstalled(); } catch(Exception e){}
            try { if (fuzzingService != null) ffufOk = fuzzingService.checkInstalled(); } catch(Exception e){}

            boolean fNmap = nmapOk;
            boolean fHydra = hydraOk;
            boolean fDirb = dirbOk;
            boolean fFfuf = ffufOk;

            SwingUtilities.invokeLater(() -> {
                // Check NMAP
                if (!fNmap) {
                    btnRunNmap.setEnabled(false);
                    btnRunNmap.setText("NMAP NO INSTAL·LAT");
                    System.err.println(">>> [MISSING] Nmap no trobat.");
                } else {
                    System.out.println(">>> [OK] Nmap detectat.");
                }

                // Check TOOLS
                if (!fHydra) System.err.println(">>> [MISSING] Hydra no trobat.");
                else System.out.println(">>> [OK] Hydra detectat.");
                
                if (!fDirb) System.err.println(">>> [MISSING] Dirb no trobat.");
                else System.out.println(">>> [OK] Dirb detectat.");

                if (!fFfuf) System.err.println(">>> [MISSING] Ffuf no trobat.");
                else System.out.println(">>> [OK] Ffuf detectat.");
            });
        }).start();
    }

    public synchronized void afegirResultat(ResultatHost host) {
        resultats.add(host);
        modelTaula.addRow(new Object[] { host.getIp(), "[ONLINE]", host.getPortsOberts().toString() });
    }
}