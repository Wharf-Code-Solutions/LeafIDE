package leaf.adapter.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import leaf.api.ArtefactoTabla;
import leaf.api.ArtefactoTexto;
import leaf.api.ContextoCompilacion;
import leaf.api.Estado;
import leaf.api.EventoFase;
import leaf.api.FaseCompilacion;
import leaf.api.Fila;
import leaf.api.ResultadoFase;
import org.junit.jupiter.api.Test;

class FaseOptimizacionTest {

    private static final class ContextoFalso implements ContextoCompilacion {
        private final String codigo;
        private final Map<String, Object> internos = new HashMap<>();

        ContextoFalso(String codigo) { this.codigo = codigo; }

        @Override public String codigo() { return codigo; }
        @Override public Optional<ResultadoFase> resultado(String faseId) { return Optional.empty(); }
        @Override public <T> T dato(String clave, Class<T> tipo) {
            Object o = internos.get(clave);
            return tipo.isInstance(o) ? tipo.cast(o) : null;
        }
        @Override public void guardar(String clave, Object valor) { internos.put(clave, valor); }
        @Override public void publicar(EventoFase evento) {}
    }

    private static ResultadoFase ejecutar(String codigo) {
        return new FaseOptimizacion().ejecutar(new ContextoFalso(codigo));
    }

    private static List<String> operaciones(ResultadoFase r) {
        return ((ArtefactoTabla) r.artefactos().get(0)).filas().stream().map(f -> f.celdas().get(0)).toList();
    }

    @Test
    void estaRegistradaDespuesDelCodigoIntermedio() {
        FaseCompilacion f = new ProveedorLeafV1().fases().stream()
                .filter(x -> x.id().equals("optimizacion")).findFirst().orElseThrow();
        assertTrue(f.implementada());
        assertEquals(List.of("intermedio"), f.dependeDe());
        FaseCompilacion objeto = new ProveedorLeafV1().fases().stream()
                .filter(x -> x.id().equals("objeto")).findFirst().orElseThrow();
        assertEquals(false, objeto.implementada(), "el código objeto sigue pendiente");
    }

    @Test
    void elPlegadoDeConstantesQuitaLaSuma() {
        ResultadoFase r = ejecutar("definir $a como entero con valor de 2 + 3.\nmostrar($a).");

        assertEquals(Estado.OK, r.estado(), r.diagnosticos().toString());
        assertEquals(List.of("Cuádruplos optimizados", "Tres direcciones", "Resumen"),
                r.artefactos().stream().map(a -> a.titulo()).toList());
        ArtefactoTabla t = (ArtefactoTabla) r.artefactos().get(0);
        assertEquals(List.of("Operación", "Arg 1", "Arg 2", "Resultado"), t.columnas());
        assertTrue(!operaciones(r).contains("+"), operaciones(r).toString());
        assertTrue(t.filas().stream().anyMatch(f -> f.celdas().equals(List.of("=", "5", "", "$a"))), t.filas().toString());
        assertEquals("Programa principal", t.filas().get(0).grupo());

        String texto = ((ArtefactoTexto) r.artefactos().get(1)).contenido();
        assertTrue(texto.startsWith("main:\n"), texto);
        assertTrue(texto.contains("$a = 5"), texto);
    }

    @Test
    void elResumenCuentaCuantasCuadruplasSeAhorraron() {
        // Original: main, + 2 3 t1, = t1 $a, print $a, halt (5). Optimizado: main, = 5 $a, print 5, halt (4).
        ResultadoFase r = ejecutar("definir $a como entero con valor de 2 + 3.\nmostrar($a).");
        String resumen = ((ArtefactoTexto) r.artefactos().get(2)).contenido();
        assertTrue(resumen.contains("Código intermedio: 5 cuádruplas"), resumen);
        assertTrue(resumen.contains("Optimizado: 4 cuádruplas"), resumen);
        assertTrue(resumen.contains("1 menos"), resumen);
    }

    @Test
    void sinNadaQueOptimizarLoDice() {
        ResultadoFase r = ejecutar("mostrar(\"hola\").");
        String resumen = ((ArtefactoTexto) r.artefactos().get(2)).contenido();
        assertTrue(resumen.contains("sin cambios"), resumen);
    }

    @Test
    void lasFuncionesSiguenEnSuGrupo() {
        ResultadoFase r = ejecutar("funcion doble($n como entero) entrega entero >\n    retornar $n * 2.\n<\nmostrar(doble(4)).");
        List<Fila> filas = ((ArtefactoTabla) r.artefactos().get(0)).filas();
        Fila func = filas.stream().filter(f -> f.celdas().get(0).equals("func")).findFirst().orElseThrow();
        assertEquals("Función doble", func.grupo());
    }

    @Test
    void conErroresSeOmite() {
        ResultadoFase r = ejecutar("mostrar($noExiste).");
        assertEquals(Estado.OMITIDA, r.estado());
        assertTrue(r.artefactos().isEmpty());
    }
}
