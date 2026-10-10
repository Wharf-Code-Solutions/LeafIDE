package leaf.ide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;
import org.junit.jupiter.api.Test;

class InformeTest {

    private static final String SEPARADOR = "=".repeat(44);

    private static Diagnostico error(String fase, int linea, int col, String mensaje) {
        return new Diagnostico(fase, Severidad.ERROR, Rango.punto(linea, col), mensaje);
    }

    private static ResultadoFase fase(String id, Estado estado, Diagnostico... diagnosticos) {
        return new ResultadoFase(id, estado, List.of(diagnosticos), List.of(), 1);
    }

    @Test
    void programaValidoComoLaConsolaOriginal() {
        String esperado = "\n" + SEPARADOR + "\n"
                + "Programa válido. No se encontró ningún error.\n"
                + SEPARADOR + "\n";
        assertEquals(esperado, Informe.de(List.of(fase("lexico", Estado.OK), fase("semantico", Estado.OK))));
    }

    @Test
    void erroresOrdenadosPorLineaConElFormatoOriginal() {
        List<ResultadoFase> fases = List.of(
                fase("lexico", Estado.OK),
                fase("sintactico", Estado.CON_ERRORES, error("sintactico", 4, 19, "Falta \")\" para cerrar el paréntesis.")),
                fase("semantico", Estado.CON_ERRORES, error("semantico", 3, 1, "La constante '$PI' es inmutable.")));

        String esperado = "\n" + SEPARADOR + "\n"
                + "  Se encontraron 2 error(es) en total:\n\n"
                + "  Tipo de Error: Semántico Línea 3, Columna 1 : La constante '$PI' es inmutable.\n"
                + "  Tipo de Error: Sintáctico Línea 4, Columna 19 : Falta \")\" para cerrar el paréntesis.\n"
                + SEPARADOR + "\n";
        assertEquals(esperado, Informe.de(fases));
    }

    @Test
    void conIgualLineaSeConservaElOrdenDeLasFases() {
        List<ResultadoFase> fases = List.of(
                fase("lexico", Estado.CON_ERRORES, error("lexico", 2, 5, "uno")),
                fase("sintactico", Estado.CON_ERRORES, error("sintactico", 2, 9, "dos")));
        String texto = Informe.de(fases);
        assertTrue(texto.indexOf("Léxico Línea 2, Columna 5") < texto.indexOf("Sintáctico Línea 2, Columna 9"), texto);
    }

    @Test
    void sinPosicionVaAlFinalYOmiteLaLinea() {
        List<ResultadoFase> fases = List.of(
                fase("lexico", Estado.CON_ERRORES,
                        new Diagnostico("lexico", Severidad.ERROR, Rango.DESCONOCIDO, "Colapso fatal del motor al leer caracteres.")),
                fase("semantico", Estado.CON_ERRORES, error("semantico", 7, 2, "tipos")));
        String texto = Informe.de(fases);
        assertTrue(texto.contains("  Tipo de Error: Semántico Línea 7, Columna 2 : tipos\n"
                + "  Tipo de Error: Léxico : Colapso fatal del motor al leer caracteres.\n"), texto);
    }
}
