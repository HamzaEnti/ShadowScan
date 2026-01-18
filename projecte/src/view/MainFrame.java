package view;

import java.awt.*;
import java.io.PrintStream;
import java.net.InetAddress;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import model.ResultatHost;

//Finestra principal de l'aplicacio.
public class MainFrame extends JFrame {
    

    // el tabbed pane que conte les pestanyes
    private JTabbedPane tabbedPane;
    
    // els tres panels principals
    private DiscoveryPanel discoveryPanel;
    private NmapPanel nmapPanel;
    private SecurityPanel securityPanel;
    
    // area de text per als logs
    private JTextArea txtConsole;
    
    // botons del menu lateral
    private JButton btnNavDiscovery;
    private JButton btnNavNmap;
    private JButton btnNavSecurity;

    public MainFrame() {
        // configuracio basica de la finestra
        configurarFinestra();
        
        // construim la UI
        initConsole();
        initPantalla();
        configurarLogs();
        
        // inicialitzacions automatiques
        autoDetectarIp();
        checkDependenciesAsync();
        
        System.out.println(">>> [INIT] Aplicacio iniciada correctament");
    }
    
    private void configurarFinestra() {
        this.setTitle("Scanner Security Suite v4.0");
        this.setSize(1100, 750);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.setLocationRelativeTo(null); // centra la finestra
    }

    // INICIALITZACIO DE LA UI
    private void initPantalla() {
        // creem el tabbed pane amb els tabs ocults
        // (navegarem amb el menu lateral)
        tabbedPane = new JTabbedPane();
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
                return 0;
            }
        });

        // creem els panels
        discoveryPanel = new DiscoveryPanel(this);
        nmapPanel = new NmapPanel(this);
        securityPanel = new SecurityPanel(this);

        // els afegim al tabbed pane
        tabbedPane.addTab("Discovery", discoveryPanel);
        tabbedPane.addTab("Nmap", nmapPanel);
        tabbedPane.addTab("Security", securityPanel);

        this.add(tabbedPane, BorderLayout.CENTER);
        
        // creem el menu lateral
        crearMenuLateral();
    }

    private void crearMenuLateral() {
        JPanel panelDreta = new JPanel();
        panelDreta.setLayout(new BoxLayout(panelDreta, BoxLayout.Y_AXIS));
        panelDreta.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        panelDreta.setPreferredSize(new Dimension(220, 0));
        panelDreta.setBackground(new Color(245, 245, 245));

        // titol del menu
        JLabel lblMenu = new JLabel("MENU PRINCIPAL");
        lblMenu.setFont(new Font("Arial", Font.BOLD, 14));
        lblMenu.setAlignmentX(Component.CENTER_ALIGNMENT);

        // botons de navegacio
        btnNavDiscovery = crearBotoMenu("1. NETWORK SCAN");
        btnNavNmap = crearBotoMenu("2. NMAP ANALYZER");
        btnNavSecurity = crearBotoMenu("3. SECURITY TOOLS");

        // afegim amb espais
        panelDreta.add(Box.createVerticalStrut(20));
        panelDreta.add(lblMenu);
        panelDreta.add(Box.createVerticalStrut(20));
        panelDreta.add(btnNavDiscovery);
        panelDreta.add(Box.createVerticalStrut(15));
        panelDreta.add(btnNavNmap);
        panelDreta.add(Box.createVerticalStrut(15));
        panelDreta.add(btnNavSecurity);
        panelDreta.add(Box.createVerticalGlue());

        // listeners - canvien de pestanya
        btnNavDiscovery.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        btnNavNmap.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        btnNavSecurity.addActionListener(e -> tabbedPane.setSelectedIndex(2));

        this.add(panelDreta, BorderLayout.EAST);
    }
    
    private JButton crearBotoMenu(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(190, 45));
        btn.setPreferredSize(new Dimension(190, 45));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(Color.WHITE);
        
        // efecte hover
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

    // Aquí la consola de logs
    private void initConsole() {
        txtConsole = new JTextArea(10, 50);
        txtConsole.setBackground(Color.BLACK);
        txtConsole.setForeground(new Color(50, 205, 50));
        txtConsole.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtConsole.setEditable(false);
        
        JScrollPane scroll = new JScrollPane(txtConsole);
        scroll.setBorder(BorderFactory.createTitledBorder("Logs del Sistema"));
        
        this.add(scroll, BorderLayout.SOUTH);
    }

    private void configurarLogs() {
        // redirigim System.out i System.err cap al JTextArea
        ConsoleRedirector redirector = new ConsoleRedirector(txtConsole);
        PrintStream outStream = new PrintStream(redirector);
        System.setOut(outStream);
        System.setErr(outStream);
    }

    private void autoDetectarIp() {
        try {
            String myIp = InetAddress.getLocalHost().getHostAddress();
            String prefix = myIp.substring(0, myIp.lastIndexOf(".") + 1);
            
            // passem la IP als panels
            discoveryPanel.setIpInici(myIp);
            discoveryPanel.setIpFi(prefix + "254");
            nmapPanel.setIp(myIp);
            securityPanel.setTargetIp(myIp);
            
            System.out.println(">>> [INIT] IP Local detectada: " + myIp);
        } catch (Exception e) {
            System.err.println(">>> [ERROR] No s'ha pogut detectar IP.");
        }
    }

    private void checkDependenciesAsync() {
        new Thread(() -> {
            System.out.println(">>> [BOOT] Verificant eines externes...");
            
            // cada panel verifica les seves dependencies
            SwingUtilities.invokeLater(() -> {
                nmapPanel.verificarInstalacio();
                securityPanel.verificarInstalacions();
                System.out.println(">>> [BOOT] Verificacio completada.");
            });
        }).start();
    }

    //Afegeix un resultat d'escaneig. Aquest metode el crida ScanTask quan troba un host.
    public synchronized void afegirResultat(ResultatHost host) {
        discoveryPanel.afegirResultat(host);
    }
    
    //Retorna el panel de Discovery.
    public DiscoveryPanel getDiscoveryPanel() {
        return discoveryPanel;
    }
    
    //Retorna el panel de Nmap.

    public NmapPanel getNmapPanel() {
        return nmapPanel;
    }
    
    //Retorna el panel de Security.
    public SecurityPanel getSecurityPanel() {
        return securityPanel;
    }
}
