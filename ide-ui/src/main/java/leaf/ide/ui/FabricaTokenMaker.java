package leaf.ide.ui;

import java.util.Set;
import leaf.api.LenguajeDef;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker;

/**
 * Entrega el coloreado de una definición de lenguaje ({@link #ESTILO}) o texto plano
 * ({@link SyntaxConstants#SYNTAX_STYLE_NONE}, para apagar el resaltado). Se pasa directamente al documento del
 * editor, en vez de registrarla en la fábrica global de RSyntaxTextArea (que es estado estático compartido).
 */
final class FabricaTokenMaker extends TokenMakerFactory {

    static final String ESTILO = "text/leaf";

    private final LenguajeDef lenguaje;

    FabricaTokenMaker(LenguajeDef lenguaje) {
        this.lenguaje = lenguaje;
    }

    @Override
    protected TokenMaker getTokenMakerImpl(String clave) {
        return ESTILO.equals(clave) ? new TokenMakerLenguaje(lenguaje) : new PlainTextTokenMaker();
    }

    @Override
    public Set<String> keySet() {
        return Set.of(ESTILO, SyntaxConstants.SYNTAX_STYLE_NONE);
    }
}
