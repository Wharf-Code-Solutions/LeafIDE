package leaf.adapter.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import generado.ServicioCompilacion;
import java.util.List;
import leaf.api.Diagnostico;
import org.junit.jupiter.api.Test;

/**
 * La UI supone que cada carácter, tabulador incluido, ocupa una columna ({@code ide-ui}: {@code Posiciones}).
 * Si se regenera el parser con otro {@code tabSize}, esta prueba falla y hay que revisar esa suposición.
 */
class ColumnasTabuladorTest {

    private static Diagnostico primerError(String codigo) {
        List<Diagnostico> d = ParserDiagnosticos.analizarTodos(ServicioCompilacion.compilar(codigo).errores);
        assertEquals(1, d.size(), d.toString());
        return d.get(0);
    }

    @Test
    void sinTabuladorLaColumnaEsElIndiceMasUno() {
        assertEquals(9, primerError("mostrar($zz).").rango().colIni());
    }

    @Test
    void unTabuladorCuentaComoUnaColumna() {
        assertEquals(10, primerError("\tmostrar($zz).").rango().colIni());
    }

    @Test
    void dosTabuladoresCuentanComoDosColumnas() {
        assertEquals(11, primerError("\t\tmostrar($zz).").rango().colIni());
    }
}
