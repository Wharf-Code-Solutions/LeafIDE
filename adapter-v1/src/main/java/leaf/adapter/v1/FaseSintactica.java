package leaf.adapter.v1;

import java.util.ArrayList;
import java.util.List;
import leaf.api.Artefacto;
import leaf.api.ArtefactoArbol;
import leaf.api.ArtefactoTexto;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.NodoVista;
import leaf.api.OpcionesCompilacion;
import leaf.api.ResultadoFase;

/** Fase sintáctica: AST como árbol y como texto, y los errores sintácticos. */
final class FaseSintactica implements FaseCompilacion {

    @Override public String id() { return ParserDiagnosticos.FASE_SINTACTICA; }
    @Override public String nombre() { return "Análisis sintáctico"; }
    @Override public List<String> dependeDe() { return List.of(ParserDiagnosticos.FASE_LEXICA); }

    /** En Leaf el léxico y el sintáctico son una sola pasada (modo pánico): siempre se muestran sus errores. */
    @Override
    public boolean toleraErroresPrevios(OpcionesCompilacion opciones) {
        return true;
    }

    @Override
    public ResultadoFase ejecutar(ContextoCompilacion ctx) {
        CompilacionCompartida.Datos datos = CompilacionCompartida.obtener(ctx);
        List<Diagnostico> diags = datos.deFase(id());
        List<Artefacto> artefactos = new ArrayList<>();
        NodoVista raiz = ConversorAst.convertir(datos.resultado().raiz);
        if (raiz != null) {
            artefactos.add(new ArtefactoArbol("Árbol sintáctico (AST)", raiz));
            artefactos.add(new ArtefactoTexto("Volcado del AST", "texto", ConversorAst.volcado(raiz), null));
        }
        return new ResultadoFase(id(), diags.isEmpty() ? Estado.OK : Estado.CON_ERRORES, diags, artefactos, 0);
    }
}
