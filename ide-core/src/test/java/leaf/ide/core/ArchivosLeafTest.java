package leaf.ide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchivosLeafTest {

    @TempDir Path carpeta;

    @Test
    void leerQuitaElBomUtf8() throws IOException {
        Path f = carpeta.resolve("conBom.leaf");
        Files.write(f, new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b'});
        assertEquals("ab", ArchivosLeaf.leer(f));
    }

    @Test
    void leerConservaAcentosYEnies() throws IOException {
        Path f = carpeta.resolve("acentos.txt");
        Files.writeString(f, "mostrar(\"Año único\").", StandardCharsets.UTF_8);
        assertEquals("mostrar(\"Año único\").", ArchivosLeaf.leer(f));
    }

    @Test
    void escribirUsaUtf8SinBom() throws IOException {
        Path f = carpeta.resolve("salida.leaf");
        ArchivosLeaf.escribir(f, "ñandú");
        byte[] bytes = Files.readAllBytes(f);
        assertEquals("ñandú", new String(bytes, StandardCharsets.UTF_8));
        assertTrue(bytes[0] != (byte) 0xEF, "no debe empezar con BOM");
    }

    @Test
    void conExtensionAgregaLeafSoloSiNoHayExtension() {
        assertEquals("a.leaf", ArchivosLeaf.conExtension(Path.of("a")).getFileName().toString());
        assertEquals("a.txt", ArchivosLeaf.conExtension(Path.of("a.txt")).getFileName().toString());
        assertEquals("a.leaf", ArchivosLeaf.conExtension(Path.of("a.leaf")).getFileName().toString());
    }
}
