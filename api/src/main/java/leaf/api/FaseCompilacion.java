package leaf.api;

import java.util.List;

public interface FaseCompilacion {
    String id();                    // "lexico", "sintactico", "semantico", "intermedio", ...
    String nombre();
    List<String> dependeDe();       // ids de fases previas
    ResultadoFase ejecutar(ContextoCompilacion ctx);

    /**
     * (Aditivo, M2) Si devuelve {@code true}, la fase se ejecuta aunque alguna dependencia haya terminado
     * {@link Estado#CON_ERRORES}. Nunca se ejecuta si una dependencia quedó OMITIDA, NO_IMPLEMENTADA o FALLO_INTERNO.
     * Por defecto {@code false}.
     */
    default boolean toleraErroresPrevios(OpcionesCompilacion opciones) {
        return false;
    }

    /**
     * (Aditivo, M2) {@code false} para fases registradas pero aún no implementadas: el pipeline las reporta
     * {@link Estado#NO_IMPLEMENTADA} sin ejecutarlas ni evaluar sus dependencias. Por defecto {@code true}.
     */
    default boolean implementada() {
        return true;
    }
}
