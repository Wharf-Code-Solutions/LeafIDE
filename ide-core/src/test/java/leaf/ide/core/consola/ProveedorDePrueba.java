package leaf.ide.core.consola;

import java.util.List;
import java.util.function.Function;
import leaf.api.Artefacto;
import leaf.api.ArtefactoArbol;
import leaf.api.ArtefactoTabla;
import leaf.api.ArtefactoTexto;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.Fila;
import leaf.api.LenguajeDef;
import leaf.api.NodoVista;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;

/**
 * Compilador falso con las seis fases de Leaf (las tres últimas "próximamente"). El código decide el resultado:
 * si contiene {@code LEXERR} falla el léxico, con {@code SINERR} el sintáctico y con {@code SEMERR} el semántico.
 */
final class ProveedorDePrueba {

    private ProveedorDePrueba() {}

    private static final class FaseDePrueba implements FaseCompilacion {
        private final String id;
        private final String nombre;
        private final List<String> dependeDe;
        private final boolean implementada;
        private final Function<String, ResultadoFase> accion;

        FaseDePrueba(String id, String nombre, List<String> dependeDe, Function<String, ResultadoFase> accion) {
            this.id = id; this.nombre = nombre; this.dependeDe = dependeDe; this.implementada = accion != null; this.accion = accion;
        }

        @Override public String id() { return id; }
        @Override public String nombre() { return nombre; }
        @Override public List<String> dependeDe() { return dependeDe; }
        @Override public boolean implementada() { return implementada; }
        @Override public boolean toleraErroresPrevios(OpcionesCompilacion o) { return true; }
        @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) { return accion.apply(ctx.codigo()); }
    }

    private static ResultadoFase resultado(String fase, String codigo, String marca, int linea, int col, String mensaje,
                                           List<Artefacto> artefactos) {
        List<Diagnostico> diags = codigo.contains(marca)
                ? List.of(new Diagnostico(fase, Severidad.ERROR, Rango.punto(linea, col), mensaje)) : List.of();
        return new ResultadoFase(fase, diags.isEmpty() ? Estado.OK : Estado.CON_ERRORES, diags, artefactos, 0);
    }

    static ProveedorCompilador crear() {
        List<FaseCompilacion> fases = List.of(
                new FaseDePrueba("lexico", "Análisis léxico", List.of(), c -> resultado("lexico", c, "LEXERR", 1, 1, "símbolo inválido",
                        List.of(new ArtefactoTabla("Tokens", List.of("#", "Lexema"), List.of(
                                new Fila("1", List.of("1", "definir"), Rango.punto(1, 1), null, null),
                                new Fila("2", List.of("2", "$x"), Rango.punto(1, 9), null, null)))))),
                new FaseDePrueba("sintactico", "Análisis sintáctico", List.of("lexico"), c -> resultado("sintactico", c, "SINERR", 2, 1, "falta algo",
                        List.of(new ArtefactoArbol("Árbol sintáctico (AST)", new NodoVista("Programa", null, Rango.DESCONOCIDO, null)),
                                new ArtefactoTexto("Volcado del AST", "texto", "Programa\n  Declaracion\n", null)))),
                new FaseDePrueba("semantico", "Análisis semántico", List.of("sintactico"), c -> resultado("semantico", c, "SEMERR", 3, 5, "tipos incompatibles",
                        List.of(new ArtefactoTabla("Tabla de símbolos", List.of("Nombre", "Tipo"), List.of(
                                        new Fila("s1", List.of("$x", "entero"), Rango.punto(1, 0), null, "Global"))),
                                new ArtefactoTabla("Traza de ámbitos", List.of("#", "Evento"), List.of())))),
                new FaseDePrueba("intermedio", "Código intermedio (cuádruplos)", List.of("semantico"), null),
                new FaseDePrueba("optimizacion", "Optimización", List.of("intermedio"), null),
                new FaseDePrueba("objeto", "Código objeto", List.of("optimizacion"), null));
        return new ProveedorCompilador() {
            @Override public String nombre() { return "Leaf"; }
            @Override public String version() { return "1.0"; }
            @Override public int apiVersion() { return API_VERSION; }
            @Override public List<FaseCompilacion> fases() { return fases; }
            @Override public LenguajeDef lenguaje() { return new LenguajeDef("Leaf", List.of("leaf")); }
        };
    }
}
