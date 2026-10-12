package leaf.adapter.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class FaseIntermedioTest {

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
        return new FaseIntermedio().ejecutar(new ContextoFalso(codigo));
    }

    private static ArtefactoTabla cuadruplos(ResultadoFase r) {
        return (ArtefactoTabla) r.artefactos().get(0);
    }

    @Test
    void estaRegistradaEnElProveedorYYaNoEsNoImplementada() {
        FaseCompilacion f = new ProveedorLeafV1().fases().stream()
                .filter(x -> x.id().equals("intermedio")).findFirst().orElseThrow();
        assertTrue(f.implementada());
        assertEquals(List.of("semantico"), f.dependeDe());
        assertEquals(6, new ProveedorLeafV1().fases().size());
    }

    @Test
    void unProgramaValidoDaTablaTextoYTemporales() {
        ResultadoFase r = ejecutar("definir $a como entero con valor de 2.\ndefinir $b como entero con valor de $a + 3.\nmostrar($b).");

        assertEquals(Estado.OK, r.estado());
        assertEquals(List.of("Cuádruplos", "Tres direcciones", "Temporales"),
                r.artefactos().stream().map(a -> a.titulo()).toList());

        ArtefactoTabla t = cuadruplos(r);
        assertEquals(List.of("#", "Operación", "Arg 1", "Arg 2", "Resultado"), t.columnas());
        assertEquals(List.of("0", "main", "", "", "main"), t.filas().get(0).celdas());
        assertEquals(List.of("1", "=", "2", "", "$a"), t.filas().get(1).celdas());
        Fila ultima = t.filas().get(t.filas().size() - 1);
        assertEquals("halt", ultima.celdas().get(1));

        String texto = ((ArtefactoTexto) r.artefactos().get(1)).contenido();
        assertTrue(texto.startsWith("main:\n"), texto);
        assertTrue(texto.contains("$a = 2"), texto);

        ArtefactoTabla temporales = (ArtefactoTabla) r.artefactos().get(2);
        assertFalse(temporales.filas().isEmpty());
        assertEquals("t1", temporales.filas().get(0).celdas().get(0));
    }

    @Test
    void lasFuncionesQuedanEnSuPropioGrupo() {
        ResultadoFase r = ejecutar("funcion doble($n como entero) entrega entero >\n    retornar $n * 2.\n<\nmostrar(doble(4)).");

        assertEquals(Estado.OK, r.estado());
        List<Fila> filas = cuadruplos(r).filas();
        assertEquals("Programa principal", filas.get(0).grupo());
        Fila func = filas.stream().filter(f -> f.celdas().get(1).equals("func")).findFirst().orElseThrow();
        assertEquals("Función doble", func.grupo());
        Fila fin = filas.stream().filter(f -> f.celdas().get(1).equals("endfunc")).findFirst().orElseThrow();
        assertEquals("Función doble", fin.grupo());
        // Las que están en main (antes de halt) siguen en el principal.
        Fila halt = filas.stream().filter(f -> f.celdas().get(1).equals("halt")).findFirst().orElseThrow();
        assertEquals("Programa principal", halt.grupo());
    }

    @Test
    void conErroresNoHayCuadruplas() {
        ResultadoFase r = ejecutar("mostrar($noExiste).");
        assertEquals(Estado.OMITIDA, r.estado());
        assertTrue(r.artefactos().isEmpty());
    }

    @Test
    void cadaFilaTienePosicionDesconocidaPorAhora() {
        ResultadoFase r = ejecutar("definir $a como entero con valor de 2.");
        for (Fila f : cuadruplos(r).filas()) assertTrue(f.origen().esDesconocido(), f.toString());
    }
}
