package leaf.ide.ui;

import java.util.List;
import javax.swing.text.BadLocationException;
import leaf.api.Diagnostico;
import leaf.ide.core.NombresFase;
import leaf.api.Severidad;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;

/**
 * Subrayado ondulado de los diagnósticos. No analiza nada: RSyntaxTextArea llama a {@link #parse} cuando se le pide
 * releer el texto y aquí solo se traducen los diagnósticos que dejó el último análisis del pipeline a avisos.
 * Los diagnósticos sin posición (colapso léxico, errores internos) no se subrayan: siguen en Problemas y en la Consola.
 */
final class SubrayadoErrores extends AbstractParser {

    private volatile List<Diagnostico> diagnosticos = List.of();

    void establecer(List<Diagnostico> nuevos) {
        diagnosticos = List.copyOf(nuevos);
    }

    /** Cantidad de diagnósticos con posición, que son los que se subrayan. */
    int cantidad() {
        return (int) diagnosticos.stream().filter(d -> !d.rango().esDesconocido()).count();
    }

    @Override
    public ParseResult parse(RSyntaxDocument doc, String estilo) {
        DefaultParseResult resultado = new DefaultParseResult(this);
        String texto;
        try {
            texto = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return resultado;
        }
        for (Diagnostico d : diagnosticos) {
            if (d.rango().esDesconocido()) continue;
            int[] tramo = Posiciones.subrayado(texto, d.rango().lineaIni(), d.rango().colIni());
            int linea = doc.getDefaultRootElement().getElementIndex(tramo[0]);
            String mensaje = NombresFase.de(d.faseId()) + ": " + d.mensaje();
            DefaultParserNotice aviso = tramo[1] == 0
                    ? new DefaultParserNotice(this, mensaje, linea)
                    : new DefaultParserNotice(this, mensaje, linea, tramo[0], tramo[1]);
            aviso.setLevel(nivel(d.severidad()));
            resultado.addNotice(aviso);
        }
        return resultado;
    }

    private static ParserNotice.Level nivel(Severidad s) {
        return switch (s) {
            case ERROR -> ParserNotice.Level.ERROR;
            case ADVERTENCIA -> ParserNotice.Level.WARNING;
            case INFO -> ParserNotice.Level.INFO;
        };
    }
}
