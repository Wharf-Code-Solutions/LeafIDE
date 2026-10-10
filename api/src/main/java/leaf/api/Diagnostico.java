package leaf.api;

/** Diagnóstico de una fase. Record puro: el análisis de los mensajes del compilador vive en el adaptador. */
public record Diagnostico(String faseId, Severidad severidad, Rango rango, String mensaje) {
    public Diagnostico {
        if (severidad == null) severidad = Severidad.ERROR;
        if (rango == null) rango = Rango.DESCONOCIDO;
        if (mensaje == null) mensaje = "";
    }
}
