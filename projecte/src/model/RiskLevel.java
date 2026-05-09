package model;

import java.util.Collection;
import java.util.Set;

/**
 * Classificació de risc d'un host segons els ports oberts.
 *
 * Mou la lògica que abans estava a la vista (DiscoveryPanel) cap al model,
 * de manera que tota la UI, els exportadors i els tests usin la mateixa
 * categorització.
 */
public enum RiskLevel {
    BAIX("BAIX"),
    MITJA("MITJÀ"),
    CRITIC("CRÍTIC");

    private static final Set<Integer> CRITIC_PORTS = Set.of(
        21,    // FTP (clear-text)
        23,    // Telnet (clear-text)
        135,   // RPC endpoint mapper
        137, 138, 139,
        445,   // SMB
        3389,  // RDP
        5900   // VNC
    );

    private static final Set<Integer> MITJA_PORTS = Set.of(
        22,    // SSH
        25,    // SMTP
        110,   // POP3
        143,   // IMAP
        1433,  // MSSQL
        3306,  // MySQL
        5432,  // PostgreSQL
        6379,  // Redis
        27017  // MongoDB
    );

    private final String etiqueta;

    RiskLevel(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Classifica un conjunt de ports oberts. Retorna BAIX si la llista
     * és nul·la o buida.
     */
    public static RiskLevel classificar(Collection<Integer> ports) {
        if (ports == null || ports.isEmpty()) return BAIX;
        boolean teCritic = false;
        boolean teMitja = false;
        for (Integer p : ports) {
            if (p == null) continue;
            if (CRITIC_PORTS.contains(p)) {
                teCritic = true;
                break;
            }
            if (MITJA_PORTS.contains(p)) {
                teMitja = true;
            }
        }
        if (teCritic) return CRITIC;
        if (teMitja)  return MITJA;
        return BAIX;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
