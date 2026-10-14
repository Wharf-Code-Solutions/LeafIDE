package leaf.adapter.v1;

import generado.Cuadrupla;
import generado.GeneradorCodigoIntermedio;
import generado.ResultadoCompilacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import leaf.api.Artefacto;
import leaf.api.ArtefactoTabla;
import leaf.api.ArtefactoTexto;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.Fila;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;

/**
 * Código intermedio en cuádruplos. Solo hay resultado si el programa no tiene errores (como en {@code Leaf.main}); con
 * errores el pipeline la omite porque la semántica terminó con errores. Artefactos: la tabla de cuádruplos de 4 columnas, como la imprime el compilador (agrupada
 * por programa principal y por función), la forma de tres direcciones y el tipo de cada temporal.
 */
final class FaseIntermedio implements FaseCompilacion {

    static final List<String> COLUMNAS_CUADRUPLOS = List.of("Operación", "Arg 1", "Arg 2", "Resultado");
    static final List<String> COLUMNAS_TEMPORALES = List.of("Temporal", "Tipo");
    private static final String GRUPO_PRINCIPAL = "Programa principal";

    @Override public String id() { return ProveedorLeafV1.FASE_INTERMEDIO; }
    @Override public String nombre() { return "Código intermedio (cuádruplos)"; }
    @Override public List<String> dependeDe() { return List.of(ParserDiagnosticos.FASE_SEMANTICA); }

    @Override
    public ResultadoFase ejecutar(ContextoCompilacion ctx) {
        ResultadoCompilacion r = CompilacionCompartida.obtener(ctx).resultado();
        GeneradorCodigoIntermedio generador = r.codigoIntermedio;
        if (generador == null) {
            if (r.fallaCodigoIntermedio == null) {
                // El programa tiene errores léxicos o sintácticos aunque la semántica no haya encontrado ninguno.
                return new ResultadoFase(id(), Estado.OMITIDA, List.of(), List.of(), 0);
            }
            Diagnostico d = new Diagnostico(id(), Severidad.ERROR, Rango.DESCONOCIDO,
                    "No se pudo generar el código intermedio: " + r.fallaCodigoIntermedio);
            return new ResultadoFase(id(), Estado.FALLO_INTERNO, List.of(d), List.of(), 0);
        }
        List<Artefacto> artefactos = List.of(
                tablaCuadruplos(generador.getCuadruplas()),
                new ArtefactoTexto("Tres direcciones", "texto", String.join("\n", generador.getTexto()), null),
                tablaTemporales(generador.getTiposTemporales()));
        return new ResultadoFase(id(), Estado.OK, List.of(), artefactos, 0);
    }

    static ArtefactoTabla tablaCuadruplos(List<Cuadrupla> cuadruplas) {
        List<Fila> filas = new ArrayList<>();
        String grupo = GRUPO_PRINCIPAL;
        for (int i = 0; i < cuadruplas.size(); i++) {
            Cuadrupla c = cuadruplas.get(i);
            if (c.op == Cuadrupla.Op.FUNC_INICIO) grupo = "Función " + c.resultado;
            String[] columnas = c.columnas();
            filas.add(new Fila("q" + i,
                    List.of(columnas[0], columnas[1], columnas[2], columnas[3]),
                    Rango.DESCONOCIDO, List.of(), grupo));
            if (c.op == Cuadrupla.Op.FUNC_FIN) grupo = GRUPO_PRINCIPAL;
        }
        return new ArtefactoTabla("Cuádruplos", COLUMNAS_CUADRUPLOS, filas);
    }

    static ArtefactoTabla tablaTemporales(Map<String, String> tipos) {
        List<Fila> filas = new ArrayList<>();
        for (Map.Entry<String, String> e : tipos.entrySet()) {
            filas.add(new Fila("t:" + e.getKey(), List.of(e.getKey(), e.getValue()), Rango.DESCONOCIDO, List.of(), null));
        }
        return new ArtefactoTabla("Temporales", COLUMNAS_TEMPORALES, filas);
    }
}
