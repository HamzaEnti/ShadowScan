package view;

import java.awt.*;
import java.io.PrintStream;
import java.net.InetAddress;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import model.ResultatHost;

public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;
    private DiscoveryPanel discoveryPanel;
    private NmapPanel nmapPanel;
    private SecurityPanel securityPanel;
    private DashboardPanel dashboardPanel;
    private CvePanel cvePanel;
    private AdvancedPanel advancedPanel;
    private JTextArea txtConsole;

    private JButton btnNavDiscovery;
    private JButton btnNavNmap;
    private JButton btnNavSecurity;
    private JButton btnNavDashboard;
    private JButton btnNavCve;
    private JButton btnNavAdvanced;

    // Colors del tema fosc per al menú lateral
    // Assisted by Claude (Anthropic) — dark sidebar design
    private static final Color SIDEBAR_BG      = new Color(28, 32, 40);
    private static final Color SIDEBAR_BTN_BG  = new Color(40, 46, 58);
    private static final Color SIDEBAR_BTN_HOV = new Color(55, 65, 85);
    private static final Color SIDEBAR_BTN_SEL = new Color(59, 130, 246);
    private static final Color SIDEBAR_TEXT    = new Color(200, 210, 230);
    private static final Color SIDEBAR_TITLE   = new Color(100, 130, 200);

    public MainFrame() {
        configurarFinestra();
        initConsole();
        initPantalla();
        configurarLogs();
        autoDetectarIp();
        checkDependenciesAsync();

        System.out.println(">>> [INIT] ShadowScan iniciada correctament.");
    }

    private void configurarFinestra() {
        this.setTitle("ShadowScan v2.0 — Network Security Toolkit");
        this.setSize(1200, 800);
        this.setMinimumSize(new Dimension(900, 600));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.setLocationRelativeTo(null);

        // MILLORA: icona a la barra de títol (si existeix)
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icon.png"));
            this.setIconImage(icon.getImage());
        } catch (Exception ignored) {}
    }

    private void initPantalla() {
        tabbedPane = new JTabbedPane();
        // Tabs ocults — la navegació és pel menú lateral
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int p, int h, int m) { return 0; }
        });

        discoveryPanel = new DiscoveryPanel(this);
        nmapPanel      = new NmapPanel(this);
        securityPanel  = new SecurityPanel(this);
        dashboardPanel = new DashboardPanel(this);
        cvePanel       = new CvePanel(this);
        advancedPanel  = new AdvancedPanel(this);

        tabbedPane.addTab("Discovery", discoveryPanel);
        tabbedPane.addTab("Nmap",      nmapPanel);
        tabbedPane.addTab("Security",  securityPanel);
        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("CVE",       cvePanel);
        tabbedPane.addTab("Advanced",  advancedPanel);

        this.add(tabbedPane, BorderLayout.CENTER);
        crearMenuLateral();
    }

    // MILLORA: menú lateral fosc professional
    private void crearMenuLateral() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(SIDEBAR_BG);

        // Logo / títol
        JLabel lblLogo = new JLabel("SHADOW");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 20));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblLogoSub = new JLabel("SCAN");
        lblLogoSub.setFont(new Font("Arial", Font.BOLD, 20));
        lblLogoSub.setForeground(SIDEBAR_TITLE);
        lblLogoSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(50, 60, 80));
        sep.setMaximumSize(new Dimension(180, 1));

        JLabel lblSection = new JLabel("MODULES");
        lblSection.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblSection.setForeground(new Color(90, 110, 150));
        lblSection.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnNavDiscovery = crearBotoSidebar("  Network Scan", "1");
        btnNavNmap      = crearBotoSidebar("  Nmap Analyzer", "2");
        btnNavSecurity  = crearBotoSidebar("  Security Tools", "3");
        btnNavDashboard = crearBotoSidebar("  Dashboard", "4");
        btnNavCve       = crearBotoSidebar("  CVE Lookup", "5");
        btnNavAdvanced  = crearBotoSidebar("  Advanced (v2.0)", "6");

        // selecció inicial
        marcarBotoSeleccionat(btnNavDiscovery);

        sidebar.add(Box.createVerticalStrut(24));
        sidebar.add(lblLogo);
        sidebar.add(lblLogoSub);
        sidebar.add(Box.createVerticalStrut(16));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(lblSection);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnNavDiscovery);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavNmap);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavSecurity);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavDashboard);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavCve);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavAdvanced);
        sidebar.add(Box.createVerticalGlue());

        // versió al peu del sidebar
        JLabel lblVer = new JLabel("v2.0.0");
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblVer.setForeground(new Color(70, 85, 110));
        lblVer.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblVer);
        sidebar.add(Box.createVerticalStrut(12));

        // listeners
        btnNavDiscovery.addActionListener(e -> {
            tabbedPane.setSelectedIndex(0);
            marcarBotoSeleccionat(btnNavDiscovery);
            desmarcarBotons(btnNavNmap, btnNavSecurity, btnNavDashboard, btnNavCve, btnNavAdvanced);
        });
        btnNavNmap.addActionListener(e -> {
            tabbedPane.setSelectedIndex(1);
            marcarBotoSeleccionat(btnNavNmap);
            desmarcarBotons(btnNavDiscovery, btnNavSecurity, btnNavDashboard, btnNavCve, btnNavAdvanced);
        });
        btnNavSecurity.addActionListener(e -> {
            tabbedPane.setSelectedIndex(2);
            marcarBotoSeleccionat(btnNavSecurity);
            desmarcarBotons(btnNavDiscovery, btnNavNmap, btnNavDashboard, btnNavCve, btnNavAdvanced);
        });
        btnNavDashboard.addActionListener(e -> {
            tabbedPane.setSelectedIndex(3);
            marcarBotoSeleccionat(btnNavDashboard);
            desmarcarBotons(btnNavDiscovery, btnNavNmap, btnNavSecurity, btnNavCve, btnNavAdvanced);
            dashboardPanel.refrescar();
        });
        btnNavCve.addActionListener(e -> {
            tabbedPane.setSelectedIndex(4);
            marcarBotoSeleccionat(btnNavCve);
            desmarcarBotons(btnNavDiscovery, btnNavNmap, btnNavSecurity, btnNavDashboard, btnNavAdvanced);
        });
        btnNavAdvanced.addActionListener(e -> {
            tabbedPane.setSelectedIndex(5);
            marcarBotoSeleccionat(btnNavAdvanced);
            desmarcarBotons(btnNavDiscovery, btnNavNmap, btnNavSecurity, btnNavDashboard, btnNavCve);
        });

        this.add(sidebar, BorderLayout.WEST);
    }

    private JButton crearBotoSidebar(String text, String num) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(185, 42));
        btn.setPreferredSize(new Dimension(185, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(SIDEBAR_BTN_BG);
        btn.setForeground(SIDEBAR_TEXT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.getBackground() != SIDEBAR_BTN_SEL) {
                    btn.setBackground(SIDEBAR_BTN_HOV);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.getBackground() != SIDEBAR_BTN_SEL) {
                    btn.setBackground(SIDEBAR_BTN_BG);
                }
            }
        });

        return btn;
    }

    private void marcarBotoSeleccionat(JButton btn) {
        btn.setBackground(SIDEBAR_BTN_SEL);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private void desmarcarBotons(JButton... botons) {
        for (JButton b : botons) {
            b.setBackground(SIDEBAR_BTN_BG);
            b.setForeground(SIDEBAR_TEXT);
            b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
    }

    // MILLORA: consola amb altura reduïda i botó de clear
    private void initConsole() {
        txtConsole = new JTextArea(7, 0);
        txtConsole.setBackground(new Color(18, 20, 26));
        txtConsole.setForeground(new Color(80, 220, 100));
        txtConsole.setCaretColor(new Color(80, 220, 100));
        txtConsole.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtConsole.setEditable(false);
        txtConsole.setMargin(new Insets(6, 8, 6, 8));

        JScrollPane scroll = new JScrollPane(txtConsole);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 60, 80)));

        // MILLORA: panel inferior amb títol i botó clear
        JPanel pnlConsola = new JPanel(new BorderLayout());
        pnlConsola.setBackground(new Color(22, 26, 34));

        JPanel pnlConsolaHeader = new JPanel(new BorderLayout());
        pnlConsolaHeader.setBackground(new Color(22, 26, 34));
        pnlConsolaHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel lblConsola = new JLabel("CONSOLA DE LOGS");
        lblConsola.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblConsola.setForeground(new Color(90, 110, 150));

        JButton btnClear = new JButton("Netejar");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClear.setFocusPainted(false);
        btnClear.addActionListener(e -> txtConsole.setText(""));

        pnlConsolaHeader.add(lblConsola, BorderLayout.WEST);
        pnlConsolaHeader.add(btnClear, BorderLayout.EAST);

        pnlConsola.add(pnlConsolaHeader, BorderLayout.NORTH);
        pnlConsola.add(scroll, BorderLayout.CENTER);

        this.add(pnlConsola, BorderLayout.SOUTH);
    }

    private void configurarLogs() {
        ConsoleRedirector redirector = new ConsoleRedirector(txtConsole);
        PrintStream outStream = new PrintStream(redirector);
        System.setOut(outStream);
        System.setErr(outStream);
    }

    private void autoDetectarIp() {
        try {
            String myIp = InetAddress.getLocalHost().getHostAddress();
            String prefix = myIp.substring(0, myIp.lastIndexOf(".") + 1);
            discoveryPanel.setIpInici(myIp);
            discoveryPanel.setIpFi(prefix + "254");
            nmapPanel.setIp(myIp);
            securityPanel.setTargetIp(myIp);
            System.out.println(">>> [INIT] IP Local detectada: " + myIp);
        } catch (Exception e) {
            System.err.println(">>> [ERROR] No s'ha pogut detectar la IP local.");
        }
    }

    private void checkDependenciesAsync() {
        new Thread(() -> {
            System.out.println(">>> [BOOT] Verificant eines externes...");
            SwingUtilities.invokeLater(() -> {
                nmapPanel.verificarInstalacio();
                securityPanel.verificarInstalacions();
                System.out.println(">>> [BOOT] Verificació completada.");
            });
        }).start();
    }

    public synchronized void afegirResultat(ResultatHost host) {
        discoveryPanel.afegirResultat(host);
    }

    public DiscoveryPanel getDiscoveryPanel() { return discoveryPanel; }
    public NmapPanel getNmapPanel()           { return nmapPanel; }
    public SecurityPanel getSecurityPanel()   { return securityPanel; }
    public DashboardPanel getDashboardPanel() { return dashboardPanel; }
    public CvePanel getCvePanel()             { return cvePanel; }
    public AdvancedPanel getAdvancedPanel()   { return advancedPanel; }
}
