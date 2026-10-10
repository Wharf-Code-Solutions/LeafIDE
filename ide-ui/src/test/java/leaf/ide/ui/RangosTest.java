package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import leaf.api.Rango;
import org.junit.jupiter.api.Test;

class RangosTest {

    private static final String DOC = "uno\ndefinir $x como entero\nfin";

    // ---------- Rangos -> tramo del documento ----------

    @Test
    void unTokenSeConvierteEnSuTramoExactoConFinInclusivo() {
        // "$x" en la línea 2: columnas 9 a 10 -> offsets 12 y 13 (fin exclusivo 14).
        assertArrayEquals(new int[] {12, 14}, Rangos.tramo(DOC, new Rango(2, 9, 2, 10)));
        // "definir": columnas 1 a 7.
        assertArrayEquals(new int[] {4, 11}, Rangos.tramo(DOC, new Rango(2, 1, 2, 7)));
    }

    @Test
    void unRangoQueCruzaLineasVaDelInicioAlFinal() {
        // Desde la línea 1 col 2 hasta la línea 2 col 7 ("ino\ndefinir").
        assertArrayEquals(new int[] {1, 11}, Rangos.tramo(DOC, new Rango(1, 2, 2, 7)));
    }

    @Test
    void columnaCeroSignificaLaLineaCompletaSinElSaltoDeLinea() {
        assertArrayEquals(new int[] {4, 26}, Rangos.tramo(DOC, new Rango(2, 0, 2, 0)));
        assertArrayEquals(new int[] {0, 3}, Rangos.tramo(DOC, new Rango(1, 0, 1, 0)));
    }

    @Test
    void unaPosicionFueraDelTextoSeAjusta() {
        // Línea 9 no existe: se usa la última ("fin"); la columna pasada del final se recorta.
        assertArrayEquals(new int[] {27, 30}, Rangos.tramo(DOC, new Rango(9, 1, 9, 99)));
    }

    @Test
    void unRangoDesconocidoNoTieneTramo() {
        assertEquals(null, Rangos.tramo(DOC, Rango.DESCONOCIDO));
    }

    @Test
    void unRangoInvertidoSeNormalizaAUnCaracter() {
        int[] t = Rangos.tramo(DOC, new Rango(2, 9, 2, 3));
        assertEquals(12, t[0]);
        assertEquals(13, t[1]);
    }

    // ---------- ¿el cursor está sobre este rango? ----------

    @Test
    void elCursorEstaDentroDelRangoEnAmbosExtremos() {
        Rango r = new Rango(2, 9, 2, 10);
        assertTrue(Rangos.contiene(r, 2, 9));
        assertTrue(Rangos.contiene(r, 2, 10));
        assertFalse(Rangos.contiene(r, 2, 8));
        assertFalse(Rangos.contiene(r, 2, 11));
        assertFalse(Rangos.contiene(r, 1, 9));
    }

    @Test
    void contieneFuncionaEnRangosDeVariasLineas() {
        Rango r = new Rango(2, 5, 4, 3);
        assertTrue(Rangos.contiene(r, 3, 99));
        assertTrue(Rangos.contiene(r, 2, 5));
        assertTrue(Rangos.contiene(r, 4, 3));
        assertFalse(Rangos.contiene(r, 2, 4));
        assertFalse(Rangos.contiene(r, 4, 4));
    }

    @Test
    void tocaAlFinalSoloPegadoAlUltimoCaracter() {
        Rango r = new Rango(2, 9, 2, 10);
        assertTrue(Rangos.tocaAlFinal(r, 2, 11));
        assertFalse(Rangos.tocaAlFinal(r, 2, 12));
        assertFalse(Rangos.tocaAlFinal(r, 3, 11));
    }

    @Test
    void losRangosDeLineaCompletaYLosDesconocidosNoSeUsanParaElCursor() {
        assertFalse(Rangos.contiene(new Rango(2, 0, 2, 0), 2, 3));
        assertFalse(Rangos.tocaAlFinal(new Rango(2, 0, 2, 0), 2, 1));
        assertFalse(Rangos.contiene(Rango.DESCONOCIDO, 1, 1));
        assertFalse(Rangos.tocaAlFinal(Rango.DESCONOCIDO, 1, 1));
    }
}
