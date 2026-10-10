package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import leaf.api.LenguajeDef;
import leaf.api.LenguajeDef.GrupoPalabras;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker;
import org.junit.jupiter.api.Test;

class FabricaTokenMakerTest {

    private static final LenguajeDef LENGUAJE = new LenguajeDef("Prueba", List.of("x"),
            List.of(new GrupoPalabras(LenguajeDef.RESERVADA, List.of("definir"))), null, null, null, null, null, "$");

    private static RSyntaxDocument documento(String estilo) throws Exception {
        RSyntaxDocument doc = new RSyntaxDocument(new FabricaTokenMaker(LENGUAJE), estilo);
        doc.insertString(0, "definir $x", null);
        return doc;
    }

    @Test
    void elEstiloDelLenguajeUsaElColoreadoPropio() {
        FabricaTokenMaker fabrica = new FabricaTokenMaker(LENGUAJE);
        assertInstanceOf(TokenMakerLenguaje.class, fabrica.getTokenMaker(FabricaTokenMaker.ESTILO));
    }

    @Test
    void elTextoPlanoNoColoreaNada() {
        FabricaTokenMaker fabrica = new FabricaTokenMaker(LENGUAJE);
        assertInstanceOf(PlainTextTokenMaker.class, fabrica.getTokenMaker(SyntaxConstants.SYNTAX_STYLE_NONE));
        assertTrue(fabrica.keySet().containsAll(List.of(FabricaTokenMaker.ESTILO, SyntaxConstants.SYNTAX_STYLE_NONE)));
    }

    @Test
    void elMismoDocumentoCambiaDeUnModoAOtro() throws Exception {
        RSyntaxDocument doc = documento(FabricaTokenMaker.ESTILO);
        Token coloreado = doc.getTokenListForLine(0);
        assertEquals(TokenTypes.RESERVED_WORD, coloreado.getType());
        assertEquals("definir", coloreado.getLexeme());

        // Texto plano: la palabra ya no es reservada (RSyntaxTextArea la entrega como identificador, que el tema pinta
        // con el color normal del texto).
        doc.setSyntaxStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        assertEquals(TokenTypes.IDENTIFIER, doc.getTokenListForLine(0).getType());

        doc.setSyntaxStyle(FabricaTokenMaker.ESTILO);
        assertEquals(TokenTypes.RESERVED_WORD, doc.getTokenListForLine(0).getType());
    }
}
