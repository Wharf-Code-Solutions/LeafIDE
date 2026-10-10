package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;
import org.junit.jupiter.api.Test;

class FilasVisiblesTest {

    private static Fila fila(String id, String grupo, String... celdas) {
        return new Fila(id, List.of(celdas), null, null, grupo);
    }

    private static ArtefactoTabla simbolos() {
        return new ArtefactoTabla("Símbolos", List.of("Nombre", "Tipo", "Ámbito"), List.of(
                fila("1", "Global", "$a", "entero", "Global"),
                fila("2", "area", "$base", "decimal", "area"),
                fila("3", "Global", "$b", "texto", "Global"),
                fila("4", "area", "$r", "decimal", "area")));
    }

    private static ArtefactoTabla tokens() {
        return new ArtefactoTabla("Tokens", List.of("#", "Lexema"), List.of(
                fila("1", null, "1", "definir"), fila("2", null, "2", "$x"), fila("3", null, "3", "como")));
    }

    @Test
    void sinFiltroNiAgrupacionDevuelveTodasLasFilasEnOrden() {
        FilasVisibles v = FilasVisibles.de(tokens(), "", false);
        assertEquals(3, v.entradas().size());
        assertEquals(3, v.coincidencias());
        assertEquals(3, v.total());
        assertNull(v.entradas().get(0).encabezado());
        assertEquals("1", v.entradas().get(0).fila().id());
    }

    @Test
    void filtraSinDistinguirMayusculasNiAcentos() {
        ArtefactoTabla t = new ArtefactoTabla("T", List.of("Fase"), List.of(
                fila("1", null, "Léxico"), fila("2", null, "Sintáctico"), fila("3", null, "Semántico")));
        assertEquals(1, FilasVisibles.de(t, "lexico", false).coincidencias());
        assertEquals(1, FilasVisibles.de(t, "SINTACTICO", false).coincidencias());
        assertEquals(1, FilasVisibles.de(t, "sem", false).coincidencias()); // solo "Semántico"
        assertEquals(0, FilasVisibles.de(t, "zzz", false).coincidencias());
        assertEquals(3, FilasVisibles.de(t, "   ", false).coincidencias(), "un filtro en blanco no filtra");
    }

    @Test
    void elFiltroBuscaEnCualquierColumna() {
        FilasVisibles v = FilasVisibles.de(simbolos(), "decimal", false);
        assertEquals(2, v.coincidencias());
        assertEquals(4, v.total());
    }

    @Test
    void agrupaPorElOrdenDePrimeraAparicionConEncabezadosYConteo() {
        FilasVisibles v = FilasVisibles.de(simbolos(), "", true);
        List<FilasVisibles.Entrada> e = v.entradas();
        assertEquals(6, e.size()); // 2 encabezados + 4 filas
        assertEquals("Global (2)", e.get(0).encabezado());
        assertEquals("$a", e.get(1).fila().celdas().get(0));
        assertEquals("$b", e.get(2).fila().celdas().get(0));
        assertEquals("area (2)", e.get(3).encabezado());
        assertEquals("$base", e.get(4).fila().celdas().get(0));
        assertEquals(4, v.coincidencias(), "los encabezados no cuentan como coincidencias");
    }

    @Test
    void conFiltroSoloQuedanLosGruposConCoincidencias() {
        FilasVisibles v = FilasVisibles.de(simbolos(), "texto", true);
        assertEquals(2, v.entradas().size());
        assertEquals("Global (1)", v.entradas().get(0).encabezado());
        assertEquals("$b", v.entradas().get(1).fila().celdas().get(0));
    }

    @Test
    void sinGruposNoHayEncabezadosAunqueSePidaAgrupar() {
        assertFalse(FilasVisibles.hayGrupos(tokens()));
        assertTrue(FilasVisibles.hayGrupos(simbolos()));
        assertEquals(3, FilasVisibles.de(tokens(), "", true).entradas().size());
    }

    @Test
    void lasFilasSinGrupoVanEnSuPropioBloque() {
        ArtefactoTabla t = new ArtefactoTabla("T", List.of("A"), List.of(
                fila("1", "X", "uno"), fila("2", null, "dos"), fila("3", "X", "tres")));
        List<FilasVisibles.Entrada> e = FilasVisibles.de(t, "", true).entradas();
        assertEquals("X (2)", e.get(0).encabezado());
        assertEquals("(sin grupo) (1)", e.get(3).encabezado());
    }
}
