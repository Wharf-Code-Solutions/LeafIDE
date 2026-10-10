package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javax.swing.text.BadLocationException;
import leaf.api.Diagnostico;
import leaf.api.LenguajeDef;
import leaf.api.Rango;
import leaf.api.Severidad;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.junit.jupiter.api.Test;

class SubrayadoErroresTest {

    private static RSyntaxDocument documento(String texto) throws BadLocationException {
        RSyntaxDocument doc = new RSyntaxDocument(
                new FabricaTokenMaker(new LenguajeDef("Prueba", List.of("x"))), FabricaTokenMaker.ESTILO);
        doc.insertString(0, texto, null);
        return doc;
    }

    private static Diagnostico error(String fase, Rango rango, String mensaje) {
        return new Diagnostico(fase, Severidad.ERROR, rango, mensaje);
    }

    @Test
    void cadaDiagnosticoConPosicionSeSubrayaSobreSuPalabra() throws Exception {
        SubrayadoErrores parser = new SubrayadoErrores();
        parser.establecer(List.of(
                error("semantico", Rango.punto(2, 9), "mala declaración"),
                error("sintactico", Rango.punto(3, 1), "falta algo")));

        ParseResult r = parser.parse(documento("uno\ndefinir $x\nfin"), FabricaTokenMaker.ESTILO);

        assertEquals(2, r.getNotices().size());
        ParserNotice a = r.getNotices().get(0);
        assertEquals(1, a.getLine(), "RSyntaxTextArea cuenta las líneas desde 0");
        assertEquals(12, a.getOffset());
        assertEquals(2, a.getLength());
        assertEquals("Semántico: mala declaración", a.getMessage());
        assertEquals(ParserNotice.Level.ERROR, a.getLevel());
        ParserNotice b = r.getNotices().get(1);
        assertEquals(2, b.getLine());
        assertEquals(15, b.getOffset());
        assertEquals(3, b.getLength());
        assertEquals("Sintáctico: falta algo", b.getMessage());
    }

    @Test
    void losDiagnosticosSinPosicionNoSeSubrayan() throws Exception {
        SubrayadoErrores parser = new SubrayadoErrores();
        parser.establecer(List.of(error("lexico", Rango.DESCONOCIDO, "colapso")));
        assertTrue(parser.parse(documento("abc"), FabricaTokenMaker.ESTILO).getNotices().isEmpty());
        assertEquals(0, parser.cantidad());
    }

    @Test
    void unaLineaVaciaSeSubrayaCompleta() throws Exception {
        SubrayadoErrores parser = new SubrayadoErrores();
        parser.establecer(List.of(error("sintactico", Rango.punto(2, 1), "línea vacía")));
        ParserNotice n = parser.parse(documento("abc\n\nz"), FabricaTokenMaker.ESTILO).getNotices().get(0);
        assertEquals(1, n.getLine());
        assertEquals(false, n.getKnowsOffsetAndLength());
    }

    @Test
    void unaPosicionFueraDelTextoSeAjustaALaUltimaLinea() throws Exception {
        SubrayadoErrores parser = new SubrayadoErrores();
        parser.establecer(List.of(error("sintactico", Rango.punto(40, 2), "fuera")));
        ParserNotice n = parser.parse(documento("abc\nz"), FabricaTokenMaker.ESTILO).getNotices().get(0);
        assertEquals(1, n.getLine());
        assertEquals(4, n.getOffset());
    }

    @Test
    void severidadesSeTraducenANivelesDeRSyntaxTextArea() throws Exception {
        SubrayadoErrores parser = new SubrayadoErrores();
        parser.establecer(List.of(
                new Diagnostico("x", Severidad.ADVERTENCIA, Rango.punto(1, 1), "w"),
                new Diagnostico("x", Severidad.INFO, Rango.punto(1, 1), "i")));
        List<ParserNotice> n = parser.parse(documento("abc"), FabricaTokenMaker.ESTILO).getNotices();
        assertEquals(ParserNotice.Level.WARNING, n.get(0).getLevel());
        assertEquals(ParserNotice.Level.INFO, n.get(1).getLevel());
    }

    @Test
    void establecerReemplazaLoAnterior() throws Exception {
        SubrayadoErrores parser = new SubrayadoErrores();
        parser.establecer(List.of(error("x", Rango.punto(1, 1), "uno")));
        assertEquals(1, parser.cantidad());
        parser.establecer(List.of());
        assertEquals(0, parser.cantidad());
        assertTrue(parser.parse(documento("abc"), FabricaTokenMaker.ESTILO).getNotices().isEmpty());
    }
}
