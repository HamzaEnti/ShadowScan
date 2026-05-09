package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import model.ScanProfile;

/**
 * Persistència de perfils d'escaneig al disc local.
 *
 * Per defecte fa servir <code>~/.shadowscan/profiles/</code>; cada perfil és
 * un fitxer JSON independent, així pots versionar-los, compartir-los o
 * editar-los manualment sense una BD.
 */
public final class ProfileStore {

    public static final Path DEFAULT_DIR =
        Paths.get(System.getProperty("user.home"), ".shadowscan", "profiles");

    private final Path dir;

    public ProfileStore() { this(DEFAULT_DIR); }

    public ProfileStore(Path dir) {
        this.dir = dir;
        try { Files.createDirectories(dir); }
        catch (IOException e) { System.err.println(">>> [PROFILE] No s'ha pogut crear " + dir); }
    }

    public Path getDir() { return dir; }

    public List<ScanProfile> loadAll() {
        List<ScanProfile> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    String json = Files.readString(p, StandardCharsets.UTF_8);
                    out.add(ScanProfile.fromJson(json));
                } catch (Exception e) {
                    System.err.println(">>> [PROFILE] Error llegint " + p + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println(">>> [PROFILE] Error llistant directori: " + e.getMessage());
        }
        out.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return out;
    }

    public void save(ScanProfile p) throws IOException {
        if (p == null || p.getName() == null || p.getName().isBlank()) {
            throw new IOException("Perfil sense nom");
        }
        Path target = dir.resolve(slug(p.getName()) + ".json");
        Files.writeString(target, p.toJson(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public boolean delete(String name) throws IOException {
        if (name == null) return false;
        Path target = dir.resolve(slug(name) + ".json");
        return Files.deleteIfExists(target);
    }

    private static String slug(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
