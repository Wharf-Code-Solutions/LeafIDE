package leaf.ide.core.consola;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Decodifica la salida de un proceso. En Windows no hay una sola codificación: los programas de Java entregan UTF-8
 * (se les fuerza con una opción de entorno) y los comandos internos de {@code cmd} la página OEM (850, 437…).
 * Cada línea se decodifica como UTF-8 si es válida y, si no, con la página OEM; un carácter UTF-8 partido entre dos
 * lecturas se retiene hasta completarlo. Quita los avisos "Picked up JAVA_TOOL_OPTIONS" de la JVM.
 */
public final class DecodificadorSalida {

    private static final Pattern AVISO_DE_LA_JVM =
            Pattern.compile("^(NOTE: )?Picked up (JAVA_TOOL_OPTIONS|_JAVA_OPTIONS|JDK_JAVA_OPTIONS):.*\\R?$");

    private final Charset oem;
    private final ByteArrayOutputStream retenidos = new ByteArrayOutputStream();

    public DecodificadorSalida(Charset oem) {
        this.oem = oem;
    }

    /** Decodifica lo recién leído. Lo que no se pueda decidir todavía (un carácter incompleto) queda para la siguiente. */
    public String alimentar(byte[] datos, int largo) {
        ByteArrayOutputStream todos = new ByteArrayOutputStream(retenidos.size() + largo);
        todos.writeBytes(retenidos.toByteArray());
        todos.write(datos, 0, largo);
        retenidos.reset();
        byte[] b = todos.toByteArray();

        int cola = bytesIncompletosAlFinal(b);
        retenidos.write(b, b.length - cola, cola);
        return decodificarPorLineas(b, b.length - cola);
    }

    /** El proceso terminó: entrega lo que quedaba retenido, decodificado como se pueda. */
    public String terminar() {
        byte[] b = retenidos.toByteArray();
        retenidos.reset();
        return decodificarPorLineas(b, b.length);
    }

    private String decodificarPorLineas(byte[] b, int largo) {
        StringBuilder salida = new StringBuilder();
        int inicio = 0;
        for (int i = 0; i < largo; i++) {
            if (b[i] == '\n') {
                agregar(salida, b, inicio, i + 1);
                inicio = i + 1;
            }
        }
        if (inicio < largo) agregar(salida, b, inicio, largo); // línea sin terminar: suele ser una pregunta
        return salida.toString();
    }

    private void agregar(StringBuilder salida, byte[] b, int desde, int hasta) {
        String linea = decodificarLinea(b, desde, hasta);
        if (!AVISO_DE_LA_JVM.matcher(linea).matches()) salida.append(linea);
    }

    private String decodificarLinea(byte[] b, int desde, int hasta) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(b, desde, hasta - desde)).toString();
        } catch (CharacterCodingException e) {
            return new String(b, desde, hasta - desde, oem);
        }
    }

    /** Cuántos bytes del final son el comienzo de un carácter UTF-8 al que le faltan bytes (0 a 3). */
    private static int bytesIncompletosAlFinal(byte[] b) {
        for (int k = 1; k <= 3 && k <= b.length; k++) {
            int x = b[b.length - k] & 0xFF;
            if ((x & 0xC0) == 0x80) continue; // byte de continuación: seguir buscando el inicial
            int esperados = x >= 0xF0 ? 4 : x >= 0xE0 ? 3 : x >= 0xC0 ? 2 : 1;
            return esperados > k ? k : 0;
        }
        return 0;
    }
}
