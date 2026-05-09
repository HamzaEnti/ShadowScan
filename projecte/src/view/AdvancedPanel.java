package view;

import api.RestApi;
import controller.HostFoundListener;
import distributed.ScanCoordinator;
import distributed.ScanCoordinator.WorkerEndpoint;
import distributed.ScanWorker;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.ResultatHost;
import model.ScanProfile;
import utils.ProfileStore;

/**
 * Panel "Avançat": agrupa les funcionalitats de v2.0 que no encaixen
 * directament als panels de Discovery/Nmap/Security.
 *
 * Tres seccions:
 *   1. Perfils d'escaneig — desar/carregar configuracions
 *   2. API REST — engegar/aturar el servidor HTTP
 *   3. Distribuït — configurar workers i llançar escaneigs distribuïts
 */
public class AdvancedPanel extends BasePanel {

    private final ProfileStore profileStore = new ProfileStore();
    private RestApi restApi;
    private ScanWorker localWorker;

    // Profiles
    private DefaultTableModel mProfiles;
    private JTable tProfiles;
    private JTextField txtPName;
    private JTextField txtPStart;
    private JTextField txtPEnd;
    private JComboBox<String> cmbPMode;
    private JCheckBox chkPUdp;

    // REST
    private JTextField txtRestPort;
    private JTextField txtRestToken;
    private JButton btnRestStart;
    private JButton btnRestStop;
    private JLabel lblRestStatus;

    // Distributed
    private DefaultTableModel mWorkers;
    private JTextField txtWorkerHost;
    private JTextField txtWorkerPort;
    private JTextField txtWorkerToken;
    private JTextField txtDistStart;
    private JTextField txtDistEnd;
    private JTextArea txtDistLog;
    private JButton btnLocalWorkerStart;
    private JButton btnLocalWorkerStop;
    private JTextField txtLocalWorkerPort;
    private JLabel lblLocalWorker;

    public AdvancedPanel(MainFrame parent) {
        super(parent);
    }

    @Override
    protected void initComponents() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Perfils",      buildProfilesTab());
        tabs.addTab("API REST",     buildRestTab());
        tabs.addTab("Distribuït",   buildDistTab());

        add(tabs, BorderLayout.CENTER);
    }

    /* ─── Perfils ─────────────────────────────────────────────────────── */

    private JComponent buildProfilesTab() {
        JPanel pnl = new JPanel(new BorderLayout(8, 8));
        pnl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Nou perfil"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Nom:"), g);
        g.gridx = 1; g.weightx = 1; txtPName = new JTextField(); form.add(txtPName, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0; form.add(new JLabel("IP inici:"), g);
        g.gridx = 1; g.weightx = 1; txtPStart = new JTextField(); form.add(txtPStart, g);

        g.gridx = 0; g.gridy = 2; g.weightx = 0; form.add(new JLabel("IP fi:"), g);
        g.gridx = 1; g.weightx = 1; txtPEnd = new JTextField(); form.add(txtPEnd, g);

        g.gridx = 0; g.gridy = 3; g.weightx = 0; form.add(new JLabel("Mode:"), g);
        g.gridx = 1; cmbPMode = new JComboBox<>(new String[]{"PARCIAL", "FULL"}); form.add(cmbPMode, g);

        g.gridx = 0; g.gridy = 4; g.gridwidth = 2;
        chkPUdp = new JCheckBox("Incloure ports UDP comuns"); form.add(chkPUdp, g);

        g.gridx = 0; g.gridy = 5; g.gridwidth = 2;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSave = crearBoto("Desar perfil");
        JButton btnApply = crearBoto("Aplicar a Discovery");
        JButton btnDelete = crearBoto("Eliminar");
        actions.add(btnSave);
        actions.add(btnApply);
        actions.add(btnDelete);
        form.add(actions, g);

        // Taula
        mProfiles = new DefaultTableModel(new String[]{"Nom", "Rang", "Mode", "UDP"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tProfiles = new JTable(mProfiles);
        tProfiles.setRowHeight(22);
        tProfiles.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tProfiles.getSelectedRow();
            if (row < 0) return;
            txtPName.setText((String) mProfiles.getValueAt(row, 0));
            String[] rang = ((String) mProfiles.getValueAt(row, 1)).split("→");
            if (rang.length == 2) {
                txtPStart.setText(rang[0].trim());
                txtPEnd.setText(rang[1].trim());
            }
            cmbPMode.setSelectedItem(mProfiles.getValueAt(row, 2));
            chkPUdp.setSelected(Boolean.TRUE.equals(mProfiles.getValueAt(row, 3)));
        });

        btnSave.addActionListener(e -> saveProfile());
        btnApply.addActionListener(e -> applyProfile());
        btnDelete.addActionListener(e -> deleteProfile());

        pnl.add(form, BorderLayout.NORTH);
        pnl.add(new JScrollPane(tProfiles), BorderLayout.CENTER);

        refreshProfiles();
        return pnl;
    }

    private void refreshProfiles() {
        mProfiles.setRowCount(0);
        for (ScanProfile p : profileStore.loadAll()) {
            mProfiles.addRow(new Object[]{
                p.getName(),
                p.getStartIp() + " → " + p.getEndIp(),
                p.getMode(),
                p.isUdp()
            });
        }
    }

    private void saveProfile() {
        String name = txtPName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nom és obligatori.", "Falten dades",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        ScanProfile p = new ScanProfile(name,
            txtPStart.getText().trim(),
            txtPEnd.getText().trim(),
            (String) cmbPMode.getSelectedItem());
        p.setUdp(chkPUdp.isSelected());
        try {
            profileStore.save(p);
            refreshProfiles();
            System.out.println(">>> [PROFILE] Desat: " + name);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error desant: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyProfile() {
        if (parentFrame == null) return;
        DiscoveryPanel dp = parentFrame.getDiscoveryPanel();
        dp.setIpInici(txtPStart.getText().trim());
        dp.setIpFi(txtPEnd.getText().trim());
        JOptionPane.showMessageDialog(this,
            "Configuració aplicada al panel de Discovery.",
            "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteProfile() {
        String name = txtPName.getText().trim();
        if (name.isEmpty()) return;
        try {
            if (profileStore.delete(name)) refreshProfiles();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error eliminant: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ─── REST API ────────────────────────────────────────────────────── */

    private JComponent buildRestTab() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; pnl.add(new JLabel("Port:"), g);
        g.gridx = 1; g.weightx = 1;
        txtRestPort = new JTextField("8765");
        pnl.add(txtRestPort, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0; pnl.add(new JLabel("Token (opcional):"), g);
        g.gridx = 1; g.weightx = 1;
        txtRestToken = new JTextField();
        pnl.add(txtRestToken, g);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRestStart = crearBoto("Engegar API", COLOR_VERD);
        btnRestStop = crearBoto("Aturar API", COLOR_SALMO);
        btnRestStop.setEnabled(false);
        buttons.add(btnRestStart);
        buttons.add(btnRestStop);
        pnl.add(buttons, g);

        g.gridy = 3;
        lblRestStatus = new JLabel("API aturada.");
        pnl.add(lblRestStatus, g);

        g.gridy = 4;
        JTextArea endpoints = new JTextArea(
            "Endpoints disponibles:\n"
            + "  GET  /api/health\n"
            + "  GET  /api/profiles\n"
            + "  POST /api/profiles\n"
            + "  POST /api/scans   {start,end,mode,udp?}\n"
            + "  GET  /api/scans\n"
            + "  GET  /api/scans/{id}\n"
            + "  GET  /api/scans/{id}/topology\n"
            + "  GET  /api/cve?service=X[&version=Y][&offline=true]\n");
        endpoints.setEditable(false);
        endpoints.setFont(new Font("Consolas", Font.PLAIN, 12));
        endpoints.setBackground(new Color(245, 245, 248));
        pnl.add(endpoints, g);

        btnRestStart.addActionListener(e -> startRest());
        btnRestStop.addActionListener(e -> stopRest());

        return pnl;
    }

    private void startRest() {
        try {
            int port = Integer.parseInt(txtRestPort.getText().trim());
            String token = txtRestToken.getText().trim();
            restApi = new RestApi(port, token.isEmpty() ? null : token);
            restApi.start();
            btnRestStart.setEnabled(false);
            btnRestStop.setEnabled(true);
            lblRestStatus.setText("API escoltant a http://localhost:" + port + "/api/");
            lblRestStatus.setForeground(new Color(0, 128, 0));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "REST API", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopRest() {
        if (restApi != null) restApi.stop();
        btnRestStart.setEnabled(true);
        btnRestStop.setEnabled(false);
        lblRestStatus.setText("API aturada.");
        lblRestStatus.setForeground(Color.GRAY);
    }

    /* ─── Distribuït ──────────────────────────────────────────────────── */

    private JComponent buildDistTab() {
        JPanel pnl = new JPanel(new BorderLayout(8, 8));
        pnl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Nord: configurar workers
        JPanel pnlAdd = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pnlAdd.setBorder(BorderFactory.createTitledBorder("Workers remots"));
        pnlAdd.add(new JLabel("Host:"));
        txtWorkerHost = new JTextField(12); pnlAdd.add(txtWorkerHost);
        pnlAdd.add(new JLabel("Port:"));
        txtWorkerPort = new JTextField("9876", 5); pnlAdd.add(txtWorkerPort);
        pnlAdd.add(new JLabel("Token:"));
        txtWorkerToken = new JTextField(8); pnlAdd.add(txtWorkerToken);

        JButton btnAddWorker = crearBoto("Afegir worker");
        JButton btnRemoveWorker = crearBoto("Treure");
        pnlAdd.add(btnAddWorker);
        pnlAdd.add(btnRemoveWorker);

        mWorkers = new DefaultTableModel(new String[]{"Host", "Port", "Token"}, 0);
        JTable tWorkers = new JTable(mWorkers);
        tWorkers.setRowHeight(22);

        btnAddWorker.addActionListener(e -> {
            String h = txtWorkerHost.getText().trim();
            String p = txtWorkerPort.getText().trim();
            if (h.isEmpty() || p.isEmpty()) return;
            mWorkers.addRow(new Object[]{h, p, txtWorkerToken.getText().trim()});
            txtWorkerHost.setText("");
        });
        btnRemoveWorker.addActionListener(e -> {
            int r = tWorkers.getSelectedRow();
            if (r >= 0) mWorkers.removeRow(r);
        });

        // Centre: rang i log
        JPanel pnlRun = new JPanel(new GridBagLayout());
        pnlRun.setBorder(BorderFactory.createTitledBorder("Llançar escaneig distribuït"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; pnlRun.add(new JLabel("Rang IP inici:"), g);
        g.gridx = 1; g.weightx = 1; txtDistStart = new JTextField(); pnlRun.add(txtDistStart, g);
        g.gridx = 2; g.weightx = 0; pnlRun.add(new JLabel("IP fi:"), g);
        g.gridx = 3; g.weightx = 1; txtDistEnd = new JTextField(); pnlRun.add(txtDistEnd, g);
        g.gridx = 4; g.weightx = 0;
        JButton btnLaunch = crearBoto("Llançar", COLOR_VERD);
        pnlRun.add(btnLaunch, g);

        // Worker local
        JPanel pnlLocal = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pnlLocal.setBorder(BorderFactory.createTitledBorder("Worker local (per testejar)"));
        pnlLocal.add(new JLabel("Port:"));
        txtLocalWorkerPort = new JTextField("9876", 5);
        pnlLocal.add(txtLocalWorkerPort);
        btnLocalWorkerStart = crearBoto("Engegar worker local", COLOR_VERD);
        btnLocalWorkerStop = crearBoto("Aturar", COLOR_SALMO);
        btnLocalWorkerStop.setEnabled(false);
        lblLocalWorker = new JLabel("(aturat)");
        pnlLocal.add(btnLocalWorkerStart);
        pnlLocal.add(btnLocalWorkerStop);
        pnlLocal.add(lblLocalWorker);

        btnLocalWorkerStart.addActionListener(e -> startLocalWorker());
        btnLocalWorkerStop.addActionListener(e -> stopLocalWorker());

        // Log de sortida
        txtDistLog = new JTextArea();
        txtDistLog.setEditable(false);
        txtDistLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtDistLog.setBackground(new Color(20, 24, 32));
        txtDistLog.setForeground(new Color(80, 220, 100));
        JScrollPane scrollLog = new JScrollPane(txtDistLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Resultats"));

        btnLaunch.addActionListener(e -> launchDistributed());

        JPanel north = new JPanel(new BorderLayout(0, 6));
        north.add(pnlAdd, BorderLayout.NORTH);
        north.add(new JScrollPane(tWorkers), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.add(pnlRun, BorderLayout.NORTH);
        center.add(pnlLocal, BorderLayout.CENTER);
        center.add(scrollLog, BorderLayout.SOUTH);

        pnl.add(north, BorderLayout.NORTH);
        pnl.add(center, BorderLayout.CENTER);

        return pnl;
    }

    private void startLocalWorker() {
        try {
            int port = Integer.parseInt(txtLocalWorkerPort.getText().trim());
            localWorker = new ScanWorker(port, null);
            localWorker.start();
            btnLocalWorkerStart.setEnabled(false);
            btnLocalWorkerStop.setEnabled(true);
            lblLocalWorker.setText("escoltant a localhost:" + port);
            lblLocalWorker.setForeground(new Color(0, 128, 0));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "Worker local", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopLocalWorker() {
        if (localWorker != null) localWorker.stop();
        btnLocalWorkerStart.setEnabled(true);
        btnLocalWorkerStop.setEnabled(false);
        lblLocalWorker.setText("(aturat)");
        lblLocalWorker.setForeground(Color.GRAY);
    }

    private void launchDistributed() {
        String start = txtDistStart.getText().trim();
        String end = txtDistEnd.getText().trim();
        if (start.isEmpty() || end.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indica el rang d'IPs.",
                "Rang requerit", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<WorkerEndpoint> endpoints = new ArrayList<>();
        for (int i = 0; i < mWorkers.getRowCount(); i++) {
            String h = (String) mWorkers.getValueAt(i, 0);
            int p = Integer.parseInt((String) mWorkers.getValueAt(i, 1));
            String t = (String) mWorkers.getValueAt(i, 2);
            endpoints.add(new WorkerEndpoint(h, p, (t == null || t.isEmpty()) ? null : t));
        }
        if (endpoints.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Afegeix com a mínim un worker.",
                "Sense workers", JOptionPane.WARNING_MESSAGE);
            return;
        }

        txtDistLog.setText("");
        appendLog("Llançant a " + endpoints.size() + " workers...");

        ScanCoordinator coord = new ScanCoordinator(endpoints);
        HostFoundListener listener = (ResultatHost h) ->
            SwingUtilities.invokeLater(() -> appendLog(h.toDisplayString()));

        new Thread(() -> {
            try {
                List<ResultatHost> all = coord.executeDistributed(start, end, "PARCIAL", false, listener);
                SwingUtilities.invokeLater(() -> appendLog("\n>>> Total: " + all.size() + " hosts trobats."));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> appendLog("ERROR: " + ex.getMessage()));
            }
        }, "coord-launch").start();
    }

    private void appendLog(String s) {
        txtDistLog.append(s + "\n");
        txtDistLog.setCaretPosition(txtDistLog.getDocument().getLength());
    }
}
