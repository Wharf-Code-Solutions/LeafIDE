package leaf.adapter.v1;

import generado.TablaSimbolos;
import java.util.ArrayList;
import java.util.List;
import leaf.api.Artefacto;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.OpcionesCompilacion;
import leaf.api.ResultadoFase;

/**
 * Fase semántica: tabla de símbolos agrupada por ámbito, traza de ámbitos y errores semánticos.
 * Absorbe los errores de fase "interno" (se reasignan a esta fase) y entonces termina en FALLO_INTERNO,
 * sin perder los demás diagnósticos semánticos.
 */
final class FaseSemantica implements FaseCompilacion {

    @Override public String id() { return ParserDiagnosticos.FASE_SEMANTICA; }
    @Override public String nombre() { return "Análisis semántico"; }
    @Override public List<String> dependeDe() { return List.of(ParserDiagnosticos.FASE_SINTACTICA); }

    @Override
    public boolean toleraErroresPrevios(OpcionesCompilacion opciones) {
        return opciones == null || opciones.ejecutarSemanticoConErroresSintacticos();
    }

    @Override
    public ResultadoFase ejecutar(ContextoCompilacion ctx) {
        CompilacionCompartida.Datos datos = CompilacionCompartida.obtener(ctx);
        List<Diagnostico> diags = new ArrayList<>(datos.deFase(id()));
        boolean hayInterno = false;
        for (Diagnostico d : datos.deFase(ParserDiagnosticos.FASE_INTERNA)) {
            hayInterno = true;
            diags.add(new Diagnostico(id(), d.severidad(), d.rango(), d.mensaje()));
        }
        List<Artefacto> artefactos = new ArrayList<>();
        TablaSimbolos tabla = datos.resultado().tabla;
        if (tabla != null) {
            artefactos.add(ConversorTabla.tablaSimbolos(tabla));
            artefactos.add(ConversorTabla.tablaTraza(tabla));
        }
        Estado estado = hayInterno ? Estado.FALLO_INTERNO : diags.isEmpty() ? Estado.OK : Estado.CON_ERRORES;
        return new ResultadoFase(id(), estado, diags, artefactos, 0);
    }
}
