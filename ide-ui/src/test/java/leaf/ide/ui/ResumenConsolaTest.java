package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import leaf.api.Estado;
import org.junit.jupiter.api.Test;

class ResumenConsolaTest {

    @Test
    void etiquetasDeEstadoParaLasPestanas() {
        assertEquals("OK", ResumenConsola.etiqueta(Estado.OK));
        assertEquals("con errores", ResumenConsola.etiqueta(Estado.CON_ERRORES));
        assertEquals("omitida", ResumenConsola.etiqueta(Estado.OMITIDA));
        assertEquals("próximamente", ResumenConsola.etiqueta(Estado.NO_IMPLEMENTADA));
        assertEquals("fallo interno", ResumenConsola.etiqueta(Estado.FALLO_INTERNO));
    }
}
