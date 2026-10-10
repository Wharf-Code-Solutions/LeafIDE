package leaf.adapter.v1;

import generado.LeafConstants;
import generado.Token;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;
import leaf.api.Rango;

/**
 * Convierte la lista de tokens del parser en las tablas de la fase léxica.
 * Se recorre desde {@code tokenInicial.next}; el token EOF se excluye (no es parte del código del usuario).
 * Los tokens especiales (comentarios/espacios, que JavaCC no encadena en {@code next}) no aparecen.
 */
public final class ConversorTokens {

    public static final List<String> COLUMNAS = List.of("#", "Línea", "Col", "Tipo de token", "Lexema", "Categoría");

    private ConversorTokens() {}

    public static ArtefactoTabla tablaTokens(Token tokenInicial) {
        List<Fila> filas = new ArrayList<>();
        int n = 0;
        for (Token t = tokenInicial == null ? null : tokenInicial.next; t != null; t = t.next) {
            if (t.kind == LeafConstants.EOF) break;
            n++;
            String tipo = TiposToken.nombre(t.kind);
            String categoria = CategoriasToken.categoria(tipo, TiposToken.imagen(t.kind));
            Rango origen = t.beginLine > 0 ? new Rango(t.beginLine, t.beginColumn, t.endLine, t.endColumn) : Rango.DESCONOCIDO;
            filas.add(new Fila(String.valueOf(n),
                    List.of(String.valueOf(n), String.valueOf(t.beginLine), String.valueOf(t.beginColumn),
                            tipo, t.image == null ? "" : t.image, categoria),
                    origen, List.of(), null));
        }
        return new ArtefactoTabla("Tokens", COLUMNAS, filas);
    }

    /** Segunda tabla: cantidad de tokens por categoría (en orden de primera aparición). */
    public static ArtefactoTabla tablaConteo(ArtefactoTabla tokens) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (Fila f : tokens.filas()) conteo.merge(f.celdas().get(5), 1, Integer::sum);
        List<Fila> filas = new ArrayList<>();
        for (Map.Entry<String, Integer> e : conteo.entrySet()) {
            filas.add(new Fila(e.getKey(), List.of(e.getKey(), String.valueOf(e.getValue())), null, null, null));
        }
        filas.add(new Fila("total", List.of("Total", String.valueOf(tokens.filas().size())), null, null, null));
        return new ArtefactoTabla("Conteo por categoría", List.of("Categoría", "Cantidad"), filas);
    }
}
