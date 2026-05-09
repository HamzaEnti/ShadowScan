package model;

import utils.JsonUtil;

/**
 * Entrada de la base de dades de vulnerabilitats (CVE).
 *
 * Camp 'severity' segueix l'escala CVSS: NONE / LOW / MEDIUM / HIGH / CRITICAL.
 */
public class CveEntry {

    private final String id;
    private final String description;
    private final double cvssScore;
    private final String severity;
    private final String url;

    public CveEntry(String id, String description, double cvssScore, String severity, String url) {
        this.id = id;
        this.description = description;
        this.cvssScore = cvssScore;
        this.severity = severity;
        this.url = url;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public double getCvssScore() { return cvssScore; }
    public String getSeverity() { return severity; }
    public String getUrl() { return url; }

    public String toJson() {
        return "{"
            + "\"id\":\"" + JsonUtil.escape(id) + "\","
            + "\"cvss\":" + JsonUtil.formatDouble(cvssScore) + ","
            + "\"severity\":\"" + JsonUtil.escape(severity) + "\","
            + "\"description\":\"" + JsonUtil.escape(description) + "\","
            + "\"url\":\"" + JsonUtil.escape(url) + "\""
            + "}";
    }

    @Override
    public String toString() {
        return id + " [" + severity + " " + JsonUtil.formatDouble(cvssScore) + "] " + description;
    }
}
