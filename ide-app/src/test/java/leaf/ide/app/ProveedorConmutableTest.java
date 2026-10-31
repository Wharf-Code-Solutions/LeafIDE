package leaf.ide.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import leaf.api.ResultadoFase;
import leaf.api.Estado;
import leaf.api.ProveedorCompilador;
import leaf.ide.core.EventBus;
import leaf.ide.core.Pipeline;
import leaf.ide.core.RegistroProveedores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * El IDE usa el compilador de una carpeta (como {@code C:\Leaf\src}) y nota cuando le borran archivos. La «carpeta del
 * compilador» de estas pruebas se arma con los .class del compilador que trae el IDE, sin sus dos clases propias.
 */
class ProveedorConmutableTest {

    private static final String VALIDO = "definir $a como entero con valor de 2.\nmostrar($a).";

    @TempDir Path leaf;           // hace de C:\Leaf
    private Path generado;        // C:\Leaf\src\generado
    private ProveedorConmutable proveedor;

    @BeforeEach
    void armarCarpetaDelCompilador() throws Exception {
        generado = Files.createDirectories(leaf.resolve("src").resolve("generado"));
        copiarClasesDelCompilador(generado);
        ProveedorCompilador delIde = RegistroProveedores.cargar().orElseThrow();
        proveedor = new ProveedorConmutable(delIde, true, leaf);
    }

    /** Copia generado/*.class del compilador del IDE, salvo ServicioCompilacion y ResultadoCompilacion (no están en Leaf). */
    private static void copiarClasesDelCompilador(Path destino) throws IOException, URISyntaxException, ClassNotFoundException {
        Path origen = Path.of(Class.forName("generado.Leaf").getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(origen)) {
            copiar(origen.resolve("generado"), destino);
        } else {
            try (FileSystem jar = FileSystems.newFileSystem(origen, Map.of())) {
                copiar(jar.getPath("generado"), destino);
            }
        }
    }

    private static void copiar(Path desde, Path destino) throws IOException {
        try (Stream<Path> s = Files.list(desde)) {
            for (Path p : s.toList()) {
                String n = p.getFileName().toString();
                if (!n.endsWith(".class") || n.startsWith("ServicioCompilacion") || n.startsWith("ResultadoCompilacion")) continue;
                try (InputStream in = Files.newInputStream(p)) {
                    Files.copy(in, destino.resolve(n));
                }
            }
        }
    }

    private List<ResultadoFase> compilar(String codigo) {
        return new Pipeline(proveedor, new EventBus()).ejecutar(1, codigo, null);
    }

    private static ResultadoFase fase(List<ResultadoFase> r, String id) {
        return r.stream().filter(f -> f.faseId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void compilaConLasClasesDeLaCarpetaYNoConLasDelIde() {
        List<ResultadoFase> r = compilar(VALIDO);

        assertEquals(Estado.OK, fase(r, "lexico").estado());
        assertEquals(Estado.OK, fase(r, "semantico").estado());
        assertEquals(Estado.OK, fase(r, "intermedio").estado());
        ClassLoader cargador = proveedor.cargadorEnUso();
        assertTrue(cargador instanceof CargadorCompiladorExterno, String.valueOf(cargador));
        assertEquals(leaf.resolve("src").toAbsolutePath().normalize(), ((CargadorCompiladorExterno) cargador).clases());
        assertTrue(proveedor.descripcion().endsWith(leaf.resolve("src").toAbsolutePath().normalize().toString()),
                proveedor.descripcion());
    }

    @Test
    void siBorranUnArchivoDelCompiladorElIdeLoDiceYNoUsaSuCopia() throws IOException {
        assertEquals(Estado.OK, fase(compilar(VALIDO), "lexico").estado());

        Files.delete(generado.resolve("Leaf.class")); // lo que haría la maestra

        List<ResultadoFase> r = compilar(VALIDO);
        ResultadoFase lexico = fase(r, "lexico");
        assertEquals(Estado.FALLO_INTERNO, lexico.estado());
        String mensaje = lexico.diagnosticos().get(0).mensaje();
        assertTrue(mensaje.contains("Leaf.class"), mensaje);
        assertEquals(Estado.OMITIDA, fase(r, "sintactico").estado(), "las fases siguientes se omiten");
        assertNull(proveedor.cargadorEnUso());
    }

    @Test
    void siBorranTodaLaCarpetaGeneradoTambienSeAvisa() throws IOException {
        try (Stream<Path> s = Files.list(generado)) {
            for (Path p : s.toList()) Files.delete(p);
        }
        Files.delete(generado);

        ResultadoFase lexico = fase(compilar(VALIDO), "lexico");
        assertEquals(Estado.FALLO_INTERNO, lexico.estado());
        assertTrue(lexico.diagnosticos().get(0).mensaje().contains("generado"), lexico.diagnosticos().toString());
    }

    @Test
    void alVolverAPonerElArchivoVuelveAFuncionarSinReiniciar() throws Exception {
        Path leafClass = generado.resolve("Leaf.class");
        byte[] respaldo = Files.readAllBytes(leafClass);
        Files.delete(leafClass);
        assertEquals(Estado.FALLO_INTERNO, fase(compilar(VALIDO), "lexico").estado());

        Files.write(leafClass, respaldo);
        assertEquals(Estado.OK, fase(compilar(VALIDO), "intermedio").estado());
    }

    @Test
    void conElCompiladorDeLaCarpetaSeVeSuOptimizacion() {
        List<ResultadoFase> r = compilar("definir $a como entero con valor de 2 + 3.\nmostrar($a).");
        assertEquals(Estado.OK, fase(r, "optimizacion").estado(), fase(r, "optimizacion").diagnosticos().toString());
        assertEquals(Estado.NO_IMPLEMENTADA, fase(r, "objeto").estado());
    }

    @Test
    void unCompiladorSinOptimizadorSoloFallaEnLaOptimizacion() throws IOException {
        Files.delete(generado.resolve("Optimizador.class"));

        List<ResultadoFase> r = compilar(VALIDO);
        assertEquals(Estado.OK, fase(r, "intermedio").estado(), "el código intermedio se sigue viendo");
        ResultadoFase opt = fase(r, "optimizacion");
        assertEquals(Estado.FALLO_INTERNO, opt.estado());
        assertTrue(opt.diagnosticos().get(0).mensaje().contains("Optimizador"), opt.diagnosticos().toString());
    }

    @Test
    void conLaCopiaDelIdeFuncionaAunqueLaCarpetaEsteRota() throws IOException {
        Files.delete(generado.resolve("Leaf.class"));
        proveedor.usarCopiaDelIde();

        assertFalse(proveedor.usaCarpeta());
        assertEquals(Estado.OK, fase(compilar(VALIDO), "intermedio").estado());
        assertNull(proveedor.cargadorEnUso());
        assertTrue(proveedor.descripcion().endsWith("copia del IDE"), proveedor.descripcion());
    }

    @Test
    void unaCarpetaQueNoExisteSeExplica() {
        proveedor.usarCarpeta(leaf.resolve("no-existe"));
        ResultadoFase lexico = fase(compilar(VALIDO), "lexico");
        assertEquals(Estado.FALLO_INTERNO, lexico.estado());
        assertTrue(lexico.diagnosticos().get(0).mensaje().contains("No se encuentra el compilador"),
                lexico.diagnosticos().toString());
    }

    @Test
    void sePuedeElegirDirectamenteLaCarpetaSrc() {
        proveedor.usarCarpeta(leaf.resolve("src"));
        assertEquals(Estado.OK, fase(compilar(VALIDO), "intermedio").estado());
        assertNotNull(proveedor.cargadorEnUso());
    }

    @Test
    void elCommitSeLeeDelRepoGitDeLaCarpeta() throws IOException {
        Path git = Files.createDirectories(leaf.resolve(".git").resolve("refs").resolve("heads"));
        Files.writeString(leaf.resolve(".git").resolve("HEAD"), "ref: refs/heads/main\n");
        Files.writeString(git.resolve("main"), "dd0e608216e3d7d3524607e1090d908f9e67ed5e\n");
        assertTrue(proveedor.descripcion().startsWith("dd0e608 · "), proveedor.descripcion());
        assertTrue(proveedor.version().startsWith("dd0e608"), proveedor.version());
    }
}
