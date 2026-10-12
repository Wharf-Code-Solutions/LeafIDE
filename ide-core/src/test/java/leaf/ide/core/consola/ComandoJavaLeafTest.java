package leaf.ide.core.consola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ComandoJavaLeafTest {

    private static final Path CARPETA = Path.of("C:\\Leaf").toAbsolutePath().normalize();

    private static Optional<Path> archivo(String linea) {
        return ComandoJavaLeaf.reconocer(linea, CARPETA);
    }

    @Test
    void reconoceElComandoDelManualYResuelveLaRutaContraLaCarpetaDeLaTerminal() {
        assertEquals(Optional.of(CARPETA.resolve("src/pruebas/SumaBasica.txt").normalize()),
                archivo("java generado.Leaf src/pruebas/SumaBasica.txt"));
        assertEquals(Optional.of(CARPETA.resolve("pruebas\\SumaBasica.txt").normalize()),
                archivo("  java   generado.Leaf   pruebas\\SumaBasica.txt  "));
    }

    @Test
    void unaRutaAbsolutaNoSeCuelgaDeLaCarpeta() {
        assertEquals(Optional.of(Path.of("D:\\otros\\x.leaf").normalize()), archivo("java generado.Leaf D:\\otros\\x.leaf"));
    }

    @Test
    void admiteComillasYNombresConEspacios() {
        assertEquals(Optional.of(CARPETA.resolve("mis pruebas\\a b.txt").normalize()),
                archivo("java generado.Leaf \"mis pruebas\\a b.txt\""));
    }

    @Test
    void admiteOpcionesDeJavaYDelCompiladorYJavaExe() {
        Optional<Path> esperado = Optional.of(CARPETA.resolve("x.txt").normalize());
        assertEquals(esperado, archivo("java -Dfile.encoding=UTF-8 generado.Leaf x.txt"));
        assertEquals(esperado, archivo("java -cp lib\\* generado.Leaf x.txt"));
        assertEquals(esperado, archivo("java -classpath \"C:\\mi lib\\*\" generado.Leaf x.txt"));
        assertEquals(esperado, archivo("JAVA.EXE generado.Leaf x.txt --guardar --tres-dir"));
        assertEquals(esperado, archivo("\"C:\\Program Files\\Java\\jdk-26\\bin\\java.exe\" generado.Leaf x.txt"));
        assertEquals(esperado, archivo("java generado.Leaf --tres-dir x.txt"));
    }

    @Test
    void ignoraLoQueNoEsElCompiladorDeLeaf() {
        assertTrue(archivo("java OtroPrograma x.txt").isEmpty());
        assertTrue(archivo("java -jar app.jar x.txt").isEmpty());
        assertTrue(archivo("echo java generado.Leaf x.txt").isEmpty());
        assertTrue(archivo("javac generado.Leaf x.txt").isEmpty());
        assertTrue(archivo("dir").isEmpty());
        assertTrue(archivo("").isEmpty());
        assertTrue(archivo("   ").isEmpty());
    }

    @Test
    void sinArchivoNoHayNadaQueAnalizar() {
        assertTrue(archivo("java generado.Leaf").isEmpty());
        assertTrue(archivo("java generado.Leaf --guardar").isEmpty());
    }

    @Test
    void unaRutaInvalidaNoLanzaExcepcion() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> archivo("java generado.Leaf a<b>.txt"));
    }
}
