package test;

import java.util.Arrays;
import java.util.Collections;
import model.RiskLevel;

public class RiskLevelTest {

    public static void run() {
        TestRunner.section("RiskLevel");

        TestRunner.test("Llista nul·la → BAIX", () ->
            TestRunner.assertEquals(RiskLevel.BAIX, RiskLevel.classificar(null), "null"));

        TestRunner.test("Llista buida → BAIX", () ->
            TestRunner.assertEquals(RiskLevel.BAIX, RiskLevel.classificar(Collections.emptyList()), "buit"));

        TestRunner.test("HTTP/HTTPS → BAIX", () ->
            TestRunner.assertEquals(RiskLevel.BAIX,
                RiskLevel.classificar(Arrays.asList(80, 443, 8080)), "ports web"));

        TestRunner.test("SSH → MITJA", () ->
            TestRunner.assertEquals(RiskLevel.MITJA,
                RiskLevel.classificar(Arrays.asList(22, 80)), "SSH"));

        TestRunner.test("MySQL → MITJA", () ->
            TestRunner.assertEquals(RiskLevel.MITJA,
                RiskLevel.classificar(Arrays.asList(3306)), "MySQL"));

        TestRunner.test("SMB → CRITIC", () ->
            TestRunner.assertEquals(RiskLevel.CRITIC,
                RiskLevel.classificar(Arrays.asList(80, 445)), "SMB"));

        TestRunner.test("RDP → CRITIC", () ->
            TestRunner.assertEquals(RiskLevel.CRITIC,
                RiskLevel.classificar(Arrays.asList(3389)), "RDP"));

        TestRunner.test("Telnet → CRITIC, té prioritat sobre MITJA", () ->
            TestRunner.assertEquals(RiskLevel.CRITIC,
                RiskLevel.classificar(Arrays.asList(22, 23, 80)), "23 + 22 → CRITIC"));

        TestRunner.test("Etiqueta amb accent", () ->
            TestRunner.assertEquals("MITJÀ", RiskLevel.MITJA.getEtiqueta(), "MITJÀ"));
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
