package view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.CveEntry;
import services.CveService;

/**
 * Panel per consultar la base de dades NVD de CVEs per servei.
 *
 * Inclou un toggle per al mode offline (catàleg embedded) que és útil per
 * a demos i quan NVD està rate-limited (5 req/30s sense API key).
 */
public class CvePanel extends BasePanel {

    private JTextField txtServei;
    private JTextField txtVersio;
    private JButton btnBuscar;
    private JCheckBox chkOffline;
    private JTable taula;
    private DefaultTableModel modelTaula;
    private JLabel lblEstat;

    // Lazy-init dins d'initComponents(): BasePanel.<init> crida initComponents
    // abans que els field initializers de la subclasse s'executin.
    private CveService cveService;

    public CvePanel(MainFrame parent) {
        super(parent);
    }

    @Override
    protected void initComponents() {
        this.cveService = new CveService();

        setLayout(new BorderLayout());

        JPanel pnlNord = new JPanel(new GridBagLayout());
        pnlNord.setBorder(BorderFactory.createTitledBorder("Cerca de CVEs (NVD)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        pnlNord.add(new JLabel("Servei (ex: openssh, apache, mysql):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtServei = new JTextField(20);
        pnlNord.add(txtServei, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        pnlNord.add(new JLabel("Versió (opcional):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        txtVersio = new JTextField(20);
        pnlNord.add(txtVersio, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 0;
        JPanel pnlAccions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBuscar = crearBoto("Buscar");
        chkOffline = new JCheckBox("Mode offline (catàleg local)");
        chkOffline.addActionListener(e -> cveService.setOfflineMode(chkOffline.isSelected()));
        pnlAccions.add(btnBuscar);
        pnlAccions.add(chkOffline);
        pnlNord.add(pnlAccions, gbc);

        add(pnlNord, BorderLayout.NORTH);

        // Taula
        String[] cols = {"CVE", "Severitat", "CVSS", "Descripció"};
        modelTaula = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        taula = new JTable(modelTaula);
        taula.setRowHeight(36);
        taula.setFont(new Font("Consolas", Font.PLAIN, 12));
        taula.getColumnModel().getColumn(0).setPreferredWidth(140);
        taula.getColumnModel().getColumn(1).setPreferredWidth(80);
        taula.getColumnModel().getColumn(2).setPreferredWidth(60);
        taula.getColumnModel().getColumn(3).setPreferredWidth(560);
        taula.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String sev = (String) t.getValueAt(row, 1);
                if (!sel) {
                    switch (sev == null ? "" : sev) {
                        case "CRITICAL": c.setBackground(new Color(255, 220, 220)); break;
                        case "HIGH":     c.setBackground(new Color(255, 235, 215)); break;
                        case "MEDIUM":   c.setBackground(new Color(255, 250, 215)); break;
                        case "LOW":      c.setBackground(new Color(230, 245, 230)); break;
                        default:         c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        add(new JScrollPane(taula), BorderLayout.CENTER);

        lblEstat = new JLabel("Introdueix un nom de servei per buscar CVEs.");
        lblEstat.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(lblEstat, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> buscar());
        txtServei.addActionListener(e -> buscar());
    }

    private void buscar() {
        String servei = txtServei.getText().trim();
        String versio = txtVersio.getText().trim();
        if (servei.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cal indicar un nom de servei.",
                "Camp buit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        modelTaula.setRowCount(0);
        lblEstat.setText("Consultant " + (chkOffline.isSelected() ? "catàleg local" : "NVD API") + "...");
        btnBuscar.setEnabled(false);

        new Thread(() -> {
            List<CveEntry> entries = cveService.lookup(servei, versio.isEmpty() ? null : versio);
            SwingUtilities.invokeLater(() -> {
                for (CveEntry e : entries) {
                    modelTaula.addRow(new Object[]{
                        e.getId(),
                        e.getSeverity(),
                        String.format(java.util.Locale.ROOT, "%.1f", e.getCvssScore()),
                        e.getDescription()
                    });
                }
                lblEstat.setText(entries.size() + " CVE(s) trobats per a '" + servei + "'.");
                btnBuscar.setEnabled(true);
            });
        }, "cve-lookup").start();
    }

    public void setServei(String servei) {
        txtServei.setText(servei);
    }

    public CveService getCveService() {
        return cveService;
    }
}
