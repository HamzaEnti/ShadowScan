package report;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.ResultatHost;
import view.DashboardPanel;

/**
 * Generador d'informes PDF artesanal — sense iText, sense OpenPDF, sense
 * cap dependència externa.
 *
 * Genera un PDF 1.4 vàlid amb el subset mínim necessari:
 *   - 1 catàleg, 1 arbre de pàgines, fonts Helvetica i Helvetica-Bold
 *   - Text amb operadors PDF (BT/ET, Tf, Td, Tj)
 *   - Multi-pàgina automàtica
 *   - Capçalera, taula de hosts i resum executiu
 *
 * Limitacions: només ASCII bàsic + escape de () \\ específic. Suficient
 * per a un informe tècnic. Validat per Adobe Reader, PDF.js (Firefox/Chrome)
 * i Sumatra.
 *
 * Format de referència: ISO 32000-1, secció 7 (file structure) i 9 (text).
 */
public final class PdfReport {

    private static final int PAGE_W = 612;   // Letter, en punts (72 dpi)
    private static final int PAGE_H = 792;
    private static final int MARGIN = 50;
    private static final int LINE = 14;

    private PdfReport() {}

    public static void write(File out, List<ResultatHost> hosts) throws IOException {
        DashboardPanel.Stats stats = DashboardPanel.compute(hosts);

        Pdf pdf = new Pdf();
        Page p = pdf.newPage();

        // Capçalera
        p.text(MARGIN, PAGE_H - MARGIN, "Helvetica-Bold", 22, "ShadowScan — Informe d'auditoria");
        p.text(MARGIN, PAGE_H - MARGIN - 24, "Helvetica", 10,
            "Generat: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        p.line(MARGIN, PAGE_H - MARGIN - 32, PAGE_W - MARGIN, PAGE_H - MARGIN - 32);

        // Resum executiu
        int y = PAGE_H - MARGIN - 60;
        p.text(MARGIN, y, "Helvetica-Bold", 14, "Resum executiu");
        y -= 22;
        for (String line : DashboardPanel.generarResum(stats)) {
            p.text(MARGIN, y, "Helvetica", 11, line);
            y -= LINE + 2;
        }

        // KPIs
        y -= 12;
        p.text(MARGIN, y, "Helvetica-Bold", 14, "Mètriques");
        y -= 18;
        String[][] kpis = {
            {"Hosts escanejats:", String.valueOf(stats.total)},
            {"Hosts actius:",     String.valueOf(stats.actius)},
            {"Ports oberts:",     String.valueOf(stats.totalPorts)},
            {"Hosts crítics:",    String.valueOf(stats.critics)}
        };
        for (String[] kv : kpis) {
            p.text(MARGIN, y, "Helvetica", 11, kv[0]);
            p.text(MARGIN + 130, y, "Helvetica-Bold", 11, kv[1]);
            y -= LINE;
        }

        // Distribució de risc
        y -= 12;
        p.text(MARGIN, y, "Helvetica-Bold", 14, "Distribució de risc");
        y -= 18;
        for (Map.Entry<String, Integer> e : stats.perRisc.entrySet()) {
            String bar = bar(e.getValue(), Math.max(1, stats.actius), 30);
            p.text(MARGIN, y, "Helvetica", 11,
                String.format("%-10s %s  %d", e.getKey(), bar, e.getValue()));
            y -= LINE;
        }

        // Top ports
        if (!stats.perPort.isEmpty()) {
            y -= 12;
            p.text(MARGIN, y, "Helvetica-Bold", 14, "Top ports oberts");
            y -= 18;
            int max = 1;
            for (Integer v : stats.perPort.values()) max = Math.max(max, v);
            int shown = 0;
            for (Map.Entry<Integer, Integer> e : stats.topPorts(8)) {
                p.text(MARGIN, y, "Helvetica", 11,
                    String.format("Port %-6d %s  %d", e.getKey(),
                        bar(e.getValue(), max, 30), e.getValue()));
                y -= LINE;
                shown++;
                if (shown >= 8) break;
            }
        }

        // Taula de hosts (multi-pàgina)
        y -= 18;
        if (y < MARGIN + 40) { p = pdf.newPage(); y = PAGE_H - MARGIN; }
        p.text(MARGIN, y, "Helvetica-Bold", 14, "Hosts descoberts");
        y -= 18;
        p.text(MARGIN,        y, "Helvetica-Bold", 10, "IP");
        p.text(MARGIN + 110,  y, "Helvetica-Bold", 10, "Hostname");
        p.text(MARGIN + 270,  y, "Helvetica-Bold", 10, "Risc");
        p.text(MARGIN + 320,  y, "Helvetica-Bold", 10, "Ports oberts");
        y -= 14;
        p.line(MARGIN, y + 4, PAGE_W - MARGIN, y + 4);

        for (ResultatHost h : hosts) {
            if (y < MARGIN + 24) { p = pdf.newPage(); y = PAGE_H - MARGIN; }
            String hn = h.getHostname() != null ? h.getHostname() : "—";
            String ports = h.getPortsOberts().toString();
            p.text(MARGIN,       y, "Helvetica", 9, ascii(h.getIp()));
            p.text(MARGIN + 110, y, "Helvetica", 9, ascii(truncate(hn, 28)));
            p.text(MARGIN + 270, y, "Helvetica", 9, h.getRiskLevel().name());
            p.text(MARGIN + 320, y, "Helvetica", 9, ascii(truncate(ports, 50)));
            y -= LINE - 2;
        }

        // Peu
        for (Page page : pdf.pages) {
            page.text(MARGIN, MARGIN - 16, "Helvetica", 9,
                "ShadowScan v1.2 — Informe automatitzat — Auditoria autoritzada únicament");
        }

        Files.write(out.toPath(), pdf.toBytes());
    }

    /** Barra ASCII per als gràfics in-PDF. */
    private static String bar(int value, int max, int width) {
        int filled = (int) Math.round(((double) value / max) * width);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) sb.append(i < filled ? "#" : " ");
        sb.append("]");
        return sb.toString();
    }

    /** PDF estricte 1.4 + Helvetica només suporta Latin1; reduïm a ASCII. */
    private static String ascii(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c < 0x20) continue;
            if (c < 0x7F) sb.append(c);
            else {
                // Aproximacions per a vocals catalanes/castellanes comuns
                switch (c) {
                    case 'à': case 'á': case 'â': case 'ä': sb.append('a'); break;
                    case 'À': case 'Á': sb.append('A'); break;
                    case 'è': case 'é': case 'ê': case 'ë': sb.append('e'); break;
                    case 'È': case 'É': sb.append('E'); break;
                    case 'í': case 'ì': case 'ï': sb.append('i'); break;
                    case 'ó': case 'ò': case 'ô': case 'ö': sb.append('o'); break;
                    case 'ú': case 'ù': case 'ü': sb.append('u'); break;
                    case 'ñ': sb.append('n'); break;
                    case 'ç': sb.append('c'); break;
                    case '·': sb.append('-'); break;
                    case '—': case '–': sb.append('-'); break;
                    case '…': sb.append("..."); break;
                    default: sb.append('?');
                }
            }
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /* ─── Capa de generació de PDF a baix nivell ──────────────────────── */

    private static final class Pdf {
        final List<Page> pages = new ArrayList<>();

        Page newPage() {
            Page p = new Page();
            pages.add(p);
            return p;
        }

        byte[] toBytes() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Llista d'objectes (1-indexed). 0 reservat al header xref.
            List<byte[]> objects = new ArrayList<>();

            // 1: catalog → 2: pages
            objects.add(("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
                .getBytes(StandardCharsets.ISO_8859_1));

            // 2: pages
            StringBuilder kids = new StringBuilder();
            int firstPageObj = 5; // 3=font helv, 4=font helv-bold, després pàgines
            for (int i = 0; i < pages.size(); i++) {
                int pageObjNum = firstPageObj + i * 2;
                kids.append(pageObjNum).append(" 0 R ");
            }
            objects.add(("2 0 obj\n<< /Type /Pages /Count " + pages.size()
                + " /Kids [" + kids.toString().trim() + "] >>\nendobj\n")
                .getBytes(StandardCharsets.ISO_8859_1));

            // 3, 4: fonts
            objects.add(("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")
                .getBytes(StandardCharsets.ISO_8859_1));
            objects.add(("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n")
                .getBytes(StandardCharsets.ISO_8859_1));

            // pàgines + content streams
            for (int i = 0; i < pages.size(); i++) {
                int pageObj = firstPageObj + i * 2;
                int contentObj = pageObj + 1;

                String pageObjStr = pageObj + " 0 obj\n"
                    + "<< /Type /Page /Parent 2 0 R "
                    + "/MediaBox [0 0 " + PAGE_W + " " + PAGE_H + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> "
                    + "/Contents " + contentObj + " 0 R >>\nendobj\n";
                objects.add(pageObjStr.getBytes(StandardCharsets.ISO_8859_1));

                byte[] stream = pages.get(i).render().getBytes(StandardCharsets.ISO_8859_1);
                String head = contentObj + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n";
                String tail = "\nendstream\nendobj\n";

                ByteArrayOutputStream contentBuf = new ByteArrayOutputStream();
                contentBuf.write(head.getBytes(StandardCharsets.ISO_8859_1));
                contentBuf.write(stream);
                contentBuf.write(tail.getBytes(StandardCharsets.ISO_8859_1));
                objects.add(contentBuf.toByteArray());
            }

            // Header
            byte[] header = "%PDF-1.4\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1);
            out.write(header);

            // Escriure objectes i recordar offsets
            int[] offsets = new int[objects.size() + 1];
            for (int i = 0; i < objects.size(); i++) {
                offsets[i + 1] = out.size();
                out.write(objects.get(i));
            }

            // xref
            int xrefStart = out.size();
            StringBuilder xref = new StringBuilder();
            xref.append("xref\n0 ").append(objects.size() + 1).append('\n');
            xref.append("0000000000 65535 f \n");
            for (int i = 1; i <= objects.size(); i++) {
                xref.append(String.format("%010d 00000 n \n", offsets[i]));
            }
            xref.append("trailer\n<< /Size ").append(objects.size() + 1)
                .append(" /Root 1 0 R >>\nstartxref\n")
                .append(xrefStart).append("\n%%EOF\n");
            out.write(xref.toString().getBytes(StandardCharsets.ISO_8859_1));

            return out.toByteArray();
        }
    }

    private static final class Page {
        final StringBuilder ops = new StringBuilder();

        void text(int x, int y, String font, int size, String text) {
            String f = "Helvetica-Bold".equals(font) ? "F2" : "F1";
            ops.append("BT\n/").append(f).append(' ').append(size).append(" Tf\n")
               .append(x).append(' ').append(y).append(" Td\n")
               .append('(').append(escape(text)).append(") Tj\n")
               .append("ET\n");
        }

        void line(int x1, int y1, int x2, int y2) {
            ops.append("0.7 0.7 0.7 RG\n0.5 w\n")
               .append(x1).append(' ').append(y1).append(" m ")
               .append(x2).append(' ').append(y2).append(" l S\n");
        }

        String render() { return ops.toString(); }

        static String escape(String s) {
            StringBuilder sb = new StringBuilder(s.length() + 4);
            for (char c : s.toCharArray()) {
                if (c == '(' || c == ')' || c == '\\') sb.append('\\').append(c);
                else sb.append(c);
            }
            return sb.toString();
        }
    }
}
