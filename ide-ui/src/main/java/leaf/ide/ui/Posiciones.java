package leaf.ide.ui;

/**
 * Convierte posiciones (línea, columna) del compilador en posiciones del texto del editor. Sin Swing.
 *
 * <p>Las columnas van desde 1 y cada carácter, tabulador incluido, ocupa una columna: el parser generado trae
 * {@code SimpleCharStream.tabSize = 1}. Una prueba de {@code adapter-v1} compila código con tabuladores y falla si
 * eso cambia (por ejemplo, al regenerar el parser con otro ancho).
 */
final class Posiciones {

    private Posiciones() {}

    /** Índice (desde 0) del carácter de la línea que está en esa columna; el largo si se pasa del final. */
    static int offsetEnLinea(CharSequence linea, int columna) {
        return Math.max(0, Math.min(columna - 1, linea.length()));
    }

    /**
     * Dónde empieza y dónde termina (sin el salto de línea) la línea {@code linea}, contada desde 1. Si no existe se
     * usa la última; en un texto vacío es {@code {0, 0}}.
     */
    static int[] limitesDeLinea(String documento, int linea) {
        int inicio = 0;
        int numero = 1;
        int fin = documento.indexOf('\n');
        while (numero < linea && fin >= 0) {
            inicio = fin + 1;
            numero++;
            fin = documento.indexOf('\n', inicio);
        }
        return new int[] {inicio, fin < 0 ? documento.length() : fin};
    }

    /**
     * Tramo del documento a subrayar para un error en (línea, columna), ambas desde 1: la palabra, la cadena o el
     * símbolo que empieza ahí. Si la posición cae al final de la línea se subraya su último carácter, y en una línea
     * vacía el largo es 0 (quien lo use subrayará la línea entera).
     *
     * @return {@code {inicio, largo}} con el inicio como offset dentro del documento
     */
    static int[] subrayado(String documento, int linea, int columna) {
        int[] limites = limitesDeLinea(documento, linea);
        int inicioLinea = limites[0];
        String texto = documento.substring(inicioLinea, limites[1]);
        if (texto.isEmpty()) return new int[] {inicioLinea, 0};

        int i = offsetEnLinea(texto, columna);
        if (i >= texto.length()) return new int[] {inicioLinea + texto.length() - 1, 1};

        int fe = i + 1;
        char c = texto.charAt(i);
        if (c == '"') {
            while (fe < texto.length() && texto.charAt(fe) != '"') fe += texto.charAt(fe) == '\\' ? 2 : 1;
            fe = Math.min(fe + 1, texto.length());
        } else if (esDePalabra(c)) {
            while (fe < texto.length() && esDePalabra(texto.charAt(fe))) fe++;
        }
        return new int[] {inicioLinea + i, fe - i};
    }

    static boolean esDePalabra(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
