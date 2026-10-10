package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.StyleConstants;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PanelTerminalTest {

    private PanelTerminal terminal;
    private final List<String> enviadas = new ArrayList<>();
    private int interrupciones;

    @BeforeEach
    void preparar() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        terminal = new PanelTerminal();
        terminal.fijarPrompt("leaf> ");
        terminal.alEnviar(enviadas::add);
        terminal.alInterrumpir(() -> interrupciones++);
    }

    /**
     * Pulsa una tecla en el campo de entrada llamando a sus oyentes de teclado. No se usa dispatchEvent porque, sin
     * foco real, el gestor de foco de Swing desvía las teclas antes de que lleguen al campo.
     */
    private void tecla(int codigo, int modificadores) {
        KeyEvent evento = new KeyEvent(terminal.campoDeEntrada(), KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), modificadores, codigo, KeyEvent.CHAR_UNDEFINED);
        for (java.awt.event.KeyListener oyente : terminal.campoDeEntrada().getKeyListeners()) oyente.keyPressed(evento);
    }

    private Color colorEn(int offset) {
        return StyleConstants.getForeground(terminal.salida().getStyledDocument().getCharacterElement(offset).getAttributes());
    }

    // ---------- salida ----------

    @Test
    void imprimeTextoYLoConservaEnOrden() {
        terminal.imprimir("uno\n", PanelTerminal.Estilo.NORMAL);
        terminal.imprimir("dos\n", PanelTerminal.Estilo.NORMAL);
        assertEquals("uno\ndos\n", terminal.texto());
    }

    @Test
    void losSaltosDeWindowsSeNormalizan() {
        terminal.imprimir("uno\r\ndos\r\n", PanelTerminal.Estilo.NORMAL);
        assertEquals("uno\ndos\n", terminal.texto());
    }

    @Test
    void loQueEsErrorSeVeDeOtroColor() {
        terminal.imprimir("bien\n", PanelTerminal.Estilo.NORMAL);
        terminal.imprimir("mal\n", PanelTerminal.Estilo.ERROR);
        assertNotEquals(colorEn(0), colorEn(5));
        assertEquals(new Color(0xD0, 0x40, 0x40), colorEn(5));
    }

    @Test
    void limpiarBorraTodo() {
        terminal.imprimir("algo\n", PanelTerminal.Estilo.NORMAL);
        terminal.limpiar();
        assertEquals("", terminal.texto());
    }

    @Test
    void noCreceSinLimiteYConservaLoMasReciente() {
        String bloque = "x".repeat(99) + "\n";
        for (int i = 0; i < 3000; i++) terminal.imprimir(bloque, PanelTerminal.Estilo.NORMAL);
        terminal.imprimir("ULTIMA\n", PanelTerminal.Estilo.NORMAL);
        assertTrue(terminal.texto().length() <= 200_000, "largo: " + terminal.texto().length());
        assertTrue(terminal.texto().endsWith("ULTIMA\n"));
    }

    // ---------- entrada ----------

    @Test
    void enterEnviaLaLineaLaEcoeaConElPromptYVaciaElCampo() {
        terminal.campoDeEntrada().setText("compilar");
        terminal.campoDeEntrada().postActionEvent();
        assertEquals(List.of("compilar"), enviadas);
        assertEquals("leaf> compilar\n", terminal.texto());
        assertEquals("", terminal.campoDeEntrada().getText());
    }

    @Test
    void lasFlechasRecorrenElHistorial() {
        terminal.enviarLinea("uno");
        terminal.enviarLinea("dos");
        tecla(KeyEvent.VK_UP, 0);
        assertEquals("dos", terminal.campoDeEntrada().getText());
        tecla(KeyEvent.VK_UP, 0);
        assertEquals("uno", terminal.campoDeEntrada().getText());
        tecla(KeyEvent.VK_DOWN, 0);
        assertEquals("dos", terminal.campoDeEntrada().getText());
        tecla(KeyEvent.VK_DOWN, 0);
        assertEquals("", terminal.campoDeEntrada().getText());
    }

    @Test
    void ctrlCSinTextoSeleccionadoInterrumpe() {
        tecla(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        assertEquals(1, interrupciones);
        assertTrue(terminal.texto().contains("^C"), terminal.texto());
    }

    @Test
    void ctrlCConTextoSeleccionadoSoloCopia() {
        terminal.campoDeEntrada().setText("abc");
        terminal.campoDeEntrada().selectAll();
        tecla(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        assertEquals(0, interrupciones);
    }

    @Test
    void ctrlLLimpiaLaPantalla() {
        terminal.imprimir("algo\n", PanelTerminal.Estilo.NORMAL);
        tecla(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        assertEquals("", terminal.texto());
    }

    // ---------- Tab ----------

    private void tabulador(String escrito, List<String> candidatos) {
        List<String> consultas = new ArrayList<>();
        terminal.alCompletar(linea -> {
            consultas.add(linea);
            return candidatos;
        });
        terminal.campoDeEntrada().setText(escrito);
        terminal.campoDeEntrada().setCaretPosition(escrito.length());
        tecla(KeyEvent.VK_TAB, 0);
        assertEquals(List.of(escrito), consultas, "se consulta con el texto hasta el cursor");
    }

    @Test
    void unSoloCandidatoCompletaYAgregaUnEspacio() {
        tabulador("comp", List.of("compilar"));
        assertEquals("compilar ", terminal.campoDeEntrada().getText());
    }

    @Test
    void unCandidatoQueTerminaEnIgualNoAgregaEspacio() {
        tabulador("compilar --h", List.of("--hasta="));
        assertEquals("compilar --hasta=", terminal.campoDeEntrada().getText());
    }

    @Test
    void variosCandidatosCompletanElPrefijoComun() {
        tabulador("compilar --hasta=s", List.of("--hasta=sintactico", "--hasta=semantico"));
        assertEquals("compilar --hasta=s", terminal.campoDeEntrada().getText(), "no hay más prefijo común que lo escrito");
        assertTrue(terminal.texto().contains("--hasta=sintactico  --hasta=semantico"), terminal.texto());
    }

    @Test
    void variosCandidatosConPrefijoComunMasLargoLoAvanzan() {
        tabulador("l", List.of("lexico", "lexicografia"));
        assertEquals("lexico", terminal.campoDeEntrada().getText());
    }

    @Test
    void sinCandidatosNoCambiaNada() {
        tabulador("zzz", List.of());
        assertEquals("zzz", terminal.campoDeEntrada().getText());
    }

    // ---------- doble clic ----------

    @Test
    void laLineaAlrededorDeUnaPosicionSeExtraeDelTexto() {
        String texto = "primera\n  Tipo de Error: Léxico Línea 3, Columna 5 : x\nultima";
        assertEquals("  Tipo de Error: Léxico Línea 3, Columna 5 : x", PanelTerminal.lineaAlrededorDe(texto, 20));
        assertEquals("primera", PanelTerminal.lineaAlrededorDe(texto, 0));
        assertEquals("ultima", PanelTerminal.lineaAlrededorDe(texto, texto.length()));
        assertFalse(PanelTerminal.lineaAlrededorDe("", 0).contains("\n"));
    }

    @Test
    void elPromptSePuedeCambiar() {
        terminal.fijarPrompt("C:\\LeafIDE> ");
        terminal.enviarLinea("dir");
        assertTrue(terminal.texto().startsWith("C:\\LeafIDE> dir"), terminal.texto());
    }
}
