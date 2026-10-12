package leaf.ide.core.consola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pruebas con procesos reales de Windows (cmd, archivos .bat, java). Se omiten en otros sistemas. */
class TerminalSistemaTest {

    @TempDir Path carpeta;

    /** Recoge lo que imprime el proceso y avisa cuando termina. */
    private static final class Recolector implements TerminalSistema.Escucha {
        final StringBuffer texto = new StringBuffer();
        final AtomicInteger codigo = new AtomicInteger(Integer.MIN_VALUE);
        volatile CountDownLatch terminado = new CountDownLatch(1);

        @Override public void salida(String t) { texto.append(t); }
        @Override public void terminado(int c) { codigo.set(c); terminado.countDown(); }

        boolean esperarFin() throws InterruptedException { return terminado.await(30, TimeUnit.SECONDS); }

        boolean esperarTexto(String buscado) throws InterruptedException {
            long limite = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < limite && !texto.toString().contains(buscado)) Thread.sleep(25);
            return texto.toString().contains(buscado);
        }
    }

    private final Recolector recolector = new Recolector();
    private TerminalSistema terminal;

    @BeforeEach
    void soloWindows() {
        Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"), "solo Windows");
        terminal = new TerminalSistema(carpeta, Map.of(), recolector, TerminalSistema.oemDelSistema());
    }

    private void ejecutarYEsperar(String linea) throws InterruptedException {
        recolector.terminado = new CountDownLatch(1);
        terminal.enviar(linea);
        assertTrue(recolector.esperarFin(), "el comando no terminó: " + linea + "\nsalida: " + recolector.texto);
    }

    @Test
    void cambiarDirectorioMueveLaTerminalYLosComandosSiguenAhi() throws Exception {
        Path otra = java.nio.file.Files.createDirectory(carpeta.resolve("otra"));
        assertTrue(terminal.cambiarDirectorio(otra));
        assertEquals(otra.toAbsolutePath().normalize(), terminal.directorio());
        ejecutarYEsperar("echo %CD%");
        assertTrue(recolector.texto.toString().contains(otra.getFileName().toString()), recolector.texto.toString());
        assertFalse(terminal.cambiarDirectorio(carpeta.resolve("no-existe")), "una ruta inexistente no mueve la terminal");
        assertEquals(otra.toAbsolutePath().normalize(), terminal.directorio());
    }

    @Test
    void ejecutaUnComandoYMuestraSuSalida() throws Exception {
        ejecutarYEsperar("echo hola");
        assertTrue(recolector.texto.toString().contains("hola"), recolector.texto.toString());
        assertEquals(0, recolector.codigo.get());
        assertFalse(terminal.enEjecucion());
    }

    @Test
    void lasComillasDelComandoLlegaranIntactas() throws Exception {
        ejecutarYEsperar("echo \"hola mundo\"");
        assertTrue(recolector.texto.toString().contains("\"hola mundo\""), recolector.texto.toString());
    }

    @Test
    void entregaElCodigoDeSalida() throws Exception {
        ejecutarYEsperar("exit /b 3");
        assertEquals(3, recolector.codigo.get());
    }

    @Test
    void losAcentosDeLosComandosInternosDeCmdSeVenBien() throws Exception {
        ejecutarYEsperar("echo Línea ñandú útil");
        assertTrue(recolector.texto.toString().contains("Línea ñandú útil"), recolector.texto.toString());
    }

    @Test
    void ponePasarVariablesDeEntornoAlProceso() throws Exception {
        terminal = new TerminalSistema(carpeta, Map.of("MI_VARIABLE_DE_PRUEBA", "valor-123"), recolector,
                TerminalSistema.oemDelSistema());
        ejecutarYEsperar("echo %MI_VARIABLE_DE_PRUEBA%");
        assertTrue(recolector.texto.toString().contains("valor-123"), recolector.texto.toString());
    }

    @Test
    void cdCambiaLaCarpetaDeLosSiguientesComandos() throws Exception {
        Files.createDirectory(carpeta.resolve("sub"));
        Files.writeString(carpeta.resolve("sub").resolve("dentro.txt"), "x");

        terminal.enviar("cd sub");
        assertEquals(carpeta.resolve("sub"), terminal.directorio());
        ejecutarYEsperar("dir /b");
        assertTrue(recolector.texto.toString().contains("dentro.txt"), recolector.texto.toString());

        terminal.enviar("cd ..");
        assertEquals(carpeta, terminal.directorio());
    }

    @Test
    void cdSinArgumentosMuestraLaCarpetaYUnaCarpetaInexistenteNoLaCambia() {
        terminal.enviar("cd");
        assertTrue(recolector.texto.toString().contains(carpeta.toString()), recolector.texto.toString());

        recolector.texto.setLength(0);
        terminal.enviar("cd no-existe");
        assertTrue(recolector.texto.toString().contains("no puede encontrar la ruta"), recolector.texto.toString());
        assertEquals(carpeta, terminal.directorio());
    }

    @Test
    void cdAdmiteRutasConEspaciosEntreComillasYLaBanderaD() throws Exception {
        Path conEspacios = Files.createDirectory(carpeta.resolve("mi carpeta"));
        terminal.enviar("cd /d \"" + conEspacios + "\"");
        assertEquals(conEspacios, terminal.directorio());
    }

    @Test
    void unaLineaMientrasHayUnProcesoVaASuEntrada() throws Exception {
        Path bat = carpeta.resolve("preguntar.bat");
        Files.writeString(bat, "@echo off\r\nset /p nombre=Nombre: \r\necho Hola %nombre%\r\n", StandardCharsets.US_ASCII);

        recolector.terminado = new CountDownLatch(1);
        terminal.enviar(".\\preguntar.bat");
        assertTrue(recolector.esperarTexto("Nombre: "), "debe mostrar la pregunta sin esperar un salto de línea");
        assertTrue(terminal.enEjecucion());

        terminal.enviar("Ana");
        assertTrue(recolector.esperarFin());
        assertTrue(recolector.texto.toString().contains("Hola Ana"), recolector.texto.toString());
    }

    @Test
    void detenerCortaUnProcesoQueNoTermina() throws Exception {
        Path bat = carpeta.resolve("largo.bat");
        Files.writeString(bat, "@echo off\r\nping -n 60 127.0.0.1 >nul\r\n", StandardCharsets.US_ASCII);

        recolector.terminado = new CountDownLatch(1);
        terminal.enviar(".\\largo.bat");
        Thread.sleep(500);
        assertTrue(terminal.enEjecucion());

        long t0 = System.currentTimeMillis();
        terminal.detener();
        assertTrue(recolector.terminado.await(10, TimeUnit.SECONDS), "debe terminar al detenerlo");
        assertTrue(System.currentTimeMillis() - t0 < 10_000);
        assertFalse(terminal.enEjecucion());
    }

    @Test
    void javaImprimeEnUtf8SinElAvisoDeLaJvm() throws Exception {
        Files.writeString(carpeta.resolve("P.java"),
                "public class P { public static void main(String[] a) { System.out.println(\"Línea ñandú útil\"); } }",
                StandardCharsets.UTF_8);

        ejecutarYEsperar("java P.java");

        String salida = recolector.texto.toString();
        assertTrue(salida.contains("Línea ñandú útil"), salida);
        assertFalse(salida.contains("Picked up"), "el aviso de JAVA_TOOL_OPTIONS no debe verse: " + salida);
    }

    @Test
    void sinRutaDeProgramaElErrorSeMuestraYTerminaConCodigo() throws Exception {
        ejecutarYEsperar("comando-que-no-existe-xyz");
        assertFalse(recolector.texto.toString().isBlank(), "cmd explica que no lo reconoce");
        assertTrue(recolector.codigo.get() != 0);
    }

    @Test
    void laPaginaOemSeDetectaDelSistema() {
        Charset oem = TerminalSistema.oemDelSistema();
        assertTrue(oem != null && oem.canEncode());
    }

    @Test
    void cerrarDetieneElProceso() throws Exception {
        Path bat = carpeta.resolve("largo.bat");
        Files.writeString(bat, "@echo off\r\nping -n 60 127.0.0.1 >nul\r\n", StandardCharsets.US_ASCII);
        terminal.enviar(".\\largo.bat");
        Thread.sleep(500);
        terminal.cerrar();
        assertTrue(recolector.terminado.await(10, TimeUnit.SECONDS));
    }

    @Test
    void unaLineaEnBlancoNoHaceNada() throws IOException {
        terminal.enviar("   ");
        assertFalse(terminal.enEjecucion());
        assertEquals("", recolector.texto.toString());
    }
}
