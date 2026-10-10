package leaf.adapter.v1;

import java.util.List;
import leaf.api.Artefacto;
import leaf.api.ArtefactoTabla;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.ResultadoFase;

/** Fase léxica: tabla de tokens + conteo por categoría, y los errores léxicos. */
final class FaseLexica implements FaseCompilacion {

    @Override public String id() { return ParserDiagnosticos.FASE_LEXICA; }
    @Override public String nombre() { return "Análisis léxico"; }
    @Override public List<String> dependeDe() { return List.of(); }

    @Override
    public ResultadoFase ejecutar(ContextoCompilacion ctx) {
        CompilacionCompartida.Datos datos = CompilacionCompartida.obtener(ctx);
        List<Diagnostico> diags = datos.deFase(id());
        ArtefactoTabla tokens = ConversorTokens.tablaTokens(datos.resultado().tokenInicial);
        List<Artefacto> artefactos = List.of(tokens, ConversorTokens.tablaConteo(tokens));
        return new ResultadoFase(id(), diags.isEmpty() ? Estado.OK : Estado.CON_ERRORES, diags, artefactos, 0);
    }
}
