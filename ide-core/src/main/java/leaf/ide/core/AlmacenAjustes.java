package leaf.ide.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lee y escribe {@code ajustes.properties} (UTF-8) en la carpeta de datos del IDE ({@code ~/.leafide}). Un archivo
 * ausente o dañado nunca es un error: se usan los valores por defecto.
 */
public final class AlmacenAjustes {

    private static final String ARCHIVO = "ajustes.properties";

    private final Path carpeta;

    public AlmacenAjustes(Path carpeta) {
        this.carpeta = carpeta;
    }

    /** Carpeta de datos del usuario: {@code ~/.leafide}. */
    public static Path carpetaDelUsuario() {
        return Path.of(System.getProperty("user.home"), ".leafide");
    }

    public Path carpeta() {
        return carpeta;
    }

    public Ajustes leer() {
        Ajustes base = Ajustes.porDefecto();
        Path archivo = carpeta.resolve(ARCHIVO);
        if (!Files.isRegularFile(archivo)) return base;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException | IllegalArgumentException e) {
            return base;
        }
        String perspectiva = p.getProperty("perspectiva", base.perspectiva()).strip();
        return new Ajustes(
                booleano(p, "temaOscuro", base.temaOscuro()),
                booleano(p, "analisisEnVivo", base.analisisEnVivo()),
                booleano(p, "resaltado", base.resaltado()),
                perspectiva.isEmpty() ? base.perspectiva() : perspectiva,
                p.getProperty("carpetaTrabajo", "").strip(),
                booleano(p, "sincronizarTerminal", base.sincronizarTerminal()));
    }

    public void guardar(Ajustes a) throws IOException {
        Files.createDirectories(carpeta);
        Properties p = new Properties();
        p.setProperty("temaOscuro", Boolean.toString(a.temaOscuro()));
        p.setProperty("analisisEnVivo", Boolean.toString(a.analisisEnVivo()));
        p.setProperty("resaltado", Boolean.toString(a.resaltado()));
        p.setProperty("perspectiva", a.perspectiva());
        p.setProperty("carpetaTrabajo", a.carpetaTrabajo());
        p.setProperty("sincronizarTerminal", Boolean.toString(a.sincronizarTerminal()));
        try (Writer w = Files.newBufferedWriter(carpeta.resolve(ARCHIVO), StandardCharsets.UTF_8)) {
            p.store(w, "Ajustes de LeafIDE");
        }
    }

    private static boolean booleano(Properties p, String clave, boolean porDefecto) {
        String v = p.getProperty(clave);
        if (v == null) return porDefecto;
        v = v.strip();
        if (v.equalsIgnoreCase("true")) return true;
        if (v.equalsIgnoreCase("false")) return false;
        return porDefecto;
    }
}
