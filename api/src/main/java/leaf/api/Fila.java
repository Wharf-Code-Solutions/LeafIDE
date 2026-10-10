package leaf.api;

import java.util.List;

/**
 * Fila de un {@link ArtefactoTabla}.
 *
 * @param id     identificador estable de la fila
 * @param celdas valores en el orden de las columnas
 * @param origen rango del código asociado (puede ser {@link Rango#DESCONOCIDO})
 * @param refs   ids de elementos relacionados en otras fases
 * @param grupo  agrupación visual (por ejemplo, el ámbito); {@code null} si no aplica
 */
public record Fila(String id, List<String> celdas, Rango origen, List<String> refs, String grupo) {
    public Fila {
        celdas = Copias.lista(celdas);
        refs = Copias.lista(refs);
        if (origen == null) origen = Rango.DESCONOCIDO;
    }
}
