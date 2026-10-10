package leaf.api;

/**
 * Posición en el código fuente (1-based). Una posición desconocida se representa con {@link #DESCONOCIDO} (todo en 0).
 */
public record Rango(int lineaIni, int colIni, int lineaFin, int colFin) {

    /** Rango para diagnósticos sin posición ("Línea ?", colapso léxico, errores internos). */
    public static final Rango DESCONOCIDO = new Rango(0, 0, 0, 0);

    /** Rango de un solo punto. */
    public static Rango punto(int linea, int col) {
        return new Rango(linea, col, linea, col);
    }

    public boolean esDesconocido() {
        return lineaIni <= 0;
    }
}
