package generado;

import java.util.List;

/** Resultado de una compilación completa (léxico + sintáctico + semántico). */
public final class ResultadoCompilacion {
    /** Mensajes con el formato original, ya ordenados por línea. */
    public final List<String> errores;
    /** Raíz del AST; null si el parser colapsó antes de producirlo. */
    public final SimpleNode raiz;
    /** Tabla tras el análisis semántico; null si no llegó a correr. */
    public final TablaSimbolos tabla;
    /** Cabeza ficticia de la lista de tokens; los tokens reales empiezan en .next */
    public final Token tokenInicial;

    ResultadoCompilacion(List<String> errores, SimpleNode raiz, TablaSimbolos tabla, Token tokenInicial) {
        this.errores = errores;
        this.raiz = raiz;
        this.tabla = tabla;
        this.tokenInicial = tokenInicial;
    }

    public boolean hayErrores() {
        return !errores.isEmpty();
    }
}
