package leaf.ide.core.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import leaf.api.Artefacto;
import leaf.api.ArtefactoArbol;
import leaf.api.ArtefactoTabla;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.Fila;
import leaf.api.NodoVista;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;
import org.junit.jupiter.api.Test;

class DemostracionTest {

    private static ArtefactoTabla tabla(String titulo, int n) {
        List<Fila> filas = new ArrayList<>();
        for (int i = 1; i <= n; i++) filas.add(new Fila("f" + i, List.of("t" + i), Rango.punto(1, i), null, null));
        return new ArtefactoTabla(titulo, List.of("c"), filas);
    }

    private static ArtefactoArbol arbol() {
        return new ArtefactoArbol("AST", new NodoVista("raiz", null, Rango.punto(1, 1),
                List.of(new NodoVista("hijo", "v", Rango.punto(1, 2), List.of()))));
    }

    private static ResultadoFase fase(String id, Estado estado, List<Diagnostico> diags, Artefacto... artefactos) {
        return new ResultadoFase(id, estado, diags, List.of(artefactos), 0);
    }

    private static List<ResultadoFase> programaValido() {
        return List.of(
                fase("lexico", Estado.OK, List.of(), tabla("Tokens", 3), tabla("Conteo", 2)),
                fase("sintactico", Estado.OK, List.of(), arbol()),
                fase("semantico", Estado.OK, List.of()),
                fase("intermedio", Estado.NO_IMPLEMENTADA, List.of()));
    }

    @Test
    void laListaDePasosTieneInicioUnidadesYCierreDeCadaFase() {
        Demostracion d = new Demostracion(programaValido());
        // léxico: inicio + 3 + cierre = 5; sintáctico: inicio + 2 + cierre = 4; semántico: inicio + cierre = 2
        assertEquals(11, d.cantidad());
        assertTrue(d.alPrincipio());
        assertEquals(0, d.actual().mostrados());
        assertTrue(d.actual().texto().contains("Léxico"), d.actual().texto());
        assertTrue(d.vista().isEmpty(), "al empezar no se muestra ninguna fase");
    }

    @Test
    void avanzarMuestraUnElementoMasYMarcaSuRango() {
        Demostracion d = new Demostracion(programaValido());
        assertTrue(d.avanzar());
        assertEquals(1, d.actual().mostrados());
        assertEquals(Rango.punto(1, 1), d.actual().rango());
        assertTrue(d.actual().texto().contains("1 de 3"), d.actual().texto());
        assertTrue(d.actual().texto().contains("t1"), d.actual().texto());

        ResultadoFase l = d.vista().get(0);
        assertEquals("lexico", l.faseId());
        assertEquals(1, l.artefactos().size(), "solo el artefacto principal mientras la fase no termina");
        assertEquals(1, ((ArtefactoTabla) l.artefactos().get(0)).filas().size());
    }

    @Test
    void alCerrarLaFaseSeMuestraCompletaConSusDemasArtefactos() {
        Demostracion d = new Demostracion(programaValido());
        for (int i = 0; i < 4; i++) d.avanzar(); // inicio -> 1, 2, 3 -> cierre
        assertTrue(d.actual().cierre());
        assertTrue(d.actual().texto().contains("terminó"), d.actual().texto());
        assertTrue(d.actual().texto().contains("Siguiente: Sintáctico"), d.actual().texto());
        ResultadoFase l = d.vista().get(0);
        assertEquals(2, l.artefactos().size());
        assertEquals(3, ((ArtefactoTabla) l.artefactos().get(0)).filas().size());
    }

    @Test
    void lasFasesAnterioresSiguenCompletasYLasPosterioresNoSeVenTodavia() {
        Demostracion d = new Demostracion(programaValido());
        d.siguienteFase(); // al inicio de sintáctico
        assertEquals("sintactico", d.actual().faseId());
        assertEquals(0, d.actual().mostrados());
        assertEquals(List.of("lexico"), d.vista().stream().map(ResultadoFase::faseId).toList());
        assertEquals(2, d.vista().get(0).artefactos().size());
    }

    @Test
    void siguienteFaseSaltaALaSiguienteYAlFinalSeQuedaEnElCierre() {
        Demostracion d = new Demostracion(programaValido());
        d.siguienteFase();
        d.siguienteFase();
        assertEquals("semantico", d.actual().faseId());
        d.siguienteFase(); // no hay más fases con pasos: queda en el último paso
        assertTrue(d.alFinal());
        assertTrue(d.actual().cierre());
        assertFalse(d.siguienteFase());
        assertFalse(d.avanzar());
    }

    @Test
    void alFinalTambienSeMuestranLasFasesQueNoSeEjecutaron() {
        Demostracion d = new Demostracion(programaValido());
        while (d.avanzar()) { /* hasta el final */ }
        List<String> ids = d.vista().stream().map(ResultadoFase::faseId).toList();
        assertEquals(List.of("lexico", "sintactico", "semantico", "intermedio"), ids);
    }

    @Test
    void unaFaseConErroresSeDetieneYAvisaQueLasSiguientesSeOmiten() {
        Diagnostico error = new Diagnostico("sintactico", Severidad.ERROR, Rango.punto(2, 3), "falta un punto");
        List<ResultadoFase> r = List.of(
                fase("lexico", Estado.OK, List.of(), tabla("Tokens", 1)),
                fase("sintactico", Estado.CON_ERRORES, List.of(error), arbol()),
                fase("semantico", Estado.OMITIDA, List.of()));
        Demostracion d = new Demostracion(r);
        while (d.avanzar()) { /* hasta el final */ }
        assertTrue(d.actual().texto().contains("1 error"), d.actual().texto());
        assertTrue(d.actual().texto().contains("falta un punto"), d.actual().texto());
        assertTrue(d.actual().texto().contains("se omiten"), d.actual().texto());
        assertEquals("semantico", d.vista().get(2).faseId());
    }

    @Test
    void reiniciarVuelveAlPrincipio() {
        Demostracion d = new Demostracion(programaValido());
        d.siguienteFase();
        d.reiniciar();
        assertTrue(d.alPrincipio());
        assertEquals("lexico", d.actual().faseId());
    }

    @Test
    void sinNingunaFaseImplementadaNoHayPasos() {
        Demostracion d = new Demostracion(List.of(fase("objeto", Estado.NO_IMPLEMENTADA, List.of())));
        assertEquals(0, d.cantidad());
        assertNull(d.actual());
        assertFalse(d.avanzar());
        assertTrue(d.alFinal());
    }
}
