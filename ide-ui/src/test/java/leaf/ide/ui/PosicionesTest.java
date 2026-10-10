package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PosicionesTest {

    @Test
    void laColumnaEsElIndiceMasUno() {
        assertEquals(0, Posiciones.offsetEnLinea("definir $x", 1));
        assertEquals(8, Posiciones.offsetEnLinea("definir $x", 9));
    }

    @Test
    void unTabuladorOcupaUnaColumna() {
        // Así lo cuenta el compilador real (tabSize = 1): "\tx" -> la x está en la columna 2.
        assertEquals(1, Posiciones.offsetEnLinea("\tx", 2));
        assertEquals(2, Posiciones.offsetEnLinea("\t\tx", 3));
    }

    @Test
    void columnaPasadaDelFinalDevuelveElLargoDeLaLinea() {
        assertEquals(3, Posiciones.offsetEnLinea("abc", 50));
        assertEquals(0, Posiciones.offsetEnLinea("", 1));
        assertEquals(0, Posiciones.offsetEnLinea("abc", 0));
    }

    @Test
    void subrayaLaPalabraQueEmpiezaEnLaPosicion() {
        String doc = "uno\ndefinir $x como entero\nfin";
        // línea 2, columna 9 -> "$x" (la línea empieza en el offset 4).
        assertArrayEquals(new int[] {12, 2}, Posiciones.subrayado(doc, 2, 9));
        // línea 2, columna 1 -> "definir"
        assertArrayEquals(new int[] {4, 7}, Posiciones.subrayado(doc, 2, 1));
    }

    @Test
    void subrayaUnaCadenaCompleta() {
        String doc = "mostrar(\"hola mundo\")";
        assertArrayEquals(new int[] {8, 12}, Posiciones.subrayado(doc, 1, 9));
    }

    @Test
    void unSimboloSubrayaUnCaracter() {
        assertArrayEquals(new int[] {7, 1}, Posiciones.subrayado("mostrar)", 1, 8));
    }

    @Test
    void alFinalDeLaLineaSubrayaElUltimoCaracter() {
        assertArrayEquals(new int[] {2, 1}, Posiciones.subrayado("abc", 1, 99));
    }

    @Test
    void lineaVaciaDevuelveLargoCeroYLineaFueraDeRangoSeAjusta() {
        assertArrayEquals(new int[] {4, 0}, Posiciones.subrayado("abc\n\nz", 2, 1));
        // Línea 9 no existe: se usa la última ("z", offset 5).
        assertArrayEquals(new int[] {5, 1}, Posiciones.subrayado("abc\n\nz", 9, 1));
    }

    @Test
    void conTabuladoresSubrayaLaPalabraCorrecta() {
        // "\tmostrar": la palabra empieza en la columna 2.
        assertArrayEquals(new int[] {1, 7}, Posiciones.subrayado("\tmostrar", 1, 2));
    }
}
