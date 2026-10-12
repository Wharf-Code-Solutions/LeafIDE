package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import leaf.api.ContextoCompilacion;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.LenguajeDef;
import leaf.api.ProveedorCompilador;
import leaf.api.ResultadoFase;
import leaf.ide.core.AlmacenAjustes;
import leaf.ide.core.Ajustes;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Lo que se ejecuta en la terminal Sistema con {@code java generado.Leaf archivo} se refleja en el IDE. */
class TerminalSincronizadaTest {

    private static ProveedorCompilador proveedor() {
        FaseCompilacion fase = new FaseCompilacion() {
            @Override public String id() { return "lexico"; }
            @Override public String nombre() { return "Léxico"; }
            @Override public List<String> dependeDe() { return List.of(); }
            @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) {
                return new ResultadoFase("lexico", Estado.OK, List.of(), List.of(), 0);
            }
        };
        return new ProveedorCompilador() {
            @Override public String nombre() { return "Falso"; }
            @Override public String version() { return "abc1234"; }
            @Override public int apiVersion() { return API_VERSION; }
            @Override public List<FaseCompilacion> fases() { return List.of(fase); }
            @Override public LenguajeDef lenguaje() { return new LenguajeDef("Falso", List.of("f")); }
        };
    }

    private static <T> T enEdt(Supplier<T> s) throws Exception {
        List<T> r = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> r.add(s.get()));
        return r.get(0);
    }

    private static void esperarHasta(Supplier<Boolean> condicion, String que) throws Exception {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite && !enEdt(condicion)) Thread.sleep(25);
        assertTrue(enEdt(condicion), "No ocurrió a tiempo: " + que);
    }

    private static VentanaPrincipal abrir(Path datos) throws Exception {
        return enEdt(() -> new VentanaPrincipal(proveedor(), 3_600_000, Map.of(), datos));
    }

    @Test
    void ejecutarUnArchivoEnLaTerminalLoAbreYMuestraSusFases(@TempDir Path datos, @TempDir Path proyecto) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Files.writeString(proyecto.resolve("suma.txt"), "definir $a como entero con valor de 1.");
        new AlmacenAjustes(datos).guardar(Ajustes.porDefecto().conCarpetaTrabajo(proyecto));

        VentanaPrincipal v = abrir(datos);
        try {
            assertEquals("Léxico", enEdt(() -> v.fases.titulo(0)), "antes de ejecutar nada la fase está sin resultado");
            // Lo que escribiría el usuario: la ruta es relativa a la carpeta que muestra el prompt de la terminal.
            SwingUtilities.invokeAndWait(() -> v.terminalSistema.enviarLinea("java generado.Leaf suma.txt"));

            esperarHasta(() -> v.fases.titulo(0).equals("Léxico · OK"), "las fases con el resultado del archivo");
            assertEquals("definir $a como entero con valor de 1.", enEdt(() -> v.editor.texto()));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void conLaSincronizacionApagadaLaTerminalNoTocaElIDE(@TempDir Path datos, @TempDir Path proyecto) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Files.writeString(proyecto.resolve("x.txt"), "otro texto");
        new AlmacenAjustes(datos).guardar(Ajustes.porDefecto().conCarpetaTrabajo(proyecto).conSincronizarTerminal(false));

        VentanaPrincipal v = abrir(datos);
        try {
            assertFalse(enEdt(() -> v.sincronizarTerminal()), "el ajuste guardado se respeta");
            SwingUtilities.invokeAndWait(() -> v.terminalSistema.enviarLinea("java generado.Leaf x.txt"));
            Thread.sleep(500);
            assertNotEquals("otro texto", enEdt(() -> v.editor.texto()));
            assertEquals("Léxico", enEdt(() -> v.fases.titulo(0)));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void unArchivoQueNoExisteNoCambiaNada(@TempDir Path datos, @TempDir Path proyecto) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = abrir(datos);
        try {
            String antes = enEdt(() -> v.editor.texto());
            SwingUtilities.invokeAndWait(() -> v.sincronizarConLaTerminal(proyecto.resolve("no-existe.txt")));
            assertEquals(antes, enEdt(() -> v.editor.texto()));
            assertEquals("Léxico", enEdt(() -> v.fases.titulo(0)));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void lasLineasQueSonEntradaDeUnProgramaEnMarchaNoSeExaminan(@TempDir Path datos, @TempDir Path proyecto) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Files.writeString(proyecto.resolve("entrada.txt"), "texto de entrada");
        new AlmacenAjustes(datos).guardar(Ajustes.porDefecto().conCarpetaTrabajo(proyecto));
        VentanaPrincipal v = abrir(datos);
        try {
            // Un programa que se queda esperando una línea (el comando "more" lee su entrada estándar).
            SwingUtilities.invokeAndWait(() -> v.terminalSistema.enviarLinea("more"));
            Thread.sleep(400);
            SwingUtilities.invokeAndWait(() -> v.terminalSistema.enviarLinea("java generado.Leaf entrada.txt"));
            Thread.sleep(500);
            assertNotEquals("texto de entrada", enEdt(() -> v.editor.texto()));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }
}
