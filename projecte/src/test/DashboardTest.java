package test;

import java.util.Arrays;
import java.util.List;
import model.ResultatHost;
import view.DashboardPanel;

public class DashboardTest {

    public static void run() {
        TestRunner.section("DashboardPanel.compute");

        TestRunner.test("Stats amb dataset mixt", () -> {
            ResultatHost a = new ResultatHost("10.0.0.1");
            a.setEsViu(true);
            a.setPortsOberts(Arrays.asList(80, 443));   // BAIX

            ResultatHost b = new ResultatHost("10.0.0.2");
            b.setEsViu(true);
            b.setPortsOberts(Arrays.asList(22, 80));    // MITJA

            ResultatHost c = new ResultatHost("10.0.0.3");
            c.setEsViu(true);
            c.setPortsOberts(Arrays.asList(445));       // CRITIC

            ResultatHost d = new ResultatHost("10.0.0.4");
            d.setEsViu(false);                          // mort

            DashboardPanel.Stats s = DashboardPanel.compute(List.of(a, b, c, d));
            TestRunner.assertEquals(4, s.total, "total 4");
            TestRunner.assertEquals(3, s.actius, "3 actius");
            TestRunner.assertEquals(1, s.critics, "1 crític");
            TestRunner.assertEquals(5, s.totalPorts, "ports totals");
            // BAIX = a (només web) + d (sense ports) = 2
            TestRunner.assertEquals(Integer.valueOf(2), s.perRisc.get("BAIX"),   "BAIX = a + d");
            TestRunner.assertEquals(Integer.valueOf(1), s.perRisc.get("MITJÀ"),  "MITJÀ = b");
            TestRunner.assertEquals(Integer.valueOf(1), s.perRisc.get("CRÍTIC"), "CRÍTIC = c");
        });

        TestRunner.test("Resum executiu amb 0 hosts", () -> {
            DashboardPanel.Stats s = DashboardPanel.compute(List.of());
            String[] lines = DashboardPanel.generarResum(s);
            TestRunner.assertTrue(lines.length > 0, "té contingut");
            TestRunner.assertContains(lines[0], "No hi ha dades", "missatge buit");
        });
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
