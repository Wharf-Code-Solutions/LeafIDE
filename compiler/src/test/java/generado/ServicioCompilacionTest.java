package generado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void unaCompilacionValidaDejaTokensArbolYTabla() {
        ResultadoCompilacion r = ServicioCompilacion.compilar(VALIDO);

        assertNotNull(r.tokenInicial.next);
        assertNotNull(r.raiz);
        assertNotNull(r.tabla.buscar("$x"));
    }
}
