package view;

import controller.ScanController;
import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;  // <--- import important per trobar la teva ip
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI; // Necessari per modificar l'estil del TabbedPane
import javax.swing.table.DefaultTableModel;
import model.ResultatHost;
import services.BruteForceService;
import services.FuzzingService;
import services.NmapWrapper;
import utils.JsonExporter;

public class MainFrame extends JFrame {

    // Components Visuals (OSCAR)
    private JTabbedPane tabbedPane; // Contenidor principal de pestanyes
    private JTextArea txtConsole;   // La consola visual
    
    // Botons de navegació lateral (Disseny Fase 2 recuperat)
    private JButton btnNavDiscovery, btnNavNmap, btnNavSecurity;

    // --- PESTANYA 1: DISCOVERY (OSCAR) ---
    private JTextField txtIpInici, txtIpFi;
    private JButton btnStart, btnStop, btnExport;
    private JTable taula;
    private DefaultTableModel modelTaula;

    // --- PESTANYA 2: NMAP (NOU) ---
    private JTextField txtNmapIp;
    private JButton btnRunNmap;
    private JLabel lblNmapStatus;

    // --- PESTANYA 3: EINES DE SEGURETAT (HAMZA) ---
    private JTextField txtBruteIp, txtBrutePort;
    private JButton btnLlançarHydra;
    private JTextField txtFuzzIp, txtFuzzPort, txtFuzzTime;
    private JButton btnLlançarFuzz;

    // Controladors i Serveis
    private ScanController controller;
    private List<ResultatHost> resultats;

    // Instàncies dels serveis del HAMZA
    private NmapWrapper nmapService;
    private BruteForceService bruteForceService;
    private FuzzingService fuzzingService;

    public MainFrame() {
        this.setTitle("Scanner Security Suite - Integració Total v3.0");
        this.setSize(1100, 750); // Finestra gran per encabir el menú lateral
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        // Inicialitzem dades
        this.resultats = new ArrayList<>();
        this.controller = new ScanController(this);
        
        // Inicialitzem els motors del Hamza
        this.nmapService = new NmapWrapper();
        this.bruteForceService = new BruteForceService();
        this.fuzzingService = new FuzzingService();

        // 1. Inicialitzem la consola (NICO) primer per veure errors si n'hi ha
        initConsole();
        
        // 2. Construïm la pantalla (OSCAR) amb el sistema de pestanyes
        initPantalla();
        
        // 3. Redirigim la consola (NICO)
        configurarLogs();
        
        // Check inicial i dependències
        checkDependencies();
        autoDetectarIp(); 
    }

    // --- 1. CONFIGURACIÓ VISUAL (OSCAR) ---
    private void initPantalla() {
        // CENTRE: El sistema de pestanyes (JTabbedPane)
        tabbedPane = new JTabbedPane();
        
        // Modifiquem la UI per ocultar les pestanyes superiors. 
        // Volem fer servir el menú lateral perquè queda més professional, però mantenint l'estructura de TabbedPane que demanava l'exercici.
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
                return 0; // Alçada 0 = Invisible
            }
        });

        // Afegim les 3 pestanyes
        tabbedPane.addTab("Discovery", createDiscoveryPanel());
        tabbedPane.addTab("Nmap", createNmapPanel());
        tabbedPane.addTab("Security", createAttackPanel());

        this.add(tabbedPane, BorderLayout.CENTER);

        // PANEL DRET (MENÚ DE NAVEGACIÓ)
        JPanel panelDreta = new JPanel();
        panelDreta.setLayout(new BoxLayout(panelDreta, BoxLayout.Y_AXIS));
        panelDreta.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        panelDreta.setPreferredSize(new Dimension(220, 0));
        panelDreta.setBackground(new Color(245, 245, 245));

        JLabel lblMenu = new JLabel("MENU PRINCIPAL");
        lblMenu.setFont(new Font("Arial", Font.BOLD, 14));
        lblMenu.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Botons grans estil panell de control
        btnNavDiscovery = createStyledButton("1. NETWORK SCAN");
        btnNavNmap = createStyledButton("2. NMAP ANALYZER");
        btnNavSecurity = createStyledButton("3. HACKING TOOLS");

        panelDreta.add(Box.createVerticalStrut(20));
        panelDreta.add(lblMenu);
        panelDreta.add(Box.createVerticalStrut(20));
        panelDreta.add(btnNavDiscovery);
        panelDreta.add(Box.createVerticalStrut(15));
        panelDreta.add(btnNavNmap);
        panelDreta.add(Box.createVerticalStrut(15));
        panelDreta.add(btnNavSecurity);
        panelDreta.add(Box.createVerticalGlue());

        // Configurem la navegació: els botons canvien la pestanya activa
        btnNavDiscovery.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        btnNavNmap.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        btnNavSecurity.addActionListener(e -> tabbedPane.setSelectedIndex(2));

        this.add(panelDreta, BorderLayout.EAST);
    }
    
    // Helper visual
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(190, 45));
        btn.setPreferredSize(new Dimension(190, 45));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(Color.WHITE);
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(220, 230, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(Color.WHITE);
            }
        });
        return btn;
    }

    // --- PESTANYA 1: DISCOVERY (Recuperat Fase 1) ---
    private JPanel createDiscoveryPanel() {
        JPanel pnlBase = new JPanel(new BorderLayout());
        
        // PANEL SUPERIOR (Xarxa)
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTop.setBorder(BorderFactory.createTitledBorder("Configuració de Xarxa"));
        
        pnlTop.add(new JLabel("IP Inici:"));
        txtIpInici = new JTextField(10);
        pnlTop.add(new JLabel("IP Fi:"));
        txtIpFi = new JTextField(10);
        
        btnStart = new JButton("Escanear");
        btnStart.setBackground(new Color(144, 238, 144)); // Verd clar
        
        btnStop = new JButton("Aturar");
        btnStop.setBackground(new Color(255, 160, 122)); // Vermell clar
        
        btnExport = new JButton("Guardar JSON");

        // Listeners bàsics
        btnStart.addActionListener(e -> {
            String ip = txtIpInici.getText();
            if(ip.contains(".")) {
                // Tallem la IP per quedar-nos només amb la xarxa base
                String xarxa = ip.substring(0, ip.lastIndexOf(".") + 1);
                System.out.println(">>> [SCAN] Iniciant escombrat de IPs a " + xarxa + "0/24");
                resultats.clear();
                modelTaula.setRowCount(0);
                controller.escanearRang(xarxa);
            }
        });

        btnStop.addActionListener(e -> controller.aturar());
        btnExport.addActionListener(e -> JsonExporter.saveToJSON(resultats, "resultats.json"));

        pnlTop.add(btnStart);
        pnlTop.add(btnStop);
        pnlTop.add(btnExport);

        // CENTRE (Taula de resultats)
        String[] titols = {"IP Adreça", "Estat", "Ports Detectats"};
        modelTaula = new DefaultTableModel(titols, 0);
        taula = new JTable(modelTaula);
        JScrollPane scrollTaula = new JScrollPane(taula);

        pnlBase.add(pnlTop, BorderLayout.NORTH);
        pnlBase.add(scrollTaula, BorderLayout.CENTER);
        return pnlBase;
    }

    // --- PESTANYA 2: INTEGRACIÓ NMAP ---
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
        
        // Listener Nmap
        btnRunNmap.addActionListener(e -> {
            String ip = txtNmapIp.getText();
            if (ip.isEmpty()) return;
            
            lblNmapStatus.setText("Estat: Escanejant " + ip + "...");
            lblNmapStatus.setForeground(Color.RED);
            
            // Llancem l'escaneig en un fil (Thread) independent. És vital fer-ho així, 
            // si ho executéssim al fil principal, la interfície gràfica es quedaria congelada ("No respon").
            new Thread(() -> {
                System.out.println("\n--- [NMAP] Iniciant anàlisi de serveis a " + ip + " ---");
                nmapService.escanearConNmap(ip); // Això es veurà a la consola gràcies al Nico
                
                SwingUtilities.invokeLater(() -> {
                    lblNmapStatus.setText("Estat: Finalitzat correctament.");
                    lblNmapStatus.setForeground(new Color(0, 128, 0)); // Verd fosc
                });
            }).start();
        });

        return pnlBase;
    }

    // --- PESTANYA 3: EINES DE SEGURETAT (HAMZA) ---
    private JPanel createAttackPanel() {
        JPanel pnlBase = new JPanel(new GridLayout(1, 2, 20, 0));
        pnlBase.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ZONA BRUTE FORCE (HYDRA)
        JPanel pnlBrute = new JPanel();
        pnlBrute.setLayout(new BoxLayout(pnlBrute, BoxLayout.Y_AXIS));
        pnlBrute.setBorder(BorderFactory.createTitledBorder("Brute Force (Hydra)"));

        txtBruteIp = new JTextField();
        txtBruteIp.setBorder(BorderFactory.createTitledBorder("IP Target"));
        txtBrutePort = new JTextField("22");
        txtBrutePort.setBorder(BorderFactory.createTitledBorder("Port (SSH/FTP)"));
        
        btnLlançarHydra = new JButton("ATACAR (Sel. Diccionaris)");
        btnLlançarHydra.setBackground(new Color(255, 99, 71)); // Tomàquet
        btnLlançarHydra.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlBrute.add(txtBruteIp);
        pnlBrute.add(Box.createVerticalStrut(10));
        pnlBrute.add(txtBrutePort);
        pnlBrute.add(Box.createVerticalStrut(20));
        pnlBrute.add(btnLlançarHydra);

        // ZONA FUZZING (FFUF)
        JPanel pnlFuzz = new JPanel();
        pnlFuzz.setLayout(new BoxLayout(pnlFuzz, BoxLayout.Y_AXIS));
        pnlFuzz.setBorder(BorderFactory.createTitledBorder("Web Fuzzing (Ffuf)"));

        txtFuzzIp = new JTextField();
        txtFuzzIp.setBorder(BorderFactory.createTitledBorder("Domini/IP"));
        txtFuzzPort = new JTextField("80");
        txtFuzzPort.setBorder(BorderFactory.createTitledBorder("Port Web"));
        txtFuzzTime = new JTextField("30");
        txtFuzzTime.setBorder(BorderFactory.createTitledBorder("Timeout (s)"));

        btnLlançarFuzz = new JButton("FUZZING (Sel. Wordlist)");
        btnLlançarFuzz.setBackground(new Color(255, 165, 0)); // Taronja
        btnLlançarFuzz.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlFuzz.add(txtFuzzIp);
        pnlFuzz.add(Box.createVerticalStrut(10));
        pnlFuzz.add(txtFuzzPort);
        pnlFuzz.add(Box.createVerticalStrut(5));
        pnlFuzz.add(txtFuzzTime);
        pnlFuzz.add(Box.createVerticalStrut(20));
        pnlFuzz.add(btnLlançarFuzz);
        
        // Configurem les accions dels botons del Hamza
        btnLlançarHydra.addActionListener(e -> {
             File fUsers = seleccionarFitxer("Tria diccionari Usuaris");
             if (fUsers == null) return;
             File fPass = seleccionarFitxer("Tria diccionari Passwords");
             if (fPass == null) return;
             
             new Thread(() -> bruteForceService.atacar(txtBruteIp.getText(), Integer.parseInt(txtBrutePort.getText()), fUsers.getAbsolutePath(), fPass.getAbsolutePath())).start();
        });

        btnLlançarFuzz.addActionListener(e -> {
            File fWordlist = seleccionarFitxer("Tria wordlist de directoris");
            if (fWordlist == null) return;
            
            new Thread(() -> fuzzingService.lanzarFuzzing(txtFuzzIp.getText(), Integer.parseInt(txtFuzzPort.getText()), fWordlist.getAbsolutePath())).start();
        });

        pnlBase.add(pnlBrute);
        pnlBase.add(pnlFuzz);
        return pnlBase;
    }

    // --- 3. UTILITATS DEL NICO (REDIRECTOR) ---
    private void initConsole() {
        txtConsole = new JTextArea(10, 50);
        txtConsole.setBackground(Color.BLACK);
        txtConsole.setForeground(new Color(50, 205, 50)); // Lime Green
        txtConsole.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtConsole.setEditable(false);
        JScrollPane scrollConsole = new JScrollPane(txtConsole);
        scrollConsole.setBorder(BorderFactory.createTitledBorder("Logs del Sistema (Sortida Standard)"));
        
        this.add(scrollConsole, BorderLayout.SOUTH); 
    }

    private void configurarLogs() {
        // Redirigim tot el que surt per consola (System.out) cap al quadre de text de l'app. 
        // Així l'usuari pot veure el progrés de les eines en temps real.
        ConsoleRedirector redirector = new ConsoleRedirector(txtConsole);
        PrintStream outStream = new PrintStream(redirector);
        System.setOut(outStream);
        System.setErr(outStream);
    }

    // --- HELPERS ---
    
    // Autodetecció d'IP (Nou requisit Fase 3)
    private void autoDetectarIp() {
        try {
            String myIp = InetAddress.getLocalHost().getHostAddress(); 
            // Tallem la IP (ex: 192.168.1.33) per quedar-nos amb el rang.
            txtIpInici.setText(myIp);
            txtIpFi.setText(myIp.substring(0, myIp.lastIndexOf(".")+1) + "254");
            
            // Omplim també les eines d'atac per comoditat
            txtNmapIp.setText(myIp);
            txtBruteIp.setText(myIp);
            txtFuzzIp.setText(myIp);
            System.out.println("[INIT] IP Local detectada i camps omplerts: " + myIp);
        } catch (Exception e) {
            System.err.println("[ERROR] No s'ha pogut detectar la IP local.");
        }
    }

    private void checkDependencies() {
        System.out.println("Verificant entorn...");
        // Comprovem si l'Nmap està instal·lat. Si no ho està, avisem per consola.
        if (!nmapService.checkNmapInstalled()) {
            System.err.println("[AVÍS] Nmap no detectat. El botó de Nmap fallarà.");
        } else {
            System.out.println("[OK] Nmap llest.");
        }
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

    // Callback del ScanTask (quan troba una IP)
    public synchronized void afegirResultat(ResultatHost host) {
        resultats.add(host);
        modelTaula.addRow(new Object[]{
            host.getIp(), 
            "ONLINE", 
            host.getPortsOberts().toString()
        });
    }
}