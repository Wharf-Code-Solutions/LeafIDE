package leaf.ide.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import leaf.api.Diagnostico;
import leaf.api.LenguajeDef;
import leaf.api.Rango;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Editor con números de línea, resaltado del lenguaje, subrayado ondulado de los errores y barra de errores a la
 * derecha. Los tabuladores tecleados se convierten en espacios (cada carácter es una columna).
 */
final class PanelEditor extends JPanel {

    private static final Font FUENTE = Fuentes.codigo(15);

    private final RSyntaxTextArea area;
    private final SubrayadoErrores subrayado = new SubrayadoErrores();
    private Highlighter.HighlightPainter pintorDeRango;
    private boolean modificado;
    private Runnable alCambiarModificado = () -> {};
    private Runnable alCambiarTexto = () -> {};
    private BiConsumer<Integer, Integer> alMoverCursor = (l, c) -> {};

    PanelEditor(LenguajeDef lenguaje) {
        super(new BorderLayout());
        area = new RSyntaxTextArea(new RSyntaxDocument(new FabricaTokenMaker(lenguaje), FabricaTokenMaker.ESTILO));
        area.setTabsEmulated(true);
        area.setTabSize(4);
        area.setCodeFoldingEnabled(false);
        area.setAntiAliasingEnabled(true);
        area.setFont(FUENTE);
        area.addParser(subrayado);

        RTextScrollPane scroll = new RTextScrollPane(area);
        scroll.setLineNumbersEnabled(true);
        add(scroll, BorderLayout.CENTER);
        add(new ErrorStrip(area), BorderLayout.LINE_END);

        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cambio(); }
            @Override public void removeUpdate(DocumentEvent e) { cambio(); }
            @Override public void changedUpdate(DocumentEvent e) { cambio(); }
        });
        // Cualquier movimiento del cursor (clic, teclas, edición) retira la marca de la última fila o nodo elegido.
        area.addCaretListener(e -> {
            quitarResaltadoDeRango();
            avisarCursor();
        });
    }

    String texto() {
        return area.getText();
    }

    /** Reemplaza el contenido (sin deshacer) y lo da por guardado. */
    void cargar(String texto) {
        area.setText(texto);
        area.discardAllEdits();
        area.setCaretPosition(0);
        fijarModificado(false);
    }

    boolean modificado() {
        return modificado;
    }

    void marcarGuardado() {
        fijarModificado(false);
    }

    /** Avisa solo cuando cambia el estado "modificado" (para el título). */
    void alCambiarModificado(Runnable accion) {
        this.alCambiarModificado = accion;
    }

    /** Avisa con cada cambio del texto (para el análisis en vivo). */
    void alCambiarTexto(Runnable accion) {
        this.alCambiarTexto = accion;
    }

    /** Recibe (línea, columna), ambas desde 1. */
    void alMoverCursor(BiConsumer<Integer, Integer> accion) {
        this.alMoverCursor = accion;
        avisarCursor();
    }

    /** Subraya los diagnósticos con posición y quita los anteriores. Solo desde el hilo de Swing. */
    void mostrarDiagnosticos(List<Diagnostico> diagnosticos) {
        subrayado.establecer(diagnosticos);
        area.forceReparsing(subrayado);
    }

    /** Cantidad de diagnósticos que se subrayan (los que tienen posición). */
    int cantidadSubrayados() {
        return subrayado.cantidad();
    }

    /**
     * Marca un tramo del texto (un token, un nodo del árbol o una línea) y lo trae a la vista, sin mover el cursor ni
     * quitar el foco a quien lo pidió. La marca se quita sola cuando el cursor se mueve o se edita el texto.
     */
    void resaltarRango(Rango rango) {
        quitarResaltadoDeRango();
        int[] tramo = Rangos.tramo(area.getText(), rango);
        if (tramo == null) return;
        try {
            Color base = UIManager.getColor("TextArea.selectionBackground");
            if (base == null) base = Color.ORANGE;
            pintorDeRango = new DefaultHighlighter.DefaultHighlightPainter(
                    new Color(base.getRed(), base.getGreen(), base.getBlue(), 150));
            area.getHighlighter().addHighlight(tramo[0], tramo[1], pintorDeRango);
            area.scrollRectToVisible(area.modelToView2D(tramo[0]).getBounds());
        } catch (BadLocationException e) {
            pintorDeRango = null; // el tramo ya se ajustó al texto; no debería ocurrir
        }
    }

    /** Tramo marcado por {@link #resaltarRango}, como {@code {inicio, finExclusivo}}, o {@code null} si no hay. */
    int[] tramoResaltado() {
        for (Highlighter.Highlight h : area.getHighlighter().getHighlights()) {
            if (h.getPainter() == pintorDeRango && pintorDeRango != null) return new int[] {h.getStartOffset(), h.getEndOffset()};
        }
        return null;
    }

    private void quitarResaltadoDeRango() {
        if (pintorDeRango == null) return;
        for (Highlighter.Highlight h : area.getHighlighter().getHighlights()) {
            if (h.getPainter() == pintorDeRango) area.getHighlighter().removeHighlight(h);
        }
        pintorDeRango = null;
    }

    /** Enciende o apaga el resaltado del lenguaje (apagado = texto plano). El subrayado de errores no cambia. */
    void activarResaltado(boolean activo) {
        area.setSyntaxEditingStyle(activo ? FabricaTokenMaker.ESTILO : SyntaxConstants.SYNTAX_STYLE_NONE);
    }

    /** Estilo de sintaxis actual: {@link FabricaTokenMaker#ESTILO} o {@code text/plain}. */
    String estilo() {
        return area.getSyntaxEditingStyle();
    }

    /** Lleva el cursor a (línea, columna) del compilador, ambas desde 1, ajustando si el texto es más corto. */
    void irA(int linea, int columna) {
        try {
            int indice = Math.max(0, Math.min(linea - 1, area.getLineCount() - 1));
            int inicio = area.getLineStartOffset(indice);
            String texto = area.getText(inicio, area.getLineEndOffset(indice) - inicio);
            int largo = texto.endsWith("\n") ? texto.length() - 1 : texto.length();
            area.setCaretPosition(inicio + Math.min(Posiciones.offsetEnLinea(texto, columna), largo));
            area.requestFocusInWindow();
        } catch (BadLocationException e) {
            // La posición ya se ajustó arriba; no debería ocurrir.
        }
    }

    void aplicarTema(boolean oscuro) {
        String recurso = "/org/fife/ui/rsyntaxtextarea/themes/" + (oscuro ? "dark.xml" : "default.xml");
        try (InputStream in = RSyntaxTextArea.class.getResourceAsStream(recurso)) {
            if (in != null) Theme.load(in).apply(area);
        } catch (IOException e) {
            // Sin tema se queda con los colores actuales.
        }
        area.setFont(FUENTE);
    }

    private void cambio() {
        fijarModificado(true);
        alCambiarTexto.run();
    }

    private void fijarModificado(boolean valor) {
        if (modificado == valor) return;
        modificado = valor;
        alCambiarModificado.run();
    }

    private void avisarCursor() {
        try {
            int pos = area.getCaretPosition();
            int linea = area.getLineOfOffset(pos);
            alMoverCursor.accept(linea + 1, pos - area.getLineStartOffset(linea) + 1);
        } catch (BadLocationException e) {
            // Posición transitoria durante un reemplazo de texto: se ignora.
        }
    }
}
