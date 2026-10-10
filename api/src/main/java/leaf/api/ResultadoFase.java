package leaf.api;

import java.util.List;

public record ResultadoFase(String faseId, Estado estado, List<Diagnostico> diagnosticos,
                            List<Artefacto> artefactos, long milis) {
    public ResultadoFase {
        diagnosticos = Copias.lista(diagnosticos);
        artefactos = Copias.lista(artefactos);
    }

    /** Copia con otra duración (el pipeline mide el tiempo real). */
    public ResultadoFase conMilis(long nuevosMilis) {
        return new ResultadoFase(faseId, estado, diagnosticos, artefactos, nuevosMilis);
    }
}
