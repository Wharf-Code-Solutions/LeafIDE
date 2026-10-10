package leaf.adapter.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import leaf.api.Diagnostico;
import leaf.api.Rango;
import leaf.api.Severidad;
import org.junit.jupiter.api.Test;

class ParserDiagnosticosTest {

    @Test
    void formatoNormal() {
        Diagnostico d = ParserDiagnosticos.analizar("Tipo de Error: Léxico Línea 3, Columna 5 : Símbolo inválido");
        assertEquals("lexico", d.faseId());
        assertEquals(Severidad.ERROR, d.severidad());
        assertEquals(new Rango(3, 5, 3, 5), d.rango());
        assertEquals("Símbolo inválido", d.mensaje());
    }

    @Test
    void formatoSintacticoLineaDesconocida() {
        Diagnostico d = ParserDiagnosticos.analizar("Tipo de Error: Sintáctico Línea ? : Token inesperado");
        assertEquals("sintactico", d.faseId());
        assertEquals(Severidad.ERROR, d.severidad());
        assertTrue(d.rango().esDesconocido());
        assertEquals("Token inesperado", d.mensaje());
    }

    @Test
    void formatoSemanticoLineaDesconocida() {
        // VisitanteSemantico.reportar emite "Línea ?" cuando el nodo no tiene primer token.
        Diagnostico d = ParserDiagnosticos.analizar("Tipo de Error: Semántico Línea ? : Uso de variable fantasma");
        assertEquals("semantico", d.faseId());
        assertEquals(Rango.DESCONOCIDO, d.rango());
        assertEquals("Uso de variable fantasma", d.mensaje());
    }

    @Test
    void colapsoLexicoSinPosicion() {
        Diagnostico d = ParserDiagnosticos.analizar("Tipo de Error: Léxico : Colapso fatal del motor al leer caracteres.");
        assertEquals("lexico", d.faseId());
        assertEquals(Severidad.ERROR, d.severidad());
        assertTrue(d.rango().esDesconocido());
        assertEquals("Colapso fatal del motor al leer caracteres.", d.mensaje());
    }

    @Test
    void errorInterno() {
        Diagnostico d = ParserDiagnosticos.analizar("Tipo de Error: Interno : El análisis semántico se interrumpió.");
        assertEquals("interno", d.faseId());
        assertEquals(Severidad.ERROR, d.severidad());
        assertTrue(d.rango().esDesconocido());
        assertEquals("El análisis semántico se interrumpió.", d.mensaje());
    }

    @Test
    void formatoDesconocido() {
        String crudo = "Exception in thread main: NullPointerException";
        Diagnostico d = ParserDiagnosticos.analizar(crudo);
        assertEquals("interno", d.faseId());
        assertEquals(Severidad.ERROR, d.severidad());
        assertTrue(d.rango().esDesconocido());
        assertEquals(crudo, d.mensaje());
    }

    @Test
    void analizarTodosIgnoraNulos() {
        List<Diagnostico> lista = ParserDiagnosticos.analizarTodos(null);
        assertTrue(lista.isEmpty());
    }
}
