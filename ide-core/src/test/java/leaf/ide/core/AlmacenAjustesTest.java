package leaf.ide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlmacenAjustesTest {

    @Test
    void sinArchivoUsaLosValoresPorDefecto(@TempDir Path dir) {
        assertEquals(Ajustes.porDefecto(), new AlmacenAjustes(dir.resolve("nada")).leer());
    }

    @Test
    void loGuardadoSeLeeIgual(@TempDir Path dir) throws IOException {
        AlmacenAjustes almacen = new AlmacenAjustes(dir.resolve("datos")); // la carpeta se crea sola
        Ajustes a = new Ajustes(true, false, false, "EVALUACION", "C:\\Leaf", false);
        almacen.guardar(a);
        assertEquals(a, almacen.leer());
    }

    @Test
    void laSincronizacionDeLaTerminalEstaEncendidaPorDefectoYSeRecuerdaApagada(@TempDir Path dir) throws IOException {
        AlmacenAjustes almacen = new AlmacenAjustes(dir);
        assertEquals(true, almacen.leer().sincronizarTerminal());
        almacen.guardar(Ajustes.porDefecto().conSincronizarTerminal(false));
        assertEquals(false, almacen.leer().sincronizarTerminal());
    }

    @Test
    void unArchivoDanadoOConValoresRarosNoRompeNada(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("ajustes.properties"),
                "temaOscuro=quizá\nanalisisEnVivo=false\nperspectiva=   \n\\uZZ=1\n", StandardCharsets.UTF_8);
        Ajustes leidos = new AlmacenAjustes(dir).leer();
        assertEquals(Ajustes.porDefecto(), leidos); // la línea mala invalida el archivo entero
    }

    @Test
    void valoresInvalidosCaenAlDefectoClaveAClave(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("ajustes.properties"),
                "temaOscuro=quizá\nanalisisEnVivo=false\nperspectiva=   \n", StandardCharsets.UTF_8);
        assertEquals(new Ajustes(false, false, true, Ajustes.PERSPECTIVA_INICIAL, "", true), new AlmacenAjustes(dir).leer());
    }
}
