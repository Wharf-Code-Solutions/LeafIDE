package leaf.adapter.v1;

import java.util.List;
import leaf.api.ContextoCompilacion;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.ResultadoFase;

/** Fase registrada pero aún no implementada (intermedio, optimización, objeto). */
final class FaseNoImplementada implements FaseCompilacion {

    private final String id;
    private final String nombre;
    private final List<String> dependeDe;

    FaseNoImplementada(String id, String nombre, List<String> dependeDe) {
        this.id = id;
        this.nombre = nombre;
        this.dependeDe = List.copyOf(dependeDe);
    }

    @Override public String id() { return id; }
    @Override public String nombre() { return nombre; }
    @Override public List<String> dependeDe() { return dependeDe; }
    @Override public boolean implementada() { return false; }

    @Override
    public ResultadoFase ejecutar(ContextoCompilacion ctx) {
        return new ResultadoFase(id, Estado.NO_IMPLEMENTADA, List.of(), List.of(), 0);
    }
}
