package leaf.adapter.v1;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import leaf.api.Diagnostico;
import leaf.api.Rango;
import leaf.api.Severidad;

/**
 * Convierte los mensajes de texto del compilador Leaf en {@link Diagnostico}. Nunca lanza excepciones.
 *
 * <p>Formatos conocidos (ver {@code Leaf.jjt} y {@code VisitanteSemantico}):
 * <pre>
 * Tipo de Error: Léxico Línea 3, Columna 5 : mensaje
 * Tipo de Error: Sintáctico Línea ? : mensaje          (token desconocido: fase real, rango 0)
 * Tipo de Error: Léxico : Colapso fatal del motor ...  (sin posición: fase real, rango 0)
 * Tipo de Error: Interno : El análisis semántico ...   (fase "interno")
 * </pre>
 * Todo lo que no encaja (o una fase no reconocida) se devuelve con fase {@link #FASE_INTERNA} y el texto completo.
 */
public final class ParserDiagnosticos {

    public static final String FASE_LEXICA = "lexico";
    public static final String FASE_SINTACTICA = "sintactico";
    public static final String FASE_SEMANTICA = "semantico";
    /** Fase de los errores internos del compilador; la fase semántica los absorbe como FALLO_INTERNO. */
    public static final String FASE_INTERNA = "interno";

    private static final Pattern FORMATO = Pattern.compile(
            "Tipo de Error:\\s*(\\S+)"
            + "(?:\\s+L[ií]nea\\s+(\\d+|\\?)(?:\\s*,\\s*Columna\\s+(\\d+|\\?))?)?"
            + "\\s*:\\s*(.*)",
            Pattern.DOTALL);

    private ParserDiagnosticos() {}

    public static List<Diagnostico> analizarTodos(List<String> mensajes) {
        List<Diagnostico> salida = new ArrayList<>();
        if (mensajes == null) return salida;
        for (String m : mensajes) salida.add(analizar(m));
        return salida;
    }

    public static Diagnostico analizar(String mensaje) {
        if (mensaje == null) {
            return new Diagnostico(FASE_INTERNA, Severidad.ERROR, Rango.DESCONOCIDO, "(mensaje nulo del compilador)");
        }
        try {
            Matcher m = FORMATO.matcher(mensaje);
            if (!m.find()) {
                return new Diagnostico(FASE_INTERNA, Severidad.ERROR, Rango.DESCONOCIDO, mensaje.trim());
            }
            String fase = normalizarFase(m.group(1));
            int linea = entero(m.group(2));
            int col = entero(m.group(3));
            Rango rango = linea > 0 ? Rango.punto(linea, Math.max(col, 0)) : Rango.DESCONOCIDO;
            String texto = m.group(4).trim();
            if (fase == null) {
                // Fase no reconocida: se conserva el mensaje completo para no perder información.
                return new Diagnostico(FASE_INTERNA, Severidad.ERROR, rango, mensaje.trim());
            }
            return new Diagnostico(fase, Severidad.ERROR, rango, texto);
        } catch (RuntimeException e) {
            return new Diagnostico(FASE_INTERNA, Severidad.ERROR, Rango.DESCONOCIDO, mensaje);
        }
    }

    /** "Léxico" → "lexico", "Sintáctico" → "sintactico", "Semántico" → "semantico", "Interno" → "interno"; otro → null. */
    static String normalizarFase(String crudo) {
        String sinAcentos = Normalizer.normalize(crudo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return switch (sinAcentos) {
            case FASE_LEXICA, FASE_SINTACTICA, FASE_SEMANTICA, FASE_INTERNA -> sinAcentos;
            default -> null;
        };
    }

    /** "?" o null → 0; número fuera de rango → 0. */
    private static int entero(String s) {
        if (s == null || s.equals("?")) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
