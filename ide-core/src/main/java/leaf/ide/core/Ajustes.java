package leaf.ide.core;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Preferencias del usuario que sobreviven al cerrar el IDE.
 *
 * @param temaOscuro   tema oscuro encendido
 * @param analisisEnVivo análisis automático mientras se escribe
 * @param resaltado    resaltado de sintaxis encendido
 * @param perspectiva  nombre de la última perspectiva elegida (ver {@code Perspectiva} en la UI)
 * @param carpetaTrabajo carpeta que se abrió por última vez (el «proyecto»); vacía si todavía no se eligió ninguna
 * @param sincronizarTerminal lo que se ejecuta con {@code java generado.Leaf archivo} en la terminal Sistema también
 *                       se analiza en el IDE (editor y pestañas de las fases)
 */
public record Ajustes(boolean temaOscuro, boolean analisisEnVivo, boolean resaltado, String perspectiva,
        String carpetaTrabajo, boolean sincronizarTerminal) {

    public Ajustes {
        if (carpetaTrabajo == null) carpetaTrabajo = "";
    }

    public static final String PERSPECTIVA_INICIAL = "ANALISIS";

    public static Ajustes porDefecto() {
        return new Ajustes(false, true, true, PERSPECTIVA_INICIAL, "", true);
    }

    public Ajustes conTemaOscuro(boolean valor) {
        return new Ajustes(valor, analisisEnVivo, resaltado, perspectiva, carpetaTrabajo, sincronizarTerminal);
    }

    public Ajustes conAnalisisEnVivo(boolean valor) {
        return new Ajustes(temaOscuro, valor, resaltado, perspectiva, carpetaTrabajo, sincronizarTerminal);
    }

    public Ajustes conResaltado(boolean valor) {
        return new Ajustes(temaOscuro, analisisEnVivo, valor, perspectiva, carpetaTrabajo, sincronizarTerminal);
    }

    public Ajustes conPerspectiva(String valor) {
        return new Ajustes(temaOscuro, analisisEnVivo, resaltado, valor, carpetaTrabajo, sincronizarTerminal);
    }

    public Ajustes conCarpetaTrabajo(Path carpeta) {
        return new Ajustes(temaOscuro, analisisEnVivo, resaltado, perspectiva,
                carpeta == null ? "" : carpeta.toAbsolutePath().normalize().toString(), sincronizarTerminal);
    }

    public Ajustes conSincronizarTerminal(boolean valor) {
        return new Ajustes(temaOscuro, analisisEnVivo, resaltado, perspectiva, carpetaTrabajo, valor);
    }

    /** La carpeta de trabajo guardada, solo si todavía existe en el disco. */
    public Optional<Path> carpetaTrabajoExistente() {
        if (carpetaTrabajo.isBlank()) return Optional.empty();
        try {
            Path p = Path.of(carpetaTrabajo);
            return Files.isDirectory(p) ? Optional.of(p) : Optional.empty();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }
}
