package leaf.ide.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import leaf.ide.core.consola.HistorialComandos;

/**
 * Pantalla de texto con una línea de entrada, como una terminal: la salida se acumula arriba (los errores en rojo), el
 * prompt y la línea de entrada van abajo, ↑ y ↓ recorren el historial, Tab completa y Ctrl+C interrumpe. No sabe qué
 * hace cada línea: eso se le conecta con {@link #alEnviar}, {@link #alCompletar} y {@link #alInterrumpir}.
 * Todo se usa desde el hilo de Swing.
 */
final class PanelTerminal extends JPanel {

    enum Estilo { NORMAL, ERROR, ECO }

    private static final int LIMITE_DE_CARACTERES = 200_000;
    private static final Color ROJO = new Color(0xD0, 0x40, 0x40);

    private final JTextPane salida = new JTextPane() {
        /** Sin ajuste de línea: una línea larga (una tabla) se desplaza en vez de partirse. */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() == null || getUI().getPreferredSize(this).width <= getParent().getWidth();
        }
    };
    private final JLabel etiquetaPrompt = new JLabel();
    private final JTextField entrada = new JTextField();
    private final HistorialComandos historial = new HistorialComandos();
    private final SimpleAttributeSet normal = new SimpleAttributeSet();
    private final SimpleAttributeSet error = new SimpleAttributeSet();
    private final SimpleAttributeSet eco = new SimpleAttributeSet();

    private String prompt = "> ";
    private Consumer<String> alEnviar = linea -> {};
    private Function<String, List<String>> alCompletar = linea -> List.of();
    private Runnable alInterrumpir = () -> {};
    private Consumer<String> alDobleClicEnLinea = linea -> {};

    PanelTerminal() {
        super(new BorderLayout());
        salida.setEditable(false);
        salida.setFont(Fuentes.codigo(14));
        StyleConstants.setForeground(error, ROJO);
        StyleConstants.setBold(eco, true);
        salida.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                try {
                    String texto = salida.getDocument().getText(0, salida.getDocument().getLength());
                    alDobleClicEnLinea.accept(lineaAlrededorDe(texto, salida.viewToModel2D(e.getPoint())));
                } catch (BadLocationException ex) {
                    // fuera del texto: nada que hacer
                }
            }
        });
        add(new JScrollPane(salida), BorderLayout.CENTER);

        entrada.setFont(Fuentes.codigo(14));
        entrada.setFocusTraversalKeysEnabled(false); // Tab completa, no cambia de componente
        entrada.addActionListener(e -> enviarLinea(entrada.getText()));
        entrada.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                atenderTecla(e);
            }
        });
        etiquetaPrompt.setFont(Fuentes.codigo(14));
        etiquetaPrompt.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 2));
        etiquetaPrompt.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel abajo = new JPanel(new BorderLayout());
        abajo.add(etiquetaPrompt, BorderLayout.WEST);
        abajo.add(entrada, BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);
        fijarPrompt(prompt);
    }

    // ---------- conexión ----------

    void alEnviar(Consumer<String> accion) {
        this.alEnviar = accion;
    }

    /** Recibe el texto escrito hasta el cursor y devuelve candidatos para la última palabra (palabras completas). */
    void alCompletar(Function<String, List<String>> accion) {
        this.alCompletar = accion;
    }

    void alInterrumpir(Runnable accion) {
        this.alInterrumpir = accion;
    }

    /** Recibe el texto de la línea de la salida donde se hizo doble clic. */
    void alDobleClicEnLinea(Consumer<String> accion) {
        this.alDobleClicEnLinea = accion;
    }

    String prompt() {
        return prompt;
    }

    void fijarPrompt(String texto) {
        this.prompt = texto;
        etiquetaPrompt.setText(texto);
    }

    // ---------- salida ----------

    void imprimir(String texto, Estilo estilo) {
        if (texto == null || texto.isEmpty()) return;
        StyledDocument doc = salida.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), texto.replace("\r\n", "\n"), switch (estilo) {
                case ERROR -> error;
                case ECO -> eco;
                case NORMAL -> normal;
            });
            recortar(doc);
            salida.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            // el final del documento siempre es una posición válida
        }
    }

    void limpiar() {
        salida.setText("");
    }

    String texto() {
        try {
            return salida.getDocument().getText(0, salida.getDocument().getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    /** Quita las líneas más viejas cuando la salida pasa del límite. */
    private void recortar(StyledDocument doc) throws BadLocationException {
        int largo = doc.getLength();
        if (largo <= LIMITE_DE_CARACTERES) return;
        int sobra = largo - (int) (LIMITE_DE_CARACTERES * 0.8);
        String texto = doc.getText(0, largo);
        int corte = texto.indexOf('\n', sobra);
        doc.remove(0, corte < 0 ? sobra : corte + 1);
    }

    JTextPane salida() {
        return salida;
    }

    JTextField campoDeEntrada() {
        return entrada;
    }

    void enfocar() {
        entrada.requestFocusInWindow();
    }

    /** Lo mismo que escribir la línea y pulsar Enter. */
    void enviarLinea(String linea) {
        imprimir(prompt + linea + "\n", Estilo.ECO);
        historial.agregar(linea);
        entrada.setText("");
        alEnviar.accept(linea);
    }

    // ---------- teclado ----------

    private void atenderTecla(KeyEvent e) {
        int atajo = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        boolean conAtajo = (e.getModifiersEx() & atajo) != 0;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> {
                String anterior = historial.anterior();
                if (anterior != null) entrada.setText(anterior);
                e.consume();
            }
            case KeyEvent.VK_DOWN -> {
                String siguiente = historial.siguiente();
                if (siguiente != null) entrada.setText(siguiente);
                e.consume();
            }
            case KeyEvent.VK_TAB -> {
                completar();
                e.consume();
            }
            case KeyEvent.VK_C -> {
                // Con texto seleccionado Ctrl+C copia; sin selección interrumpe, como en una terminal.
                if (conAtajo && entrada.getSelectedText() == null) {
                    imprimir("^C\n", Estilo.NORMAL);
                    alInterrumpir.run();
                    e.consume();
                }
            }
            case KeyEvent.VK_L -> {
                if (conAtajo) {
                    limpiar();
                    e.consume();
                }
            }
            default -> { }
        }
    }

    private void completar() {
        int cursor = entrada.getCaretPosition();
        String texto = entrada.getText();
        String antes = texto.substring(0, cursor);
        List<String> candidatos = alCompletar.apply(antes);
        if (candidatos.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        int inicio = antes.lastIndexOf(' ') + 1;
        String palabra = antes.substring(inicio);
        String comun = prefijoComun(candidatos);
        if (candidatos.size() == 1) {
            reemplazar(inicio, cursor, comun + (comun.endsWith("=") ? "" : " "));
        } else if (comun.length() > palabra.length()) {
            reemplazar(inicio, cursor, comun);
        } else {
            imprimir(prompt + antes + "\n", Estilo.ECO);
            imprimir(String.join("  ", candidatos) + "\n", Estilo.NORMAL);
        }
    }

    private void reemplazar(int desde, int hasta, String sustituto) {
        String texto = entrada.getText();
        entrada.setText(texto.substring(0, desde) + sustituto + texto.substring(hasta));
        entrada.setCaretPosition(desde + sustituto.length());
    }

    static String prefijoComun(List<String> palabras) {
        String comun = palabras.get(0);
        for (String p : palabras) {
            int i = 0;
            while (i < comun.length() && i < p.length() && comun.charAt(i) == p.charAt(i)) i++;
            comun = comun.substring(0, i);
        }
        return comun;
    }

    /** La línea de {@code texto} que contiene la posición dada (sin el salto de línea). */
    static String lineaAlrededorDe(String texto, int posicion) {
        int pos = Math.max(0, Math.min(posicion, texto.length()));
        int inicio = texto.lastIndexOf('\n', pos - 1) + 1;
        int fin = texto.indexOf('\n', pos);
        return texto.substring(inicio, fin < 0 ? texto.length() : fin);
    }
}
