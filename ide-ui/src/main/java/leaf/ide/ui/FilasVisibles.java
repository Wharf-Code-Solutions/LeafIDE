package leaf.ide.ui;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;

/**
 * Qué filas de una tabla se muestran, según el filtro y la agrupación. Sin Swing.
 *
 * <p>El filtro busca el texto en cualquier celda sin distinguir mayúsculas ni acentos. Al agrupar, cada grupo (en
 * orden de primera aparición) lleva una entrada de encabezado con su conteo, y los grupos sin coincidencias no salen.
 */
record FilasVisibles(List<Entrada> entradas, int coincidencias, int total) {

    /** Una fila de datos ({@code encabezado == null}) o el encabezado de un grupo ({@code fila == null}). */
    record Entrada(Fila fila, String encabezado) {}

    private static final String SIN_GRUPO = "(sin grupo)";

    static boolean hayGrupos(ArtefactoTabla tabla) {
        return tabla.filas().stream().anyMatch(f -> f.grupo() != null);
    }

    static FilasVisibles de(ArtefactoTabla tabla, String filtro, boolean agrupar) {
        String buscado = normalizar(filtro == null ? "" : filtro.trim());
        List<Fila> coinciden = new ArrayList<>();
        for (Fila f : tabla.filas()) {
            if (buscado.isEmpty() || f.celdas().stream().anyMatch(c -> c != null && normalizar(c).contains(buscado))) {
                coinciden.add(f);
            }
        }
        List<Entrada> entradas = new ArrayList<>();
        if (agrupar && hayGrupos(tabla)) {
            Map<String, List<Fila>> porGrupo = new LinkedHashMap<>();
            for (Fila f : coinciden) porGrupo.computeIfAbsent(f.grupo() == null ? SIN_GRUPO : f.grupo(), g -> new ArrayList<>()).add(f);
            for (Map.Entry<String, List<Fila>> g : porGrupo.entrySet()) {
                entradas.add(new Entrada(null, g.getKey() + " (" + g.getValue().size() + ")"));
                for (Fila f : g.getValue()) entradas.add(new Entrada(f, null));
            }
        } else {
            for (Fila f : coinciden) entradas.add(new Entrada(f, null));
        }
        return new FilasVisibles(List.copyOf(entradas), coinciden.size(), tabla.filas().size());
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
