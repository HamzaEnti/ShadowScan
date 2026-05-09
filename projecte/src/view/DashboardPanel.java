package view;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.swing.*;
import model.ResultatHost;
import model.RiskLevel;
import report.PdfReport;

/**
 * Dashboard amb estadístiques agregades dels resultats d'escaneig.
 *
 * Inclou:
 *   - 4 KPIs (hosts totals, actius, ports oberts, hosts crítics)
 *   - Pie chart de distribució de risc
 *   - Bar chart dels top-10 ports més freqüents
 *   - Bar chart de hosts per nivell de risc (alternativa numèrica)
 *
 * Es refresca quan l'usuari prem "Actualitzar" o quan canvia de pestanya.
 */
public class DashboardPanel extends BasePanel {

    // Bug: tenia un camp 'parent' redundant que s'assignava al constructor
    // DESPRÉS de super(parent). Si algú cridava refrescar() durant la
    // construcció, NPE. Reusem el parentFrame heretat de BasePanel,
    // que sí està inicialitzat abans d'initComponents().
    private DashboardCanvas canvas;
    private JButton btnRefresh;
    private JButton btnExportPdf;

    public DashboardPanel(MainFrame parent) {
        super(parent);
    }

    @Override
    protected void initComponents() {
        setLayout(new BorderLayout());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.setBorder(BorderFactory.createTitledBorder("Dashboard"));

        btnRefresh = crearBoto("Actualitzar");
        btnExportPdf = crearBoto("Exportar PDF");

        btnRefresh.addActionListener(e -> refrescar());
        btnExportPdf.addActionListener(e -> exportarPdf());

        toolbar.add(btnRefresh);
        toolbar.add(btnExportPdf);

        canvas = new DashboardCanvas();
        canvas.setBackground(Color.WHITE);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(canvas), BorderLayout.CENTER);
    }

    /** Refresca dades llegint el panel de Discovery. */
    public void refrescar() {
        canvas.dades = parentFrame != null ? parentFrame.getDiscoveryPanel().getResultats() : List.of();
        canvas.repaint();
    }

    private void exportarPdf() {
        List<ResultatHost> dades = parentFrame.getDiscoveryPanel().getResultats();
        if (dades.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No hi ha dades. Fes primer un escaneig.",
                "Dashboard buit", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File f = guardarFitxer("Exportar informe PDF", "shadowscan-report.pdf");
        if (f == null) return;
        try {
            PdfReport.write(f, dades);
            JOptionPane.showMessageDialog(this,
                "Informe PDF generat correctament:\n" + f.getName(),
                "PDF", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error generant PDF: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Càlculs estadístics reutilitzats per UI i PDF. */
    public static Stats compute(List<ResultatHost> dades) {
        Stats s = new Stats();
        if (dades == null) return s;
        s.total = dades.size();
        for (ResultatHost h : dades) {
            if (h.isEsViu()) s.actius++;
            int n = h.getPortsOberts().size();
            s.totalPorts += n;
            RiskLevel r = h.getRiskLevel();
            s.perRisc.merge(r.getEtiqueta(), 1, Integer::sum);
            for (Integer p : h.getPortsOberts()) {
                s.perPort.merge(p, 1, Integer::sum);
            }
            if (r == RiskLevel.CRITIC) s.critics++;
        }
        return s;
    }

    public static final class Stats {
        public int total;
        public int actius;
        public int critics;
        public int totalPorts;
        public final Map<String, Integer> perRisc = new LinkedHashMap<>();
        public final Map<Integer, Integer> perPort = new HashMap<>();

        public List<Map.Entry<Integer, Integer>> topPorts(int n) {
            List<Map.Entry<Integer, Integer>> list = new ArrayList<>(perPort.entrySet());
            list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            return list.subList(0, Math.min(n, list.size()));
        }
    }

    /** Subclasse per pintar el dashboard amb Graphics2D. */
    private static final class DashboardCanvas extends JPanel {
        List<ResultatHost> dades = List.of();

        DashboardCanvas() {
            setPreferredSize(new Dimension(900, 720));
        }

        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg;
            int w = getWidth();

            Stats s = compute(dades);

            // Fila de KPIs
            int kpiY = 16, kpiH = 78, gap = 14;
            int kpiW = (w - 4 * gap) / 4;
            ChartUtil.drawKpi(g, gap,                          kpiY, kpiW, kpiH,
                "Hosts escanejats", String.valueOf(s.total),     new Color(59, 130, 246));
            ChartUtil.drawKpi(g, gap * 2 + kpiW,                kpiY, kpiW, kpiH,
                "Hosts actius",     String.valueOf(s.actius),    new Color(34, 197, 94));
            ChartUtil.drawKpi(g, gap * 3 + kpiW * 2,            kpiY, kpiW, kpiH,
                "Ports oberts",     String.valueOf(s.totalPorts), new Color(251, 191, 36));
            ChartUtil.drawKpi(g, gap * 4 + kpiW * 3,            kpiY, kpiW, kpiH,
                "Hosts crítics",    String.valueOf(s.critics),   new Color(239, 68, 68));

            int chartTop = kpiY + kpiH + 24;
            int chartH = 240;

            // Pie de risc
            List<Map.Entry<String, Integer>> riscData = new ArrayList<>(s.perRisc.entrySet());
            ChartUtil.drawPieChart(g, gap, chartTop, w / 2 - gap * 2, chartH,
                "Distribució de risc", riscData);

            // Bar dels top ports
            List<Map.Entry<String, Integer>> portData = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : s.topPorts(10)) {
                portData.add(Map.entry("Port " + e.getKey(), e.getValue()));
            }
            ChartUtil.drawBarChart(g, w / 2, chartTop, w / 2 - gap, chartH,
                "Top 10 ports oberts", portData);

            // Resum de text al fons
            int yText = chartTop + chartH + 30;
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.setColor(new Color(45, 55, 72));
            g.drawString("Resum executiu", gap, yText);

            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.setColor(new Color(60, 75, 100));
            String[] resum = generarResum(s);
            for (int i = 0; i < resum.length; i++) {
                g.drawString(resum[i], gap, yText + 22 + i * 18);
            }
        }
    }

    public static String[] generarResum(Stats s) {
        List<String> lines = new ArrayList<>();
        if (s.total == 0) {
            lines.add("No hi ha dades. Executa un escaneig al panel de Discovery.");
            return lines.toArray(new String[0]);
        }
        int pctActius = s.total == 0 ? 0 : (int) Math.round(100.0 * s.actius / s.total);
        lines.add("• " + s.actius + " de " + s.total + " hosts són actius (" + pctActius + "%).");
        if (s.actius > 0) {
            lines.add("• Mitjana de " + (s.totalPorts / Math.max(s.actius, 1))
                + " ports oberts per host actiu.");
        }
        if (s.critics > 0) {
            lines.add("• ATENCIÓ: " + s.critics + " host"
                + (s.critics == 1 ? "" : "s") + " amb nivell de risc CRÍTIC.");
        } else {
            lines.add("• Cap host classificat com a crític.");
        }
        return lines.toArray(new String[0]);
    }
}
