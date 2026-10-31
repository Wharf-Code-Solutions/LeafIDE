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
 * @param compiladorDeCarpeta el IDE usa el compilador de {@code carpetaCompilador} (sus {@code .class}); si es
 *                       {@code false}, usa la copia que lleva dentro
 * @param carpetaCompilador carpeta del compilador base (por ejemplo {@code C:\Leaf}, o directamente su {@code src})
 */
public record Ajustes(boolean temaOscuro, boolean analisisEnVivo, boolean resaltado, String perspectiva,
        String carpetaTrabajo, boolean sincronizarTerminal, boolean compiladorDeCarpeta, String carpetaCompilador) {

    public static final String PERSPECTIVA_INICIAL = "ANALISIS";
    /** Dónde está el compilador base si el usuario no eligió otra carpeta. */
    public static final String CARPETA_COMPILADOR_INICIAL = "C:\\Leaf";

    public Ajustes {
        if (carpetaTrabajo == null) carpetaTrabajo = "";
        if (carpetaCompilador == null || carpetaCompilador.isBlank()) carpetaCompilador = CARPETA_COMPILADOR_INICIAL;
    }

    /** Sin datos del compilador: los valores iniciales (usar el de {@code C:\Leaf}). */
    public Ajustes(boolean temaOscuro, boolean analisisEnVivo, boolean resaltado, String perspectiva,
            String carpetaTrabajo, boolean sincronizarTerminal) {
        this(temaOscuro, analisisEnVivo, resaltado, perspectiva, carpetaTrabajo, sincronizarTerminal, true,
                CARPETA_COMPILADOR_INICIAL);
    }

    public static Ajustes porDefecto() {
        return new Ajustes(false, true, true, PERSPECTIVA_INICIAL, "", true, true, CARPETA_COMPILADOR_INICIAL);
    }

    public Ajustes conTemaOscuro(boolean valor) {
        return new Ajustes(valor, analisisEnVivo, resaltado, perspectiva, carpetaTrabajo, sincronizarTerminal,
                compiladorDeCarpeta, carpetaCompilador);
    }

    public Ajustes conAnalisisEnVivo(boolean valor) {
        return new Ajustes(temaOscuro, valor, resaltado, perspectiva, carpetaTrabajo, sincronizarTerminal,
                compiladorDeCarpeta, carpetaCompilador);
    }

    public Ajustes conResaltado(boolean valor) {
        return new Ajustes(temaOscuro, analisisEnVivo, valor, perspectiva, carpetaTrabajo, sincronizarTerminal,
                compiladorDeCarpeta, carpetaCompilador);
    }

    public Ajustes conPerspectiva(String valor) {
        return new Ajustes(temaOscuro, analisisEnVivo, resaltado, valor, carpetaTrabajo, sincronizarTerminal,
                compiladorDeCarpeta, carpetaCompilador);
    }

    public Ajustes conCarpetaTrabajo(Path carpeta) {
        return new Ajustes(temaOscuro, analisisEnVivo, resaltado, perspectiva,
                carpeta == null ? "" : carpeta.toAbsolutePath().normalize().toString(), sincronizarTerminal,
                compiladorDeCarpeta, carpetaCompilador);
    }

    public Ajustes conSincronizarTerminal(boolean valor) {
        return new Ajustes(temaOscuro, analisisEnVivo, resaltado, perspectiva, carpetaTrabajo, valor,
                compiladorDeCarpeta, carpetaCompilador);
    }

    /** @param deCarpeta {@code true}: el compilador de {@code carpeta}; {@code false}: la copia del IDE */
    public Ajustes conCompilador(boolean deCarpeta, Path carpeta) {
        return new Ajustes(temaOscuro, analisisEnVivo, resaltado, perspectiva, carpetaTrabajo, sincronizarTerminal,
                deCarpeta, carpeta == null ? carpetaCompilador : carpeta.toAbsolutePath().normalize().toString());
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

    /** La carpeta del compilador como ruta (aunque no exista: el IDE avisará al compilar). */
    public Path rutaCompilador() {
        try {
            return Path.of(carpetaCompilador);
        } catch (InvalidPathException e) {
            return Path.of(CARPETA_COMPILADOR_INICIAL);
        }
    }
}
