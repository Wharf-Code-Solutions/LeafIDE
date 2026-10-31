package leaf.adapter.v1;

import generado.Cuadrupla;
import generado.ResultadoCompilacion;
import java.util.ArrayList;
import java.util.List;
import leaf.api.Artefacto;
import leaf.api.ArtefactoTexto;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;

/**
 * Optimización del código intermedio: la lista de cuádruplos que devuelve el {@code Optimizador} del compilador (la
 * definitiva, como en {@code Leaf.main}). La original se ve en la fase de código intermedio. El compilador no anota qué
 * transformación produjo cada cuádrupla, así que aquí no hay columna «transformación».
 */
final class FaseOptimizacion implements FaseCompilacion {

    @Override public String id() { return ProveedorLeafV1.FASE_OPTIMIZACION; }
    @Override public String nombre() { return "Optimización"; }
    @Override public List<String> dependeDe() { return List.of(ProveedorLeafV1.FASE_INTERMEDIO); }

    @Override
    public ResultadoFase ejecutar(ContextoCompilacion ctx) {
        ResultadoCompilacion r = CompilacionCompartida.obtener(ctx).resultado();
        List<Cuadrupla> optimizado = r.codigoOptimizado;
        if (optimizado == null) {
            if (r.fallaOptimizacion == null) return new ResultadoFase(id(), Estado.OMITIDA, List.of(), List.of(), 0);
            Diagnostico d = new Diagnostico(id(), Severidad.ERROR, Rango.DESCONOCIDO,
                    "No se pudo optimizar el código intermedio: " + r.fallaOptimizacion);
            return new ResultadoFase(id(), Estado.FALLO_INTERNO, List.of(d), List.of(), 0);
        }
        int original = r.codigoIntermedio == null ? 0 : r.codigoIntermedio.getCuadruplas().size();
        List<Artefacto> artefactos = List.of(
                FaseIntermedio.tablaCuadruplos("Cuádruplos optimizados", optimizado),
                new ArtefactoTexto("Tres direcciones", "texto", String.join("\n", tresDirecciones(optimizado)), null),
                new ArtefactoTexto("Resumen", "texto", resumen(original, optimizado.size()), null));
        return new ResultadoFase(id(), Estado.OK, List.of(), artefactos, 0);
    }

    /** Igual que la forma de tres direcciones del compilador: sin sangría las etiquetas, main y las funciones. */
    static List<String> tresDirecciones(List<Cuadrupla> cuadruplas) {
        List<String> lineas = new ArrayList<>();
        for (Cuadrupla c : cuadruplas) {
            boolean sinSangria = c.op == Cuadrupla.Op.ETIQUETA || c.op == Cuadrupla.Op.INICIO
                    || c.op == Cuadrupla.Op.FUNC_INICIO || c.op == Cuadrupla.Op.FUNC_FIN;
            lineas.add(sinSangria ? c.aTexto() : "    " + c.aTexto());
        }
        return lineas;
    }

    static String resumen(int original, int optimizado) {
        int diferencia = original - optimizado;
        String cambio = diferencia > 0 ? diferencia + " menos" : diferencia < 0 ? (-diferencia) + " más" : "sin cambios";
        return "Código intermedio: " + original + " cuádruplas\n"
                + "Optimizado: " + optimizado + " cuádruplas (" + cambio + ")";
    }
}
