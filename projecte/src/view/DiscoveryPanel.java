package view;

import controller.HostFoundListener;
import controller.PortScanMode;
import controller.ScanController;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import model.ResultatHost;
import model.RiskLevel;
import utils.JsonExporter;

public class DiscoveryPanel extends BasePanel implements HostFoundListener {

    private JTextField txtIpInici;
    private JTextField txtIpFi;
    private JRadioButton rbPortsComuns;
    private JRadioButton rbTotsPorts;

    private JButton btnStart;
    private JButton btnStop;
    private JButton btnExport;
    private JButton btnExportRedTrace;

    private JTable taula;
    private DefaultTableModel modelTaula;

    private JProgressBar progressBar;
    private JLabel lblProgress;
    private int hostsEscanejats = 0;

    private ScanController controller;
    private List<ResultatHost> resultats;

    public DiscoveryPanel(MainFrame parent) {
        super(parent);
    }

    @Override
    protected void initComponents() {
        this.resultats = new ArrayList<>();
        // FIX: el controlador ja no rep MainFrame; rep aquest panel com a listener
        this.controller = new ScanController(this);

        // FIX: callback ara rep el comptador real de hosts trobats
        controller.setCallback(hostsFound -> SwingUtilities.invokeLater(() -> {
            btnExport.setEnabled(true);
            btnExportRedTrace.setEnabled(true);
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            progressBar.setIndeterminate(false);
            progressBar.setValue(100);
            lblProgress.setText("Escaneig finalitzat. " + hostsFound + " hosts actius trobats.");
        }));

        this.setLayout(new BorderLayout());

        JPanel pnlNord = crearPanelNord();
        JPanel pnlCentre = crearPanelTaula();

        this.add(pnlNord, BorderLayout.NORTH);
        this.add(pnlCentre, BorderLayout.CENTER);

        configurarListeners();
    }

    private JPanel crearPanelNord() {
        JPanel pnlNord = new JPanel(new BorderLayout());

        JPanel pnlConfig = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        pnlConfig.setBorder(BorderFactory.createTitledBorder("Configuració de Xarxa"));

        pnlConfig.add(new JLabel("IP Inici:"));
        txtIpInici = crearCampText(12);
        pnlConfig.add(txtIpInici);

        pnlConfig.add(new JLabel("IP Fi:"));
        txtIpFi = crearCampText(12);
        pnlConfig.add(txtIpFi);

        btnStart = crearBoto("Escanejar", COLOR_VERD);
        btnStop  = crearBoto("Aturar", COLOR_SALMO);
        btnStop.setEnabled(false);

        btnExport = crearBoto("Guardar JSON");
        btnExport.setEnabled(false);

        btnExportRedTrace = crearBoto("Exportar RedTrace");
        btnExportRedTrace.setEnabled(false);
        btnExportRedTrace.setBackground(new Color(200, 230, 255));

        pnlConfig.add(btnStart);
        pnlConfig.add(btnStop);
        pnlConfig.add(btnExport);
        pnlConfig.add(btnExportRedTrace);

        JPanel pnlMode = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        pnlMode.setBorder(BorderFactory.createTitledBorder("Mode d'escaneig"));

        rbPortsComuns = new JRadioButton("Ports comuns (ràpid)", true);
        rbTotsPorts   = new JRadioButton("Tots els ports (lent)");

        ButtonGroup grup = new ButtonGroup();
        grup.add(rbPortsComuns);
        grup.add(rbTotsPorts);

        pnlMode.add(rbPortsComuns);
        pnlMode.add(rbTotsPorts);

        JPanel pnlProgress = new JPanel(new BorderLayout(8, 0));
        pnlProgress.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        lblProgress = new JLabel("Prem Escanejar per començar.");
        lblProgress.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblProgress.setForeground(Color.GRAY);
        pnlProgress.add(progressBar, BorderLayout.CENTER);
        pnlProgress.add(lblProgress, BorderLayout.EAST);

        pnlNord.add(pnlConfig,   BorderLayout.NORTH);
        pnlNord.add(pnlMode,     BorderLayout.CENTER);
        pnlNord.add(pnlProgress, BorderLayout.SOUTH);

        return pnlNord;
    }

    private JPanel crearPanelTaula() {
        JPanel pnl = new JPanel(new BorderLayout());

        // MILLORA: nova columna Hostname
        String[] columnes = {"IP Adreça", "Hostname", "Estat", "Ports Detectats", "Risc"};
        modelTaula = new DefaultTableModel(columnes, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        taula = new JTable(modelTaula);
        taula.setRowHeight(22);
        taula.setFont(new Font("Consolas", Font.PLAIN, 12));
        taula.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        taula.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taula.setGridColor(new Color(230, 230, 230));

        taula.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String risc = (String) t.getValueAt(row, 4);
                if (!sel) {
                    if (RiskLevel.CRITIC.getEtiqueta().equals(risc))
                        c.setBackground(new Color(255, 230, 230));
                    else if (RiskLevel.MITJA.getEtiqueta().equals(risc))
                        c.setBackground(new Color(255, 248, 220));
                    else
                        c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(taula);
        scroll.setBorder(BorderFactory.createTitledBorder("Hosts Descoberts"));
        pnl.add(scroll, BorderLayout.CENTER);

        return pnl;
    }

    private void configurarListeners() {
        btnStart.addActionListener(e -> iniciarEscaneig());
        btnStop.addActionListener(e -> {
            controller.aturar();
            btnStop.setEnabled(false);
            btnStart.setEnabled(true);
            progressBar.setIndeterminate(false);
            lblProgress.setText("Escaneig aturat per l'usuari.");
        });
        btnExport.addActionListener(e -> exportarResultats());
        btnExportRedTrace.addActionListener(e -> exportarRedTrace());
    }

    private void iniciarEscaneig() {
        String ipInici = txtIpInici.getText().trim();
        String ipFi    = txtIpFi.getText().trim();

        if (!ipValida(ipInici)) {
            JOptionPane.showMessageDialog(this,
                "IP d'inici invàlida (ex: 192.168.1.1)",
                "IP incorrecta", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!ipValida(ipFi)) {
            JOptionPane.showMessageDialog(this,
                "IP de fi invàlida (ex: 192.168.1.254)",
                "IP incorrecta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnExport.setEnabled(false);
        btnExportRedTrace.setEnabled(false);
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);

        progressBar.setIndeterminate(true);
        hostsEscanejats = 0;
        resultats.clear();
        modelTaula.setRowCount(0);
        lblProgress.setText("Escanejant rang " + ipInici + " → " + ipFi + "...");

        PortScanMode mode = rbPortsComuns.isSelected() ? PortScanMode.PARCIAL : PortScanMode.FULL;
        // MILLORA: usem el rang real introduït per l'usuari
        controller.escanearRang(ipInici, ipFi, mode);
    }

    private void exportarRedTrace() {
        File fitxer = guardarFitxer("Guardar topologia RedTrace", "topology.json");
        if (fitxer != null) {
            if (JsonExporter.saveToTopology(resultats, fitxer.getAbsolutePath())) {
                JOptionPane.showMessageDialog(this,
                    "Topologia RedTrace guardada!\n" + fitxer.getName(),
                    "Export OK", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error guardant la topologia",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarResultats() {
        File fitxer = guardarFitxer("Guardar resultats", "resultats.json");
        if (fitxer != null) {
            if (JsonExporter.saveToJSON(resultats, fitxer.getAbsolutePath())) {
                JOptionPane.showMessageDialog(this, "Resultats guardats correctament!");
            } else {
                JOptionPane.showMessageDialog(this, "Error guardant els resultats",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Implementació de HostFoundListener: el controller crida aquí cada
     * vegada que un host és trobat. Garantim execució al EDT.
     */
    @Override
    public void onHostFound(ResultatHost host) {
        SwingUtilities.invokeLater(() -> afegirResultat(host));
    }

    public synchronized void afegirResultat(ResultatHost host) {
        resultats.add(host);
        hostsEscanejats++;

        // FIX: la classificació de risc viu al model, no al panel
        String risc = host.getRiskLevel().getEtiqueta();
        String hostname = host.getHostname() != null ? host.getHostname() : "—";

        modelTaula.addRow(new Object[]{
            host.getIp(),
            hostname,
            "[" + host.getEstat() + "]",
            host.getPortsOberts().toString(),
            risc
        });

        lblProgress.setText("Hosts trobats: " + hostsEscanejats);
    }

    private boolean ipValida(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        String regex = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
        return ip.matches(regex);
    }

    public void setIpInici(String ip) { txtIpInici.setText(ip); }
    public void setIpFi(String ip)    { txtIpFi.setText(ip); }

    public ScanController getController() { return controller; }
    public List<ResultatHost> getResultats() { return resultats; }
}
