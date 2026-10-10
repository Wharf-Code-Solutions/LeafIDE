package leaf.adapter.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import generado.ResultadoCompilacion;
import generado.ServicioCompilacion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import leaf.api.ArtefactoTabla;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.EventoFase;
import leaf.api.Fila;
import leaf.api.OpcionesCompilacion;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;
import org.junit.jupiter.api.Test;

class FaseSemanticaTest {

    /** Contexto mínimo en memoria (el real vive en ide-core, del que el adaptador no depende). */
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

    @Test
    void errorInternoDaFalloInternoSinPerderLosDemasDiagnosticos() {
        // Código real con un error semántico, al que se le añade un mensaje "Interno" como el que
        // emite ServicioCompilacion cuando el visitante se interrumpe.
        ResultadoCompilacion r = ServicioCompilacion.compilar("mostrar($y).");
        List<String> mensajes = new ArrayList<>(r.errores);
        mensajes.add("Tipo de Error: Interno : El análisis semántico se interrumpió (java.lang.NullPointerException).");
        ContextoFalso ctx = new ContextoFalso("mostrar($y).");
        ctx.guardar(CompilacionCompartida.CLAVE_DIAGNOSTICOS,
                new CompilacionCompartida.Datos(r, ParserDiagnosticos.analizarTodos(mensajes)));

        ResultadoFase res = new FaseSemantica().ejecutar(ctx);

        assertEquals(Estado.FALLO_INTERNO, res.estado());
        assertEquals(2, res.diagnosticos().size());
        for (Diagnostico d : res.diagnosticos()) {
            assertEquals("semantico", d.faseId());
            assertEquals(Severidad.ERROR, d.severidad());
        }
        assertTrue(res.diagnosticos().get(0).mensaje().contains("'$y' no ha sido declarada"));
        assertEquals(new Rango(1, 9, 1, 9), res.diagnosticos().get(0).rango());
        assertTrue(res.diagnosticos().get(1).mensaje().contains("se interrumpió"));
        assertTrue(res.diagnosticos().get(1).rango().esDesconocido());
        // La tabla parcial se sigue entregando.
        assertEquals(2, res.artefactos().size());
    }

    @Test
    void sinErroresInternosElEstadoDependeDeLosDiagnosticos() {
        assertEquals(Estado.CON_ERRORES, new FaseSemantica().ejecutar(new ContextoFalso("mostrar($y).")).estado());
        assertEquals(Estado.OK,
                new FaseSemantica().ejecutar(new ContextoFalso("definir $x como entero con valor de 1.")).estado());
    }

    @Test
    void laTablaLeeElEstadoFinalDelSimbolo() {
        // tipoRetorno y tiposParametros se asignan DESPUÉS de insertar la función en la tabla.
        String codigo = """
                funcion area($base como decimal, $lados como entero) entrega decimal >
                    retornar $base.
                <
                """;
        ResultadoFase res = new FaseSemantica().ejecutar(new ContextoFalso(codigo));

        assertEquals(Estado.OK, res.estado(), res.diagnosticos().toString());
        ArtefactoTabla simbolos = (ArtefactoTabla) res.artefactos().get(0);
        assertEquals(ConversorTabla.COLUMNAS_SIMBOLOS, simbolos.columnas());
        Fila funcion = simbolos.filas().get(0);
        assertEquals("area", funcion.celdas().get(0));
        assertEquals("funcion", funcion.celdas().get(2));
        assertEquals("(decimal, entero) -> decimal", funcion.celdas().get(7));
        assertEquals("Global", funcion.grupo());
        // La traza es cronológica: sin grupos, para que la UI no la reordene.
        ArtefactoTabla traza = (ArtefactoTabla) res.artefactos().get(1);
        assertEquals(false, traza.filas().isEmpty());
        assertEquals(null, traza.filas().get(0).grupo());
        // Los parámetros quedan agrupados en el ámbito de la función.
        assertEquals("$base", simbolos.filas().get(1).celdas().get(0));
        assertEquals("parametro", simbolos.filas().get(1).celdas().get(2));
        assertEquals("area", simbolos.filas().get(1).grupo());
    }

    @Test
    void toleraErroresPreviosSegunLaOpcion() {
        FaseSemantica fase = new FaseSemantica();
        assertTrue(fase.toleraErroresPrevios(OpcionesCompilacion.porDefecto()));
        assertTrue(fase.toleraErroresPrevios(null));
        assertEquals(false, fase.toleraErroresPrevios(new OpcionesCompilacion(null, false)));
    }
}
