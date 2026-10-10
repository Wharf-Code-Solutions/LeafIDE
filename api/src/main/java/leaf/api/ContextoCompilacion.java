package leaf.api;

import java.util.Optional;

public interface ContextoCompilacion {
    String codigo();
    Optional<ResultadoFase> resultado(String faseId);
    /** Objeto interno guardado por una fase previa; {@code null} si no existe o no es del tipo pedido. */
    <T> T dato(String clave, Class<T> tipo);   // objetos internos (SimpleNode, TablaSimbolos)
    void guardar(String clave, Object valor);  // que NUNCA llegan a la UI
    void publicar(EventoFase evento);
}
