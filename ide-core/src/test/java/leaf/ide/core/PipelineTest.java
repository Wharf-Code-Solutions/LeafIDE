package leaf.ide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import leaf.api.ContextoCompilacion;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.ResultadoFase;
import org.junit.jupiter.api.Test;

class PipelineTest {

    static class FaseFalsa implements FaseCompilacion {
        private final String id;
        private final List<String> deps;
        private final boolean implementada;
        private final boolean tolera;
        private final ResultadoFase resAEntregar;

        public FaseFalsa(String id, List<String> deps, boolean implementada, boolean tolera, ResultadoFase res) {
            this.id = id; this.deps = deps; this.implementada = implementada; this.tolera = tolera; this.resAEntregar = res;
        }

        @Override public String id() { return id; }
        @Override public String nombre() { return id; }
        @Override public List<String> dependeDe() { return deps; }
        @Override public boolean implementada() { return implementada; }
        @Override public boolean toleraErroresPrevios(OpcionesCompilacion opciones) { return tolera; }
        @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) { return resAEntregar; }
    }

    @Test
    void ordenTopologicoSimple() {
        FaseCompilacion f1 = new FaseFalsa("A", List.of(), true, false, null);
        FaseCompilacion f2 = new FaseFalsa("B", List.of("A"), true, false, null);
        FaseCompilacion f3 = new FaseFalsa("C", List.of("B", "A"), true, false, null);

        // Desordenadas
        List<FaseCompilacion> ordenadas = Pipeline.ordenarFases(List.of(f3, f1, f2));
        assertEquals(3, ordenadas.size());
        assertEquals("A", ordenadas.get(0).id());
        assertEquals("B", ordenadas.get(1).id());
        assertEquals("C", ordenadas.get(2).id());
    }

    @Test
    void omiteSiDependenciaTieneErrorYNoTolera() {
        EventBus bus = new EventBus();
        
        ResultadoFase errorLexico = new ResultadoFase("lexico", Estado.CON_ERRORES, List.of(), List.of(), 0);
        FaseCompilacion lex = new FaseFalsa("lexico", List.of(), true, false, errorLexico);
        
        // No tolera errores (por defecto sintáctico no tolera si no lo dice, en nuestra prueba pusimos false)
        FaseCompilacion sin = new FaseFalsa("sintactico", List.of("lexico"), true, false, null);

        ProveedorCompilador prov = new ProveedorCompilador() {
            @Override public int apiVersion() { return 1; }
            @Override public String nombre() { return "Test"; }
            @Override public String version() { return "1.0"; }
            @Override public leaf.api.LenguajeDef lenguaje() { return null; }
            @Override public List<FaseCompilacion> fases() { return List.of(lex, sin); }
        };

        Pipeline pipeline = new Pipeline(prov, bus);
        List<ResultadoFase> res = pipeline.ejecutar(1, "codigo", null);

        assertEquals(2, res.size());
        assertEquals(Estado.CON_ERRORES, res.get(0).estado());
        assertEquals(Estado.OMITIDA, res.get(1).estado());
    }

    @Test
    void ejecutaSiDependenciaTieneErrorPeroTolera() {
        EventBus bus = new EventBus();
        
        ResultadoFase errorLexico = new ResultadoFase("lexico", Estado.CON_ERRORES, List.of(), List.of(), 0);
        FaseCompilacion lex = new FaseFalsa("lexico", List.of(), true, false, errorLexico);
        
        ResultadoFase resSin = new ResultadoFase("sintactico", Estado.OK, List.of(), List.of(), 0);
        // Sí tolera
        FaseCompilacion sin = new FaseFalsa("sintactico", List.of("lexico"), true, true, resSin);

        ProveedorCompilador prov = new ProveedorCompilador() {
            @Override public int apiVersion() { return 1; }
            @Override public String nombre() { return "Test"; }
            @Override public String version() { return "1.0"; }
            @Override public leaf.api.LenguajeDef lenguaje() { return null; }
            @Override public List<FaseCompilacion> fases() { return List.of(lex, sin); }
        };

        Pipeline pipeline = new Pipeline(prov, bus);
        List<ResultadoFase> res = pipeline.ejecutar(1, "codigo", null);

        assertEquals(2, res.size());
        assertEquals(Estado.CON_ERRORES, res.get(0).estado());
        assertEquals(Estado.OK, res.get(1).estado()); // Se ejecutó y fue exitosa
    }

    private static ProveedorCompilador proveedor(FaseCompilacion... fases) {
        return new ProveedorCompilador() {
            @Override public int apiVersion() { return 1; }
            @Override public String nombre() { return "Test"; }
            @Override public String version() { return "1.0"; }
            @Override public leaf.api.LenguajeDef lenguaje() { return null; }
            @Override public List<FaseCompilacion> fases() { return List.of(fases); }
        };
    }

    private static ResultadoFase ok(String id) {
        return new ResultadoFase(id, Estado.OK, List.of(), List.of(), 0);
    }

    @Test
    void hastaCortaDespuesDeLaFaseIndicada() {
        FaseCompilacion a = new FaseFalsa("A", List.of(), true, false, ok("A"));
        FaseCompilacion b = new FaseFalsa("B", List.of("A"), true, false, ok("B"));
        FaseCompilacion c = new FaseFalsa("C", List.of("B"), true, false, ok("C"));

        List<ResultadoFase> res = new Pipeline(proveedor(a, b, c), new EventBus())
                .ejecutar(1, "codigo", OpcionesCompilacion.hasta("B"));

        assertEquals(2, res.size());
        assertEquals("A", res.get(0).faseId());
        assertEquals("B", res.get(1).faseId());
        assertEquals(Estado.OK, res.get(1).estado());
    }

    @Test
    void faseQueLanzaExcepcionDaFalloInternoYNoTumbaElPipeline() {
        FaseCompilacion a = new FaseFalsa("A", List.of(), true, false, null) {
            @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) {
                throw new IllegalStateException("se rompió");
            }
        };
        FaseCompilacion b = new FaseFalsa("B", List.of("A"), true, true, ok("B"));
        FaseCompilacion independiente = new FaseFalsa("X", List.of(), true, false, ok("X"));

        List<ResultadoFase> res = new Pipeline(proveedor(a, b, independiente), new EventBus())
                .ejecutar(1, "codigo", null);

        assertEquals(3, res.size());
        assertEquals(Estado.FALLO_INTERNO, res.get(0).estado());
        assertEquals(1, res.get(0).diagnosticos().size());
        assertEquals("A", res.get(0).diagnosticos().get(0).faseId());
        assertTrue(res.get(0).diagnosticos().get(0).mensaje().contains("se rompió"));
        // Depender de un FALLO_INTERNO omite la fase aunque tolere errores previos.
        assertEquals(Estado.OMITIDA, res.get(1).estado());
        assertEquals(Estado.OK, res.get(2).estado());
    }

    @Test
    void faseNoImplementadaNoSeEjecuta() {
        List<String> ejecutadas = new ArrayList<>();
        FaseCompilacion a = new FaseFalsa("A", List.of(), true, false, ok("A"));
        FaseCompilacion futura = new FaseFalsa("futura", List.of("A"), false, false, null) {
            @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) {
                ejecutadas.add("futura");
                return ok("futura");
            }
        };
        // Otra fase futura que depende de la anterior: también NO_IMPLEMENTADA (no OMITIDA).
        FaseCompilacion futura2 = new FaseFalsa("futura2", List.of("futura"), false, false, null);
        // Una fase implementada que depende de una no implementada sí se omite.
        FaseCompilacion real = new FaseFalsa("real", List.of("futura"), true, false, ok("real"));

        List<ResultadoFase> res = new Pipeline(proveedor(a, futura, futura2, real), new EventBus())
                .ejecutar(1, "codigo", null);

        assertEquals(4, res.size());
        assertEquals(Estado.OK, res.get(0).estado());
        assertEquals(Estado.NO_IMPLEMENTADA, res.get(1).estado());
        assertEquals(Estado.NO_IMPLEMENTADA, res.get(2).estado());
        assertEquals(Estado.OMITIDA, res.get(3).estado());
        assertTrue(ejecutadas.isEmpty());
        assertTrue(res.get(1).artefactos().isEmpty());
    }

    @Test
    void laOmisionSePropagaEnCadena() {
        ResultadoFase conErrores = new ResultadoFase("A", Estado.CON_ERRORES, List.of(), List.of(), 0);
        FaseCompilacion a = new FaseFalsa("A", List.of(), true, false, conErrores);
        FaseCompilacion b = new FaseFalsa("B", List.of("A"), true, false, ok("B"));
        // C tolera errores previos, pero su dependencia quedó OMITIDA (no CON_ERRORES): también se omite.
        FaseCompilacion c = new FaseFalsa("C", List.of("B"), true, true, ok("C"));
        FaseCompilacion d = new FaseFalsa("D", List.of("C"), true, false, ok("D"));

        List<ResultadoFase> res = new Pipeline(proveedor(a, b, c, d), new EventBus()).ejecutar(1, "codigo", null);

        assertEquals(4, res.size());
        assertEquals(Estado.CON_ERRORES, res.get(0).estado());
        assertEquals(Estado.OMITIDA, res.get(1).estado());
        assertEquals(Estado.OMITIDA, res.get(2).estado());
        assertEquals(Estado.OMITIDA, res.get(3).estado());
    }
}
