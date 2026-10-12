package leaf.ide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ListadoCarpetaTest {

    private static void crear(Path base, String... nombres) throws IOException {
        for (String n : nombres) {
            Path p = base.resolve(n);
            if (n.endsWith("/")) Files.createDirectories(p);
            else Files.writeString(p, "x");
        }
    }

    private static List<String> nombres(List<Path> rutas) {
        return rutas.stream().map(p -> p.getFileName().toString()).toList();
    }

    @Test
    void primeroLasCarpetasYLuegoSoloLosArchivosLeafOrdenadosSinImportarMayusculas(@TempDir Path dir) throws IOException {
        crear(dir, "pruebas/", "Zeta/", "beta.txt", "Alfa.leaf", "notas.md", "Leaf.java", "Archivo.TXT", "sin_extension");

        assertEquals(List.of("pruebas", "Zeta", "Alfa.leaf", "Archivo.TXT", "beta.txt"),
                nombres(ListadoCarpeta.hijos(dir)));
    }

    @Test
    void ocultaLasCarpetasQueEmpiezanConPuntoYLasDeCompilacion(@TempDir Path dir) throws IOException {
        crear(dir, ".git/", ".idea/", "target/", "out/", "src/");
        assertEquals(List.of("src"), nombres(ListadoCarpeta.hijos(dir)));
    }

    @Test
    void unaCarpetaInexistenteOUnArchivoDanListaVacia(@TempDir Path dir) throws IOException {
        crear(dir, "a.txt");
        assertTrue(ListadoCarpeta.hijos(dir.resolve("nada")).isEmpty());
        assertTrue(ListadoCarpeta.hijos(dir.resolve("a.txt")).isEmpty());
    }

    @Test
    void reconoceLosArchivosLeaf() {
        assertTrue(ListadoCarpeta.esArchivoLeaf(Path.of("x.txt")));
        assertTrue(ListadoCarpeta.esArchivoLeaf(Path.of("x.LEAF")));
        assertFalse(ListadoCarpeta.esArchivoLeaf(Path.of("x.java")));
        assertFalse(ListadoCarpeta.esArchivoLeaf(Path.of("txt")));
    }

    @Test
    void sabeSiUnaCarpetaTieneSubcarpetasParaMostrarLaFlechaDelArbol(@TempDir Path dir) throws IOException {
        crear(dir, "con/hijo/", "vacia/", "solo/");
        crear(dir, "solo/a.txt");
        assertTrue(ListadoCarpeta.tieneContenido(dir.resolve("con")));
        assertTrue(ListadoCarpeta.tieneContenido(dir.resolve("solo")));
        assertFalse(ListadoCarpeta.tieneContenido(dir.resolve("vacia")));
    }
}
