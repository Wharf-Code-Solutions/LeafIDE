package generado;

import java.util.List;

/** Resultado de una compilación completa (léxico + sintáctico + semántico + código intermedio). */
public final class ResultadoCompilacion {
    /** Mensajes con el formato original, ya ordenados por línea. */
    public final List<String> errores;
    /** Raíz del AST; null si el parser colapsó antes de producirlo. */
    public final SimpleNode raiz;
    /** Tabla tras el análisis semántico; null si no llegó a correr. */
    public final TablaSimbolos tabla;
    /** Cabeza ficticia de la lista de tokens; los tokens reales empiezan en .next */
    public final Token tokenInicial;
    /** Código intermedio (cuádruplos); null si hubo errores o si la generación falló (ver {@link #fallaCodigoIntermedio}). */
    public final GeneradorCodigoIntermedio codigoIntermedio;
    /** Motivo por el que no se pudo generar el código intermedio de un programa sin errores; null si no hubo falla. */
    public final String fallaCodigoIntermedio;

    ResultadoCompilacion(List<String> errores, SimpleNode raiz, TablaSimbolos tabla, Token tokenInicial,
            GeneradorCodigoIntermedio codigoIntermedio, String fallaCodigoIntermedio) {
        this.errores = errores;
        this.raiz = raiz;
        this.tabla = tabla;
        this.tokenInicial = tokenInicial;
        this.codigoIntermedio = codigoIntermedio;
        this.fallaCodigoIntermedio = fallaCodigoIntermedio;
    }

    public boolean hayErrores() {
        return !errores.isEmpty();
    }
}
