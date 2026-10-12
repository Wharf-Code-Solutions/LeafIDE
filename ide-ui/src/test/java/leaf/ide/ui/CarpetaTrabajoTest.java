package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
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

/** La carpeta de trabajo: se recuerda, mueve las consolas y alimenta el explorador. Se omite sin pantalla. */
class CarpetaTrabajoTest {

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

    private static VentanaPrincipal abrir(Path datos) throws Exception {
        return enEdt(() -> new VentanaPrincipal(proveedor(), 3_600_000, Map.of(), datos));
    }

    private static void cerrar(VentanaPrincipal v) throws Exception {
        SwingUtilities.invokeAndWait(v::dispose);
    }

    private static Path normal(Path p) {
        return p.toAbsolutePath().normalize();
    }

    @Test
    void laCarpetaGuardadaSeAbreSolaEnElExploradorYEnLasDosConsolas(@TempDir Path datos, @TempDir Path proyecto) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Files.writeString(proyecto.resolve("hola.txt"), "x");
        new AlmacenAjustes(datos).guardar(Ajustes.porDefecto().conCarpetaTrabajo(proyecto));

        VentanaPrincipal v = abrir(datos);
        try {
            assertEquals(normal(proyecto), enEdt(v::carpetaTrabajo));
            assertEquals(normal(proyecto), enEdt(v::directorioDelSistema));
            assertEquals(normal(proyecto), enEdt(v::directorioDeLeaf));
            assertEquals(List.of("hola.txt"), enEdt(v.explorador::nombresDeLaRaiz));
        } finally {
            cerrar(v);
        }
    }

    @Test
    void abrirOtraCarpetaMueveTodoYSeRecuerdaAlReiniciar(@TempDir Path datos, @TempDir Path otra) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Files.writeString(otra.resolve("prueba.leaf"), "x");

        VentanaPrincipal v = abrir(datos);
        try {
            assertNull(enEdt(v::carpetaTrabajo), "sin carpeta guardada no hay carpeta de trabajo");
            SwingUtilities.invokeAndWait(() -> v.abrirCarpeta(otra));
            assertEquals(normal(otra), enEdt(v::directorioDelSistema));
            assertEquals(normal(otra), enEdt(v::directorioDeLeaf));
            assertEquals(List.of("prueba.leaf"), enEdt(v.explorador::nombresDeLaRaiz));
            assertEquals(otra.toAbsolutePath().normalize().toString(), new AlmacenAjustes(datos).leer().carpetaTrabajo(),
                    "se guarda enseguida, sin esperar a cerrar");
        } finally {
            cerrar(v);
        }

        VentanaPrincipal reiniciada = abrir(datos);
        try {
            assertEquals(normal(otra), enEdt(reiniciada::carpetaTrabajo));
            assertEquals(normal(otra), enEdt(reiniciada::directorioDelSistema));
        } finally {
            cerrar(reiniciada);
        }
    }

    @Test
    void unaCarpetaGuardadaQueYaNoExisteSeIgnora(@TempDir Path datos) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        new AlmacenAjustes(datos).guardar(Ajustes.porDefecto().conCarpetaTrabajo(datos.resolve("borrada")));

        VentanaPrincipal v = abrir(datos);
        try {
            assertNull(enEdt(v::carpetaTrabajo));
            assertEquals(normal(Path.of("")), enEdt(v::directorioDelSistema));
        } finally {
            cerrar(v);
        }
    }

    @Test
    void unaRutaQueNoEsCarpetaNoCambiaNada(@TempDir Path datos, @TempDir Path proyecto) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Path archivo = Files.writeString(proyecto.resolve("a.txt"), "x");
        VentanaPrincipal v = abrir(datos);
        try {
            SwingUtilities.invokeAndWait(() -> v.abrirCarpeta(archivo));
            SwingUtilities.invokeAndWait(() -> v.abrirCarpeta(null));
            assertNull(enEdt(v::carpetaTrabajo));
        } finally {
            cerrar(v);
        }
    }

    @Test
    void elDobleClicEnUnArchivoDelExploradorLoAbreEnElEditor(@TempDir Path datos, @TempDir Path proyecto) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Path archivo = Files.writeString(proyecto.resolve("saludo.txt"), "mostrar(\"hola\").");
        VentanaPrincipal v = abrir(datos);
        try {
            SwingUtilities.invokeAndWait(() -> v.abrirCarpeta(proyecto));
            SwingUtilities.invokeAndWait(() -> v.explorador.abrir(archivo));
            assertEquals("mostrar(\"hola\").", enEdt(() -> v.editor.texto()));
        } finally {
            cerrar(v);
        }
    }

    @Test
    void sinCarpetaDeAjustesNoSePideNiSeGuardaNada(@TempDir Path datos) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedor(), 3_600_000));
        try {
            SwingUtilities.invokeAndWait(v::pedirCarpetaSiHaceFalta); // sin almacén no abre ningún diálogo
            assertNull(enEdt(v::carpetaTrabajo));
        } finally {
            cerrar(v);
        }
        try (var archivos = Files.list(datos)) {
            assertTrue(archivos.findAny().isEmpty());
        } catch (IOException e) {
            throw e;
        }
    }
}
