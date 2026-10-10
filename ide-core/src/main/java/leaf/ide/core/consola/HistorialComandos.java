package leaf.ide.core.consola;

import java.util.ArrayList;
import java.util.List;

/** Historial de líneas escritas, para recorrerlo con las flechas ↑ y ↓ como en una terminal. */
public final class HistorialComandos {

    private static final int LIMITE_POR_DEFECTO = 200;

    private final int limite;
    private final List<String> lineas = new ArrayList<>();
    private int posicion; // índice de la línea mostrada; lineas.size() = la línea en blanco de abajo

    public HistorialComandos() {
        this(LIMITE_POR_DEFECTO);
    }

    public HistorialComandos(int limite) {
        this.limite = Math.max(1, limite);
    }

    /** Guarda una línea (no las vacías ni la misma dos veces seguidas) y vuelve a la línea en blanco. */
    public void agregar(String linea) {
        String l = linea == null ? "" : linea.strip();
        if (!l.isEmpty() && (lineas.isEmpty() || !lineas.get(lineas.size() - 1).equals(l))) {
            lineas.add(l);
            if (lineas.size() > limite) lineas.remove(0);
        }
        posicion = lineas.size();
    }

    /** La línea anterior (↑); la primera si ya se está en ella; {@code null} si no hay historial. */
    public String anterior() {
        if (lineas.isEmpty()) return null;
        if (posicion > 0) posicion--;
        return lineas.get(posicion);
    }

    /** La línea siguiente (↓); pasada la más nueva, la línea en blanco; {@code null} si no hay historial. */
    public String siguiente() {
        if (lineas.isEmpty()) return null;
        if (posicion < lineas.size()) posicion++;
        return posicion >= lineas.size() ? "" : lineas.get(posicion);
    }

    public int tamano() {
        return lineas.size();
    }
}
