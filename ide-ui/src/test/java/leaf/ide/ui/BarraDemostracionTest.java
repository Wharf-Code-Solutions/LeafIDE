package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import leaf.api.ArtefactoTabla;
import leaf.api.Estado;
import leaf.api.Fila;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.ide.core.demo.Demostracion;
import org.junit.jupiter.api.Test;

/** Los controles del modo demostración (sin abrir ventanas, funcionan también sin pantalla). */
class BarraDemostracionTest {

    private static Demostracion demostracion() {
        List<Fila> filas = new ArrayList<>();
        for (int i = 1; i <= 2; i++) filas.add(new Fila("f" + i, List.of("t" + i), Rango.punto(1, i), null, null));
        ResultadoFase lexico = new ResultadoFase("lexico", Estado.OK, List.of(),
                List.of(new ArtefactoTabla("Tokens", List.of("c"), filas)), 0);
        ResultadoFase sintactico = new ResultadoFase("sintactico", Estado.OK, List.of(), List.of(), 0);
        return new Demostracion(List.of(lexico, sintactico));
    }

    @Test
    void fijarMuestraElPrimerPasoYAvisaAlCambiar() {
        List<Demostracion.Paso> vistos = new ArrayList<>();
        BarraDemostracion barra = new BarraDemostracion(d -> vistos.add(d.actual()));
        barra.fijar(demostracion());
        assertEquals(1, vistos.size());
        assertTrue(barra.textoDeNarracion().contains("Léxico"), barra.textoDeNarracion());

        assertTrue(barra.paso());
        assertEquals(2, vistos.size());
        assertEquals(1, vistos.get(1).mostrados());
        assertTrue(barra.textoDeNarracion().contains("1 de 2"), barra.textoDeNarracion());
    }

    @Test
    void siguienteFaseYReiniciar() {
        List<Demostracion.Paso> vistos = new ArrayList<>();
        BarraDemostracion barra = new BarraDemostracion(d -> vistos.add(d.actual()));
        barra.fijar(demostracion());
        assertTrue(barra.siguienteFase());
        assertEquals("sintactico", vistos.get(vistos.size() - 1).faseId());
        barra.reiniciar();
        assertEquals("lexico", vistos.get(vistos.size() - 1).faseId());
        assertEquals(0, vistos.get(vistos.size() - 1).mostrados());
    }

    @Test
    void alFinalNoHayMasPasosNiAutomatico() {
        BarraDemostracion barra = new BarraDemostracion(d -> { });
        barra.fijar(demostracion());
        while (barra.paso()) { /* hasta el final */ }
        assertFalse(barra.paso());
        assertFalse(barra.siguienteFase());
        barra.alternarAuto();
        assertFalse(barra.enAutomatico(), "no se arranca el automático si ya terminó");
    }

    @Test
    void elAutomaticoSeEnciendeYSeApaga() {
        BarraDemostracion barra = new BarraDemostracion(d -> { });
        barra.fijar(demostracion());
        barra.alternarAuto();
        assertTrue(barra.enAutomatico());
        barra.alternarAuto();
        assertFalse(barra.enAutomatico());
        barra.alternarAuto();
        barra.fijar(demostracion()); // una demostración nueva detiene el avance
        assertFalse(barra.enAutomatico());
    }

    @Test
    void sinDemostracionCargadaNoHaceNada() {
        BarraDemostracion barra = new BarraDemostracion(d -> { });
        assertFalse(barra.paso());
        assertFalse(barra.siguienteFase());
        barra.aviso("Compilando…");
        assertEquals("Compilando…", barra.textoDeNarracion());
    }
}
