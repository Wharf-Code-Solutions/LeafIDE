package generado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** El estado static de Leaf se reinicia en cada compilación (hallazgo 2): las corridas no se mezclan. */
class ServicioCompilacionTest {

    private static final String CON_DOS_ERRORES = "definir $x como entero con valor de \"A\".\nmostrar($y).";
    private static final String VALIDO = "definir $x como entero con valor de 1.";

    @Test
    void elEstadoSeReiniciaEntreCompilaciones() {
        ResultadoCompilacion r1 = ServicioCompilacion.compilar(CON_DOS_ERRORES);
        ResultadoCompilacion r2 = ServicioCompilacion.compilar(VALIDO);
        ResultadoCompilacion r3 = ServicioCompilacion.compilar(CON_DOS_ERRORES);

        assertEquals(2, r1.errores.size(), r1.errores.toString());
        assertEquals(0, r2.errores.size(), r2.errores.toString());
        assertEquals(2, r3.errores.size(), r3.errores.toString());
        assertEquals(r1.errores, r3.errores);
    }

    @Test
    void unProgramaSinErroresGeneraCodigoIntermedio() {
        ResultadoCompilacion r = ServicioCompilacion.compilar("definir $x como entero con valor de 1.\nmostrar($x).");

        assertNotNull(r.codigoIntermedio);
        assertNull(r.fallaCodigoIntermedio);
        assertEquals(Cuadrupla.Op.INICIO, r.codigoIntermedio.getCuadruplas().get(0).op);
        assertEquals("$x", r.codigoIntermedio.getCuadruplas().get(1).resultado);
        assertEquals(Cuadrupla.Op.FIN, r.codigoIntermedio.getCuadruplas().get(r.codigoIntermedio.getCuadruplas().size() - 1).op);
    }

    @Test
    void conErroresNoSeGeneraCodigoIntermedio() {
        ResultadoCompilacion r = ServicioCompilacion.compilar(CON_DOS_ERRORES);

        assertTrue(r.hayErrores());
        assertNull(r.codigoIntermedio);
        assertNull(r.fallaCodigoIntermedio);
    }

    @Test
    void cadaCompilacionEmpiezaSusTemporalesDeCero() {
        String programa = "definir $a como entero con valor de 2.\ndefinir $b como entero con valor de $a + 3.";
        String primera = ServicioCompilacion.compilar(programa).codigoIntermedio.getTexto().toString();
        String segunda = ServicioCompilacion.compilar(programa).codigoIntermedio.getTexto().toString();
        assertEquals(primera, segunda);
    }

    @Test
    void unaCompilacionValidaDejaTokensArbolYTabla() {
        ResultadoCompilacion r = ServicioCompilacion.compilar(VALIDO);

        assertNotNull(r.tokenInicial.next);
        assertNotNull(r.raiz);
        assertNotNull(r.tabla.buscar("$x"));
    }
}
