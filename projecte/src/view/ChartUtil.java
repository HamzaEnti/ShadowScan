package view;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Utilitats de dibuix per als gràfics del Dashboard. Implementació amb
 * Graphics2D pura per no dependre de cap llibreria externa (JFreeChart, etc.).
 */
public final class ChartUtil {

    private ChartUtil() {}

    public static final Color[] PALETTE = {
        new Color(59, 130, 246),    // blau
        new Color(34, 197, 94),     // verd
        new Color(251, 191, 36),    // groc
        new Color(239, 68, 68),     // vermell
        new Color(168, 85, 247),    // lila
        new Color(20, 184, 166),    // turquesa
        new Color(249, 115, 22),    // taronja
        new Color(99, 102, 241)     // indi
    };

    /**
     * Gràfic de barres horitzontals etiquetades. Les barres s'auto-escalen
     * al màxim del dataset.
     */
    public static void drawBarChart(Graphics2D g, int x, int y, int w, int h,
                                    String title, List<Map.Entry<String, Integer>> data) {
        applyHints(g);

        g.setColor(new Color(45, 55, 72));
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.drawString(title, x, y + 14);

        int top = y + 26;
        int bottom = y + h - 8;
        int chartH = bottom - top;

        if (data.isEmpty()) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            g.drawString("(sense dades)", x + 8, top + chartH / 2);
            return;
        }

        int max = 1;
        for (Map.Entry<String, Integer> e : data) {
            max = Math.max(max, e.getValue());
        }

        int rowH = Math.max(18, chartH / data.size());
        int labelW = 120;
        int barX = x + labelW;
        int barW = w - labelW - 50;

        FontMetrics fm = g.getFontMetrics(new Font("Segoe UI", Font.PLAIN, 11));
        int i = 0;
        for (Map.Entry<String, Integer> e : data) {
            int by = top + i * rowH + 2;
            int bw = (int) ((double) e.getValue() / max * barW);

            g.setColor(new Color(40, 50, 70));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.drawString(truncate(e.getKey(), fm, labelW - 8), x, by + 12);

            g.setColor(PALETTE[i % PALETTE.length]);
            g.fillRoundRect(barX, by, Math.max(2, bw), rowH - 6, 6, 6);

            g.setColor(new Color(20, 30, 50));
            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g.drawString(String.valueOf(e.getValue()), barX + bw + 6, by + 12);
            i++;
        }
    }

    /**
     * Gràfic circular (pie chart) amb llegenda al costat dret.
     */
    public static void drawPieChart(Graphics2D g, int x, int y, int w, int h,
                                    String title, List<Map.Entry<String, Integer>> data) {
        applyHints(g);

        g.setColor(new Color(45, 55, 72));
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.drawString(title, x, y + 14);

        int top = y + 26;
        int diameter = Math.min(h - 30, w / 2 - 20);
        int cx = x + diameter / 2 + 10;
        int cy = top + diameter / 2;

        int total = 0;
        for (Map.Entry<String, Integer> e : data) total += e.getValue();

        if (total == 0) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            g.drawString("(sense dades)", x + 8, top + 20);
            return;
        }

        // arcs
        double angle = 90;
        int i = 0;
        for (Map.Entry<String, Integer> e : data) {
            double sweep = -360.0 * e.getValue() / total;
            g.setColor(PALETTE[i % PALETTE.length]);
            g.fillArc(cx - diameter / 2, cy - diameter / 2, diameter, diameter,
                      (int) angle, (int) sweep);
            angle += sweep;
            i++;
        }

        // forat central per estètica donut
        g.setColor(Color.WHITE);
        int hole = diameter / 2;
        g.fillOval(cx - hole / 2, cy - hole / 2, hole, hole);

        g.setColor(new Color(45, 55, 72));
        g.setFont(new Font("Segoe UI", Font.BOLD, 14));
        String tot = String.valueOf(total);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(tot, cx - fm.stringWidth(tot) / 2, cy + 5);

        // llegenda
        int legendX = cx + diameter / 2 + 20;
        int legendY = top + 4;
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        i = 0;
        for (Map.Entry<String, Integer> e : data) {
            g.setColor(PALETTE[i % PALETTE.length]);
            g.fillRect(legendX, legendY + i * 18, 12, 12);
            g.setColor(new Color(40, 50, 70));
            int pct = (int) Math.round(100.0 * e.getValue() / total);
            g.drawString(e.getKey() + " — " + e.getValue() + " (" + pct + "%)",
                         legendX + 18, legendY + i * 18 + 11);
            i++;
        }
    }

    /**
     * Targeta de KPI (número gran + etiqueta + accent de color).
     */
    public static void drawKpi(Graphics2D g, int x, int y, int w, int h,
                                String label, String value, Color accent) {
        applyHints(g);

        g.setColor(new Color(248, 250, 253));
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setColor(accent);
        g.fillRoundRect(x, y, 4, h, 4, 4);

        g.setColor(new Color(100, 116, 139));
        g.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g.drawString(label.toUpperCase(), x + 14, y + 20);

        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g.drawString(value, x + 14, y + 52);
    }

    private static void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static String truncate(String s, FontMetrics fm, int maxW) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= maxW) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 0 && fm.stringWidth(sb.toString() + "…") > maxW) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString() + "…";
    }
}
