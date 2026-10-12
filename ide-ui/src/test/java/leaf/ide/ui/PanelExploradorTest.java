package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** El árbol de archivos (se puede crear sin pantalla). */
class PanelExploradorTest {

    @Test
    void muestraLasCarpetasPrimeroYSoloLosProgramasLeaf(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("pruebas"));
        Files.createDirectories(dir.resolve("target"));
        Files.writeString(dir.resolve("b.txt"), "x");
        Files.writeString(dir.resolve("a.leaf"), "x");
        Files.writeString(dir.resolve("Leaf.java"), "x");

        PanelExplorador p = new PanelExplorador(a -> { });
        p.fijarRaiz(dir);

        assertEquals(List.of("pruebas", "a.leaf", "b.txt"), p.nombresDeLaRaiz());
    }

    @Test
    void abrirUnArchivoAvisaAlCreadorPeroUnaCarpetaNo(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("sub"));
        Path archivo = Files.writeString(dir.resolve("x.txt"), "x");
        List<Path> abiertos = new ArrayList<>();

        PanelExplorador p = new PanelExplorador(abiertos::add);
        p.fijarRaiz(dir);
        p.abrir(dir.resolve("sub"));
        p.abrir(archivo);

        assertEquals(List.of(archivo), abiertos);
    }

    @Test
    void sinCarpetaNoMuestraNadaYRefrescarReleeLoNuevo(@TempDir Path dir) throws IOException {
        PanelExplorador p = new PanelExplorador(a -> { });
        assertTrue(p.nombresDeLaRaiz().isEmpty());

        p.fijarRaiz(dir);
        assertTrue(p.nombresDeLaRaiz().isEmpty());
        Files.writeString(dir.resolve("nuevo.txt"), "x");
        p.refrescar();
        assertEquals(List.of("nuevo.txt"), p.nombresDeLaRaiz());

        p.fijarRaiz(null);
        assertTrue(p.nombresDeLaRaiz().isEmpty());
    }
}
