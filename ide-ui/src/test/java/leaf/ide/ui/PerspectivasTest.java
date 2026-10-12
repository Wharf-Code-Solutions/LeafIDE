package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.andrewauclair.moderndocking.app.Docking;
import java.awt.GraphicsEnvironment;
import java.nio.charset.StandardCharsets;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Las perspectivas y su persistencia entre sesiones. Se omiten sin pantalla. */
class PerspectivasTest {

    private static final List<String> IDS = List.of("lexico", "sintactico", "semantico", "intermedio", "optimizacion", "objeto");

    private static FaseCompilacion fase(String id) {
        return new FaseCompilacion() {
            @Override public String id() { return id; }
            @Override public String nombre() { return id; }
            @Override public List<String> dependeDe() { return List.of(); }
            @Override public boolean implementada() { return false; }
            @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) {
                return new ResultadoFase(id, Estado.NO_IMPLEMENTADA, List.of(), List.of(), 0);
            }
        };
    }

    static ProveedorCompilador proveedor() {
        List<FaseCompilacion> fases = IDS.stream().map(PerspectivasTest::fase).toList();
        return new ProveedorCompilador() {
            @Override public String nombre() { return "Falso"; }
            @Override public String version() { return "9.9"; }
            @Override public int apiVersion() { return API_VERSION; }
            @Override public List<FaseCompilacion> fases() { return fases; }
            @Override public LenguajeDef lenguaje() { return new LenguajeDef("Falso", List.of("f")); }
        };
    }

    static <T> T enEdt(Supplier<T> s) throws Exception {
        List<T> r = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> r.add(s.get()));
        return r.get(0);
    }

    static VentanaPrincipal abrir(Path datos) throws Exception {
        return enEdt(() -> {
            VentanaPrincipal v = new VentanaPrincipal(proveedor(), 3_600_000, Map.of(), datos);
            v.setVisible(true); // sin mostrarse, los paneles aún no saben en qué ventana están
            return v;
        });
    }

    static void cerrar(VentanaPrincipal v) throws Exception {
        // Como en la vida real, la ventana ya está acomodada cuando se cierra; sin esto el docking aún no tiene
        // listas sus raíces y no se puede guardar la disposición.
        SwingUtilities.invokeAndWait(v::validate);
        SwingUtilities.invokeAndWait(v::dispose);
    }

    static boolean enPrincipal(VentanaPrincipal v, String id) {
        for (int i = 0; i < v.fases.cantidad(); i++) {
            if (v.fases.idDe(i).equals(id)) return v.fases.acoplable(i).enVentanaPrincipal();
        }
        throw new IllegalArgumentException(id);
    }

    static boolean acoplada(VentanaPrincipal v, String id) {
        for (int i = 0; i < v.fases.cantidad(); i++) {
            if (v.fases.idDe(i).equals(id)) return v.fases.acoplable(i).acoplado();
        }
        throw new IllegalArgumentException(id);
    }

    @Test
    void cadaPerspectivaColocaSusPaneles(@TempDir Path datos) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = abrir(datos);
        try {
            SwingUtilities.invokeAndWait(() -> v.aplicarPerspectiva(Perspectiva.ESCRITURA));
            assertTrue(enEdt(() -> v.dockEditor.enVentanaPrincipal()));
            assertTrue(enEdt(() -> v.dockLeaf.enVentanaPrincipal()));
            assertFalse(enEdt(() -> acoplada(v, "lexico")), "Escritura no muestra las fases");

            SwingUtilities.invokeAndWait(() -> v.aplicarPerspectiva(Perspectiva.ANALISIS));
            assertTrue(enEdt(() -> enPrincipal(v, "lexico") && enPrincipal(v, "semantico")));
            assertFalse(enEdt(() -> acoplada(v, "objeto")));
            assertEquals(1, enEdt(() -> Docking.getRootPanels().size()));

            SwingUtilities.invokeAndWait(() -> v.aplicarPerspectiva(Perspectiva.SINTESIS));
            assertTrue(enEdt(() -> enPrincipal(v, "intermedio") && enPrincipal(v, "objeto")));
            assertFalse(enEdt(() -> acoplada(v, "lexico")));
        } finally {
            cerrar(v);
        }
    }

    @Test
    void unaDisposicionDanadaCaeALaPerspectivaGuardada(@TempDir Path datos) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Files.createDirectories(datos.resolve("layouts"));
        Files.writeString(datos.resolve("layouts").resolve("ultima.xml"), "<esto no es un layout", StandardCharsets.UTF_8);
        Files.writeString(datos.resolve("ajustes.properties"), "perspectiva=SINTESIS\n", StandardCharsets.UTF_8);
        VentanaPrincipal v = abrir(datos);
        try {
            assertTrue(enEdt(() -> v.dockEditor.enVentanaPrincipal()));
            assertTrue(enEdt(() -> enPrincipal(v, "objeto")), "parte de la perspectiva guardada (Síntesis)");
        } finally {
            cerrar(v);
        }
    }

    @Test
    void sinCarpetaDeAjustesNoSeEscribeNada(@TempDir Path datos) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedor(), 3_600_000));
        try {
            assertTrue(enEdt(() -> v.dockEditor.enVentanaPrincipal()));
        } finally {
            cerrar(v);
        }
        try (var archivos = Files.list(datos)) {
            assertEquals(0, archivos.count());
        }
    }
}
