package leaf.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Definición del lenguaje. En M2 era mínima (nombre y extensiones); en M4 se amplió, de forma aditiva, con lo que el
 * IDE necesita para colorear el código sin conocer el compilador: palabras por categoría, símbolos y comentarios.
 * Todo campo ausente se normaliza a vacío.
 *
 * @param nombre                  nombre visible del lenguaje
 * @param extensiones             extensiones de archivo sin punto (por ejemplo {@code "leaf"}, {@code "txt"})
 * @param grupos                  palabras agrupadas por categoría (puede haber varios grupos de la misma categoría);
 *                                una palabra puede tener espacios ({@code "con valor de"})
 * @param operadores              operadores simbólicos ({@code "+"}, {@code "-/"})
 * @param delimitadores           delimitadores ({@code "."}, {@code "("}, {@code ">"})
 * @param comentarioLinea         inicio de comentario de línea ({@code "//"}); vacío si no hay
 * @param comentarioBloqueInicio  inicio de comentario de bloque ({@code "/*"}); vacío si no hay
 * @param comentarioBloqueFin     fin de comentario de bloque ({@code "*&#47;"}); vacío si no hay
 * @param prefijoVariable         prefijo que marca una variable ({@code "$"}); vacío si no hay
 */
public record LenguajeDef(String nombre, List<String> extensiones, List<GrupoPalabras> grupos,
                          List<String> operadores, List<String> delimitadores,
                          String comentarioLinea, String comentarioBloqueInicio, String comentarioBloqueFin,
                          String prefijoVariable) {

    /** Palabras de control, declaración y entrada/salida. */
    public static final String RESERVADA = "reservada";
    /** Nombres de tipos de dato. */
    public static final String TIPO = "tipo";
    /** Literales con nombre ({@code verdadero}, {@code falso}). */
    public static final String LITERAL = "literal";
    /** Operadores escritos con palabras ({@code Y}, {@code igual que}). */
    public static final String OPERADOR = "operador";

    /** Palabras de una categoría; el IDE trata una categoría desconocida como {@link #RESERVADA}. */
    public record GrupoPalabras(String categoria, List<String> palabras) {
        public GrupoPalabras {
            categoria = categoria == null ? "" : categoria;
            palabras = Copias.lista(palabras);
        }
    }

    public LenguajeDef {
        nombre = nombre == null ? "" : nombre;
        extensiones = Copias.lista(extensiones);
        grupos = Copias.lista(grupos);
        operadores = Copias.lista(operadores);
        delimitadores = Copias.lista(delimitadores);
        comentarioLinea = comentarioLinea == null ? "" : comentarioLinea;
        comentarioBloqueInicio = comentarioBloqueInicio == null ? "" : comentarioBloqueInicio;
        comentarioBloqueFin = comentarioBloqueFin == null ? "" : comentarioBloqueFin;
        prefijoVariable = prefijoVariable == null ? "" : prefijoVariable;
    }

    /** Definición mínima (la de M2): sin palabras ni símbolos. */
    public LenguajeDef(String nombre, List<String> extensiones) {
        this(nombre, extensiones, null, null, null, null, null, null, null);
    }

    /** Todas las palabras de los grupos con esa categoría, en orden. */
    public List<String> palabras(String categoria) {
        List<String> salida = new ArrayList<>();
        for (GrupoPalabras g : grupos) {
            if (g.categoria().equals(categoria)) salida.addAll(g.palabras());
        }
        return List.copyOf(salida);
    }

    /** Todas las palabras de todos los grupos. */
    public List<String> todasLasPalabras() {
        List<String> salida = new ArrayList<>();
        for (GrupoPalabras g : grupos) salida.addAll(g.palabras());
        return List.copyOf(salida);
    }
}
