package leaf.api;

/**
 * (Aditivo, M2) Eventos del pipeline. Los eventos por fila (streaming) llegan en M8.
 */
public sealed interface EventoFase permits EventoFase.FaseIniciada, EventoFase.FaseTerminada {

    long idCompilacion();

    String faseId();

    record FaseIniciada(long idCompilacion, String faseId) implements EventoFase {}

    record FaseTerminada(long idCompilacion, ResultadoFase resultado) implements EventoFase {
        @Override
        public String faseId() {
            return resultado.faseId();
        }
    }
}
