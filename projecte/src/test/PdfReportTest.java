package test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import model.ResultatHost;
import report.PdfReport;

public class PdfReportTest {

    public static void run() {
        TestRunner.section("PdfReport");

        TestRunner.test("Genera un PDF vàlid amb header i %%EOF", () -> {
            try {
                ResultatHost h1 = new ResultatHost("10.0.0.1");
                h1.setEsViu(true);
                h1.setHostname("router.local");
                h1.setPortsOberts(Arrays.asList(80, 443));

                ResultatHost h2 = new ResultatHost("10.0.0.2");
                h2.setEsViu(true);
                h2.setPortsOberts(Arrays.asList(22, 445, 3389));

                File tmp = File.createTempFile("shadowscan-", ".pdf");
                try {
                    PdfReport.write(tmp, List.of(h1, h2));
                    byte[] bytes = Files.readAllBytes(tmp.toPath());
                    String head = new String(bytes, 0, Math.min(8, bytes.length));
                    TestRunner.assertTrue(head.startsWith("%PDF-1."), "header PDF");
                    String tail = new String(bytes, Math.max(0, bytes.length - 8), Math.min(8, bytes.length));
                    TestRunner.assertContains(tail, "%%EOF", "trailer");
                    TestRunner.assertTrue(bytes.length > 500, "mida raonable");
                } finally {
                    tmp.delete();
                }
            } catch (Exception e) {
                throw new AssertionError(e.getMessage());
            }
        });

        TestRunner.test("PDF amb 0 hosts no peta", () -> {
            try {
                File tmp = File.createTempFile("shadowscan-empty-", ".pdf");
                try {
                    PdfReport.write(tmp, List.of());
                    TestRunner.assertTrue(tmp.length() > 200, "almenys el header + estructura");
                } finally {
                    tmp.delete();
                }
            } catch (Exception e) {
                throw new AssertionError(e.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
