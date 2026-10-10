package leaf.ide.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import leaf.api.Diagnostico;
import leaf.api.ResultadoFase;

/**
 * Informe de una compilación con el mismo texto que la consola de Windows ({@code java generado.Leaf archivo}):
 * total de errores y la lista ordenada por línea, o "Programa válido". Los diagnósticos sin posición van al final,
 * como en el original.
 */
public final class Informe {

    private static final String SEPARADOR = "=".repeat(44);

    private Informe() {}

    public static String de(List<ResultadoFase> fases) {
        List<Diagnostico> todos = new ArrayList<>();
        for (ResultadoFase f : fases) todos.addAll(f.diagnosticos());
        todos.sort(Comparator.comparingInt(d -> d.rango().esDesconocido() ? Integer.MAX_VALUE : d.rango().lineaIni()));

        StringBuilder sb = new StringBuilder("\n").append(SEPARADOR).append('\n');
        if (todos.isEmpty()) {
            sb.append("Programa válido. No se encontró ningún error.\n");
        } else {
            sb.append("  Se encontraron ").append(todos.size()).append(" error(es) en total:\n\n");
            for (Diagnostico d : todos) sb.append("  ").append(lineaDeError(d)).append('\n');
        }
        return sb.append(SEPARADOR).append('\n').toString();
    }

    private static String lineaDeError(Diagnostico d) {
        String posicion = d.rango().esDesconocido()
                ? ""
                : " Línea " + d.rango().lineaIni() + ", Columna " + d.rango().colIni();
        return "Tipo de Error: " + NombresFase.de(d.faseId()) + posicion + " : " + d.mensaje();
    }
}
