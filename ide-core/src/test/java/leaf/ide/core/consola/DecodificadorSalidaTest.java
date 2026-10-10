package leaf.ide.core.consola;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DecodificadorSalidaTest {

    private static final Charset OEM = Charset.forName("IBM850");

    private final DecodificadorSalida decodificador = new DecodificadorSalida(OEM);

    private String alimentar(byte[] datos) {
        return decodificador.alimentar(datos, datos.length);
    }

    @Test
    void asciiPasaTalCual() {
        assertEquals("hola mundo\n", alimentar("hola mundo\n".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void utf8ValidoSeDecodificaComoUtf8() {
        assertEquals("Línea ñandú\n", alimentar("Línea ñandú\n".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void siNoEsUtf8SeDecodificaConLaPaginaOem() {
        // Lo que escribe "echo" de cmd: bytes de la página 850, que no son UTF-8 válido.
        assertEquals("Línea ñandú útil\n", alimentar("Línea ñandú útil\n".getBytes(OEM)));
    }

    @Test
    void cadaLineaSeDecodificaPorSeparado() {
        byte[] a = "Línea uno\n".getBytes(StandardCharsets.UTF_8);
        byte[] b = "Línea dos\n".getBytes(OEM);
        byte[] ambas = new byte[a.length + b.length];
        System.arraycopy(a, 0, ambas, 0, a.length);
        System.arraycopy(b, 0, ambas, a.length, b.length);
        assertEquals("Línea uno\nLínea dos\n", alimentar(ambas));
    }

    @Test
    void unCaracterUtf8PartidoEntreDosLecturasSeCompleta() {
        byte[] bytes = "añb\n".getBytes(StandardCharsets.UTF_8); // 'ñ' ocupa 2 bytes: posiciones 1 y 2
        String primera = decodificador.alimentar(new byte[] {bytes[0], bytes[1]}, 2);
        String segunda = decodificador.alimentar(new byte[] {bytes[2], bytes[3], bytes[4]}, 3);
        assertEquals("a", primera);
        assertEquals("ñb\n", segunda);
    }

    @Test
    void unaLineaSinSaltoSeEntregaEnseguidaPorqueSueleSerUnaPregunta() {
        assertEquals("Escriba la ruta: ", alimentar("Escriba la ruta: ".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void unaLineaPartidaEnDosLecturasSeEntregaEnPartes() {
        assertEquals("Hola ", decodificador.alimentar("Hola ".getBytes(StandardCharsets.UTF_8), 5));
        assertEquals("mundo\n", decodificador.alimentar("mundo\n".getBytes(StandardCharsets.UTF_8), 6));
    }

    @Test
    void quitaLosAvisosDeLaJvmSobreLasOpcionesDeEntorno() {
        String entrada = "Picked up JAVA_TOOL_OPTIONS: -Dstdout.encoding=UTF-8\n"
                + "NOTE: Picked up JDK_JAVA_OPTIONS: -Dx=y\n"
                + "java version \"26\"\n";
        assertEquals("java version \"26\"\n", alimentar(entrada.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void terminarEntregaLosBytesQueQuedabanRetenidos() {
        byte[] bytes = "ñ".getBytes(StandardCharsets.UTF_8);
        assertEquals("", decodificador.alimentar(new byte[] {bytes[0]}, 1));
        // El proceso terminó con un byte suelto: se decodifica como se pueda, sin perderlo.
        assertEquals(1, decodificador.terminar().length());
    }

    @Test
    void entenderLosSaltosDeLineaDeWindows() {
        assertEquals("uno\r\ndos\r\n", alimentar("uno\r\ndos\r\n".getBytes(StandardCharsets.UTF_8)));
    }
}
