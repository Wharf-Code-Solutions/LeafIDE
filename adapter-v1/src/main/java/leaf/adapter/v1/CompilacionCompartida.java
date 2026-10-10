package leaf.adapter.v1;

import generado.ResultadoCompilacion;
import generado.ServicioCompilacion;
import java.util.List;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;

/**
 * Garantiza que el código se compile UNA sola vez por compilación: la primera fase que lo pide llama a
 * {@link ServicioCompilacion#compilar(String)} y guarda el resultado en el contexto; las demás lo reutilizan.
 * Los objetos internos se quedan en el contexto y nunca salen del adaptador.
 */
final class CompilacionCompartida {

    static final String CLAVE_RESULTADO = "leaf.v1.resultadoCompilacion";
    static final String CLAVE_DIAGNOSTICOS = "leaf.v1.diagnosticos";

    /** Resultado del compilador más sus mensajes ya convertidos. */
    record Datos(ResultadoCompilacion resultado, List<Diagnostico> diagnosticos) {
        List<Diagnostico> deFase(String faseId) {
            return diagnosticos.stream().filter(d -> faseId.equals(d.faseId())).toList();
        }
    }

    private CompilacionCompartida() {}

    static Datos obtener(ContextoCompilacion ctx) {
        Datos datos = ctx.dato(CLAVE_DIAGNOSTICOS, Datos.class);
        if (datos != null) return datos;
        ResultadoCompilacion r = ServicioCompilacion.compilar(ctx.codigo() == null ? "" : ctx.codigo());
        datos = new Datos(r, List.copyOf(ParserDiagnosticos.analizarTodos(r.errores)));
        ctx.guardar(CLAVE_RESULTADO, r);
        ctx.guardar(CLAVE_DIAGNOSTICOS, datos);
        return datos;
    }
}
