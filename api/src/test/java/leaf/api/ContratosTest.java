package leaf.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContratosTest {

    @Test
    void diagnosticoNormalizaNulos() {
        Diagnostico d = new Diagnostico("lexico", null, null, null);
        assertEquals(Severidad.ERROR, d.severidad());
        assertEquals(Rango.DESCONOCIDO, d.rango());
        assertEquals("", d.mensaje());
        assertTrue(d.rango().esDesconocido());
    }

    @Test
    void filaEsInmutableYToleraNulos() {
        List<String> celdas = new ArrayList<>(Arrays.asList("a", null));
        Fila f = new Fila("1", celdas, null, null, null);
        celdas.add("c");
        assertEquals(2, f.celdas().size());
        assertNull(f.celdas().get(1));
        assertTrue(f.refs().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> f.celdas().add("x"));
    }

    @Test
    void resultadoFaseConMilis() {
        ResultadoFase r = new ResultadoFase("x", Estado.OK, null, null, 0);
        assertEquals(15, r.conMilis(15).milis());
        assertTrue(r.diagnosticos().isEmpty());
    }

    @Test
    void artefactoEsSellado() {
        Artefacto a = new ArtefactoArbol("AST", new NodoVista("Programa", null, null, null));
        String tipo = switch (a) {
            case ArtefactoTabla t -> "tabla";
            case ArtefactoArbol t -> "arbol";
            case ArtefactoTexto t -> "texto";
        };
        assertEquals("arbol", tipo);
    }

    @Test
    void lenguajeMinimoSigueFuncionando() {
        LenguajeDef l = new LenguajeDef("Leaf", List.of("leaf", "txt"));
        assertEquals("Leaf", l.nombre());
        assertEquals(List.of("leaf", "txt"), l.extensiones());
        assertTrue(l.grupos().isEmpty());
        assertTrue(l.operadores().isEmpty());
        assertTrue(l.delimitadores().isEmpty());
        assertEquals("", l.comentarioLinea());
        assertEquals("", l.prefijoVariable());
        assertTrue(l.palabras(LenguajeDef.RESERVADA).isEmpty());
    }

    @Test
    void lenguajeAmpliadoEsInmutableYAgrupaPalabras() {
        List<String> reservadas = new ArrayList<>(List.of("definir", "con valor de"));
        LenguajeDef l = new LenguajeDef("Leaf", List.of("leaf"),
                List.of(new LenguajeDef.GrupoPalabras(LenguajeDef.RESERVADA, reservadas),
                        new LenguajeDef.GrupoPalabras(LenguajeDef.TIPO, List.of("entero")),
                        new LenguajeDef.GrupoPalabras(LenguajeDef.RESERVADA, List.of("si"))),
                List.of("+", "-/"), List.of(".", "("), "//", "/*", "*/", "$");
        reservadas.add("x");

        assertEquals(List.of("definir", "con valor de", "si"), l.palabras(LenguajeDef.RESERVADA));
        assertEquals(List.of("entero"), l.palabras(LenguajeDef.TIPO));
        assertEquals(4, l.todasLasPalabras().size());
        assertEquals("//", l.comentarioLinea());
        assertEquals("/*", l.comentarioBloqueInicio());
        assertEquals("*/", l.comentarioBloqueFin());
        assertEquals("$", l.prefijoVariable());
        assertThrows(UnsupportedOperationException.class, () -> l.operadores().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> l.grupos().get(0).palabras().add("x"));
    }

    @Test
    void lenguajeToleraNulos() {
        LenguajeDef l = new LenguajeDef(null, null, null, null, null, null, null, null, null);
        assertEquals("", l.nombre());
        assertTrue(l.extensiones().isEmpty());
        assertTrue(l.todasLasPalabras().isEmpty());
        assertEquals("", l.comentarioBloqueFin());
    }

    @Test
    void opcionesPorDefecto() {
        OpcionesCompilacion o = OpcionesCompilacion.porDefecto();
        assertNull(o.hastaFaseId());
        assertTrue(o.ejecutarSemanticoConErroresSintacticos());
    }
}
