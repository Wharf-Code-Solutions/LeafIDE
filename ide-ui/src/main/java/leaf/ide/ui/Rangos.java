package leaf.ide.ui;

import leaf.api.Rango;

/**
 * Operaciones con {@link Rango}: llevarlos al texto del editor y saber si el cursor cae sobre ellos. Sin Swing.
 *
 * <p>Un rango con {@code colIni == 0} significa "toda la línea" (los símbolos solo conocen su línea); en otro caso es
 * el tramo exacto, con la columna final incluida (así la entrega el compilador).
 */
final class Rangos {

    private Rangos() {}

    /**
     * Tramo del documento que cubre el rango.
     *
     * @return {@code {inicio, finExclusivo}}, o {@code null} si el rango es desconocido
     */
    static int[] tramo(String documento, Rango r) {
        if (r.esDesconocido()) return null;
        if (r.colIni() <= 0) {
            int[] primera = Posiciones.limitesDeLinea(documento, r.lineaIni());
            int[] ultima = Posiciones.limitesDeLinea(documento, Math.max(r.lineaFin(), r.lineaIni()));
            return new int[] {primera[0], ultima[1]};
        }
        int[] li = Posiciones.limitesDeLinea(documento, r.lineaIni());
        int ini = li[0] + Posiciones.offsetEnLinea(documento.substring(li[0], li[1]), r.colIni());
        int[] lf = Posiciones.limitesDeLinea(documento, Math.max(r.lineaFin(), r.lineaIni()));
        int largoFin = lf[1] - lf[0];
        int fin = lf[0] + Math.min(Posiciones.offsetEnLinea(documento.substring(lf[0], lf[1]), r.colFin()) + 1, largoFin);
        if (fin <= ini) fin = Math.min(ini + 1, documento.length()); // rango invertido o vacío: un carácter
        return new int[] {ini, fin};
    }

    /** ¿El carácter en (línea, columna) está dentro del rango? Solo para rangos con posición exacta. */
    static boolean contiene(Rango r, int linea, int columna) {
        if (r.esDesconocido() || r.colIni() <= 0) return false;
        return comparar(linea, columna, r.lineaIni(), r.colIni()) >= 0
                && comparar(linea, columna, r.lineaFin(), r.colFin()) <= 0;
    }

    /** ¿El cursor está justo después del último carácter del rango? (se usa si no está dentro de ninguno) */
    static boolean tocaAlFinal(Rango r, int linea, int columna) {
        if (r.esDesconocido() || r.colIni() <= 0) return false;
        return linea == r.lineaFin() && columna == r.colFin() + 1;
    }

    private static int comparar(int linea1, int col1, int linea2, int col2) {
        return linea1 != linea2 ? Integer.compare(linea1, linea2) : Integer.compare(col1, col2);
    }
}
