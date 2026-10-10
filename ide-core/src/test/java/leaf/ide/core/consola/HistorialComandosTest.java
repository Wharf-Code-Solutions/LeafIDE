package leaf.ide.core.consola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class HistorialComandosTest {

    @Test
    void anteriorRecorreDeLoMasNuevoALoMasViejoYSeDetieneEnElPrimero() {
        HistorialComandos h = new HistorialComandos();
        h.agregar("uno");
        h.agregar("dos");
        h.agregar("tres");
        assertEquals("tres", h.anterior());
        assertEquals("dos", h.anterior());
        assertEquals("uno", h.anterior());
        assertEquals("uno", h.anterior(), "al llegar al principio se queda en el primero");
    }

    @Test
    void siguienteVuelveHaciaLoNuevoYTerminaEnVacio() {
        HistorialComandos h = new HistorialComandos();
        h.agregar("uno");
        h.agregar("dos");
        h.anterior();
        h.anterior();
        assertEquals("dos", h.siguiente());
        assertEquals("", h.siguiente(), "pasado lo más nuevo se vuelve a la línea en blanco");
        assertEquals("", h.siguiente());
    }

    @Test
    void sinHistorialNoHayNada() {
        HistorialComandos h = new HistorialComandos();
        assertNull(h.anterior());
        assertNull(h.siguiente());
    }

    @Test
    void agregarReiniciaLaPosicion() {
        HistorialComandos h = new HistorialComandos();
        h.agregar("uno");
        h.agregar("dos");
        h.anterior();
        h.agregar("tres");
        assertEquals("tres", h.anterior());
    }

    @Test
    void noGuardaLineasVaciasNiLaMismaDosVecesSeguidas() {
        HistorialComandos h = new HistorialComandos();
        h.agregar("  ");
        h.agregar("uno");
        h.agregar("uno");
        assertEquals(1, h.tamano());
        h.agregar("dos");
        h.agregar("uno");
        assertEquals(3, h.tamano());
    }

    @Test
    void conservaSoloLasUltimasLineasHastaElLimite() {
        HistorialComandos h = new HistorialComandos(3);
        for (String c : new String[] {"a", "b", "c", "d"}) h.agregar(c);
        assertEquals(3, h.tamano());
        h.anterior();
        h.anterior();
        assertEquals("b", h.anterior());
        assertEquals("b", h.anterior());
    }
}
