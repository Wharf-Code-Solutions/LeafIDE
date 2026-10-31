package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
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
import leaf.ide.core.SelectorCompilador;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** El menú Compilar → Compilador cambia el compilador, la barra de estado lo dice y se recuerda. */
class SelectorCompiladorVentanaTest {

    /** Proveedor falso que además permite elegir compilador (como el de ide-app). */
    private static final class ProveedorElegible implements ProveedorCompilador, SelectorCompilador {
        boolean deCarpeta = true;
        Path carpeta = Path.of("C:", "Leaf");

        private final FaseCompilacion fase = new FaseCompilacion() {
            @Override public String id() { return "lexico"; }
            @Override public String nombre() { return "Léxico"; }
            @Override public List<String> dependeDe() { return List.of(); }
            @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) {
                return new ResultadoFase("lexico", Estado.OK, List.of(), List.of(), 0);
            }
        };

        @Override public String nombre() { return "Leaf"; }
        @Override public String version() { return descripcion(); }
        @Override public int apiVersion() { return API_VERSION; }
        @Override public List<FaseCompilacion> fases() { return List.of(fase); }
        @Override public LenguajeDef lenguaje() { return new LenguajeDef("Falso", List.of("f")); }

        @Override public boolean usaCarpeta() { return deCarpeta; }
        @Override public Path carpeta() { return carpeta; }
        @Override public void usarCarpeta(Path c) { carpeta = c; deCarpeta = true; }
        @Override public void usarCopiaDelIde() { deCarpeta = false; }
        @Override public String descripcion() { return deCarpeta ? "dd0e608 · " + carpeta : "a864ec7 · copia del IDE"; }
    }

    private static <T> T enEdt(Supplier<T> s) throws Exception {
        List<T> r = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> r.add(s.get()));
        return r.get(0);
    }

    @Test
    void cambiarDeCompiladorSeVeEnLaBarraYSeRecuerda(@TempDir Path datos, @TempDir Path otraCarpeta) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        ProveedorElegible proveedor = new ProveedorElegible();
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedor, 3_600_000, Map.of(), datos));
        try {
            assertTrue(enEdt(v::textoCompilador).contains("dd0e608"), enEdt(v::textoCompilador));

            SwingUtilities.invokeAndWait(() -> v.cambiarCompilador(proveedor, false, null));
            assertTrue(enEdt(v::textoCompilador).contains("copia del IDE"), enEdt(v::textoCompilador));
            assertEquals(false, new AlmacenAjustes(datos).leer().compiladorDeCarpeta(), "se guarda enseguida");

            SwingUtilities.invokeAndWait(() -> v.cambiarCompilador(proveedor, true, otraCarpeta));
            assertTrue(enEdt(v::textoCompilador).contains(otraCarpeta.toString()), enEdt(v::textoCompilador));
            assertEquals(otraCarpeta.toAbsolutePath().normalize(), new AlmacenAjustes(datos).leer().rutaCompilador());
            assertEquals(true, new AlmacenAjustes(datos).leer().compiladorDeCarpeta());
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }
}
