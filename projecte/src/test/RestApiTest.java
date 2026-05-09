package test;

import api.RestApi;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import utils.JsonParser;

public class RestApiTest {

    public static void run() {
        TestRunner.section("REST API");

        TestRunner.test("GET /api/health respon 200 amb status ok", () -> {
            RestApi api = new RestApi(0, null);
            int port = freePort();
            api = new RestApi(port, null);
            try {
                try { api.start(); } catch (Exception ex) { throw new AssertionError(ex.getMessage()); }
                String body = httpGet("http://localhost:" + port + "/api/health", null);
                Object json = JsonParser.parse(body);
                TestRunner.assertEquals("ok", JsonParser.getString(json, "status"), "status");
            } finally {
                api.stop();
            }
        });

        TestRunner.test("Token vàlid: 200; token incorrecte: 401", () -> {
            int port = freePort();
            RestApi api = new RestApi(port, "secret-123");
            try {
                try { api.start(); } catch (Exception ex) { throw new AssertionError(ex.getMessage()); }
                int code1 = httpGetCode("http://localhost:" + port + "/api/health", "Bearer secret-123");
                int code2 = httpGetCode("http://localhost:" + port + "/api/health", "Bearer wrong");
                TestRunner.assertEquals(200, code1, "amb token");
                TestRunner.assertEquals(401, code2, "sense token");
            } finally {
                api.stop();
            }
        });

        TestRunner.test("GET /api/cve?service=smb&offline=true", () -> {
            int port = freePort();
            RestApi api = new RestApi(port, null);
            try {
                try { api.start(); } catch (Exception ex) { throw new AssertionError(ex.getMessage()); }
                String body = httpGet("http://localhost:" + port
                    + "/api/cve?service=smb&offline=true", null);
                TestRunner.assertContains(body, "CVE-", "almenys un CVE");
                TestRunner.assertContains(body, "CRITICAL", "severitat present");
            } finally {
                api.stop();
            }
        });
    }

    private static int freePort() {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        } catch (Exception e) {
            return 8765;
        }
    }

    private static String httpGet(String url, String authHeader) {
        try {
            HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
            if (authHeader != null) c.setRequestProperty("Authorization", authHeader);
            c.setConnectTimeout(2000);
            c.setReadTimeout(2000);
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            throw new AssertionError("HTTP GET fail: " + e.getMessage());
        }
    }

    private static int httpGetCode(String url, String authHeader) {
        try {
            HttpURLConnection c = (HttpURLConnection) URI.create(url).toURL().openConnection();
            if (authHeader != null) c.setRequestProperty("Authorization", authHeader);
            c.setConnectTimeout(2000);
            c.setReadTimeout(2000);
            int code = c.getResponseCode();
            c.disconnect();
            return code;
        } catch (Exception e) {
            return -1;
        }
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
