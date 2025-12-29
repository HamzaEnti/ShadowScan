package view;

import controller.ScanController;
import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;  // <--- import important per trobar la teva ip
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.ResultatHost;
import services.BruteForceService;
import services.FuzzingService;
import services.NmapWrapper;
import utils.JsonExporter;

public class MainFrame extends JFrame {

    // Components Visuals (OSCAR)
    private JTextField txtIp;
    private JButton btnStart, btnStop, btnExport;
    private JTable taula;
    private DefaultTableModel modelTaula;
    private JTextArea txtConsole; // La consola visual
    
    // Botons d'Atac (NOU FASE 2)
    private JButton btnNmap, btnHydra, btnFfuf;
    
    // Controladors i Serveis
    private ScanController controller;
    private List<ResultatHost> resultats;

    // Instàncies dels serveis del HAMZA
    private NmapWrapper nmapService;
    private BruteForceService bruteForceService;
    private FuzzingService fuzzingService;

    public MainFrame() {
        this.setTitle("Scanner Security Suite - Integració Total");
        this.setSize(1000, 700); // Finestra gran
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        // Inicialitzem dades
        this.resultats = new ArrayList<>();
        this.controller = new ScanController(this);
        
        // Inicialitzem els motors del Hamza
        this.nmapService = new NmapWrapper();
        this.bruteForceService = new BruteForceService();
        this.fuzzingService = new FuzzingService();

        // Construïm la pantalla (OSCAR)
        initPantalla();
        
        // Redirigim la consola (NICO)
        configurarLogs();
        
        // Check inicial
        checkDependencies();
    }

    // --- 1. CONFIGURACIÓ VISUAL (OSCAR) ---
    private void initPantalla() {
        // PANEL SUPERIOR (Xarxa)
        JPanel panelDalt = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelDalt.setBorder(BorderFactory.createTitledBorder("Configuració de Xarxa"));
        
        panelDalt.add(new JLabel("Xarxa Objectiu:"));
        txtIp = new JTextField(trobarLaMevaXarxa(), 12);
        panelDalt.add(txtIp);
        
        btnStart = new JButton("Escanear");
        btnStart.setBackground(new Color(144, 238, 144)); // Verd clar
        
        btnStop = new JButton("Aturar");
        btnStop.setBackground(new Color(255, 160, 122)); // Vermell clar
        
        btnExport = new JButton("Guardar JSON");

        // Listeners bàsics
        btnStart.addActionListener(e -> {
            String ip = txtIp.getText();
            if (!ip.endsWith(".")) { ip += "."; txtIp.setText(ip); }
            
            resultats.clear();
            modelTaula.setRowCount(0);
            System.out.println(">>> [SCAN] Iniciant escombrat de IPs a " + ip + "0/24");
            controller.escanearRang(ip);
        });

        btnStop.addActionListener(e -> controller.aturar());
        btnExport.addActionListener(e -> JsonExporter.saveToJSON(resultats, "resultats.json"));

        panelDalt.add(btnStart);
        panelDalt.add(btnStop);
        panelDalt.add(btnExport);

        // PANEL DRET (EINES DE SEGURETAT - HAMZA)
        JPanel panelDreta = new JPanel();
        panelDreta.setLayout(new BoxLayout(panelDreta, BoxLayout.Y_AXIS));
        panelDreta.setBorder(BorderFactory.createTitledBorder("Arsenal de Seguretat"));
        panelDreta.setPreferredSize(new Dimension(220, 0));

        btnNmap = createStyledButton("1. Nmap Detallat");
        btnHydra = createStyledButton("2. BruteForce (Hydra)");
        btnFfuf = createStyledButton("3. Fuzzing (Ffuf)");

        panelDreta.add(Box.createVerticalStrut(20));
        panelDreta.add(btnNmap);
        panelDreta.add(Box.createVerticalStrut(15));
        panelDreta.add(btnHydra);
        panelDreta.add(Box.createVerticalStrut(15));
        panelDreta.add(btnFfuf);
        
        // Configurem les accions dels botons del Hamza
        configurarBotonsAtac();

        // CENTRE (Taula de resultats)
        String[] titols = {"IP Adreça", "Estat", "Ports Detectats"};
        modelTaula = new DefaultTableModel(titols, 0);
        taula = new JTable(modelTaula);
        JScrollPane scrollTaula = new JScrollPane(taula);
        
        // BAIX (Consola - NICO/OSCAR)
        txtConsole = new JTextArea(12, 50);
        txtConsole.setBackground(Color.BLACK);
        txtConsole.setForeground(new Color(50, 205, 50)); // Lime Green
        txtConsole.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtConsole.setEditable(false);
        JScrollPane scrollConsole = new JScrollPane(txtConsole);
        scrollConsole.setBorder(BorderFactory.createTitledBorder("Logs del Sistema (Sortida Standard)"));

        // Muntatge final del Layout
        this.add(panelDalt, BorderLayout.NORTH);
        this.add(panelDreta, BorderLayout.EAST);
        this.add(scrollTaula, BorderLayout.CENTER);
        this.add(scrollConsole, BorderLayout.SOUTH);
    }

    // --- 2. INTEGRACIÓ LÒGICA D'ATACS (HAMZA) ---
    private void configurarBotonsAtac() {
        
        // A. ACCIÓ NMAP
        btnNmap.addActionListener(e -> {
            String ip = getSelectedIp();
            if (ip == null) return;
            // Llancem l'escaneig en un fil (Thread) independent. És vital fer-ho així, si ho executéssim al fil principal, la interfície gràfica es quedaria totalment 
            // congelada ("No respon") fins que l'Nmap acabés.
            new Thread(() -> {
                System.out.println("\n--- [NMAP] Iniciant anàlisi de serveis a " + ip + " ---");
                String report = nmapService.escanearConNmap(ip);
                System.out.println(report); // Això es veurà a la consola gràcies al Nico
                System.out.println("--- [NMAP] Finalitzat ---");
            }).start();
        });

        // B. ACCIÓ HYDRA
        btnHydra.addActionListener(e -> {
            String ip = getSelectedIp();
            if (ip == null) return;
            
            String port = JOptionPane.showInputDialog(this, "Port objectiu (ex: 22, 21)?");
            if (port == null || port.isEmpty()) return;

            File fUsers = seleccionarFitxer("Tria diccionari Usuaris");
            if (fUsers == null) return;
            
            File fPass = seleccionarFitxer("Tria diccionari Passwords");
            if (fPass == null) return;

            new Thread(() -> {
                try {
                    int p = Integer.parseInt(port);
                    bruteForceService.atacar(ip, p, fUsers.getAbsolutePath(), fPass.getAbsolutePath());
                } catch (NumberFormatException ex) {
                    System.err.println("Error: El port ha de ser un número.");
                }
            }).start();
        });

        // C. ACCIÓ FFUF
        btnFfuf.addActionListener(e -> {
            String ip = getSelectedIp();
            if (ip == null) return;

            String port = JOptionPane.showInputDialog(this, "Port Web (ex: 80, 8080)?", "80");
            File fWordlist = seleccionarFitxer("Tria wordlist de directoris");
            if (fWordlist == null) return;

            new Thread(() -> {
                fuzzingService.lanzarFuzzing(ip, Integer.parseInt(port), fWordlist.getAbsolutePath());
            }).start();
        });
    }

    // --- 3. UTILITATS DEL NICO (REDIRECTOR) ---
    private void configurarLogs() {
        // Redirigim tot el que surt per consola (System.out) cap al quadre de text de l'app. Així l'usuari pot veure el progrés de les eines (Nmap, Hydra...) 
        // en temps real sense haver de mirar l'output de l'Eclipse/IntelliJ.
        ConsoleRedirector redirector = new ConsoleRedirector(txtConsole);
        PrintStream outStream = new PrintStream(redirector);
        
        // Redirigim tant la sortida normal com els errors
        System.setOut(outStream);
        System.setErr(outStream);
    }

    // --- HELPERS ---
    
    private void checkDependencies() {
        System.out.println("Verificant entorn...");
        // Comprovem si l'Nmap està instal·lat i accessible des del sistema. Com que la nostra app fa de "wrapper" (llança comandes externes), 
        // si l'usuari no té l'Nmap instal·lat, el botó fallarà.
        if (!nmapService.checkNmapInstalled()) {
            System.err.println("[AVÍS] Nmap no detectat. El botó de Nmap fallarà.");
        } else {
            System.out.println("[OK] Nmap llest.");
        }
    }

    private String getSelectedIp() {
        int row = taula.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Primer selecciona una IP de la taula!");
            return null;
        }
        return (String) modelTaula.getValueAt(row, 0);
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
        btn.setMaximumSize(new Dimension(200, 40));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        return btn;
    }

    private String trobarLaMevaXarxa() {
        try {
            InetAddress jo = InetAddress.getLocalHost();
            String laMevaIp = jo.getHostAddress(); 
            // Tallem la IP (ex: 192.168.1.33) per quedar-nos només amb el rang de xarxa.
            return laMevaIp.substring(0, laMevaIp.lastIndexOf(".") + 1);
        } catch (Exception e) {
        // Si falla la detecció automàtica, posem la IP estàndard per defecte.
        return "192.168.1."; }
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