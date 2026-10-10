package leaf.ide.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;
import leaf.api.Rango;

/**
 * Tabla de un {@link ArtefactoTabla} con filtro, conteo y, si las filas traen grupo, agrupación con encabezados.
 * Una fila con posición conocida resalta ese tramo en el editor al elegirla (con el ratón o el teclado), y el cursor
 * del editor selecciona aquí la fila que lo contiene.
 */
final class VistaTabla extends JPanel implements Sincronizable {

    /** Lo que el usuario dejó puesto en la vista, para reponerlo cuando se reconstruye tras un nuevo análisis. */
    record Estado(String filtro, boolean agrupar) {}

    private final ArtefactoTabla artefacto;
    private final Consumer<Rango> alElegir;
    private final JTable tabla = new JTable();
    private final JTextField campoFiltro = new JTextField(16);
    private final JCheckBox casillaAgrupar = new JCheckBox("Agrupar", true);
    private final JLabel etiquetaConteo = new JLabel();
    private FilasVisibles visibles;
    private boolean sincronizando;

    VistaTabla(ArtefactoTabla artefacto, Consumer<Rango> alElegir) {
        super(new BorderLayout());
        this.artefacto = artefacto;
        this.alElegir = alElegir;

        campoFiltro.putClientProperty("JTextField.placeholderText", "Filtrar…");
        campoFiltro.putClientProperty("JTextField.showClearButton", true);
        campoFiltro.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { reconstruir(); }
            @Override public void removeUpdate(DocumentEvent e) { reconstruir(); }
            @Override public void changedUpdate(DocumentEvent e) { reconstruir(); }
        });
        casillaAgrupar.setToolTipText("Muestra las filas agrupadas, con un encabezado por grupo");
        casillaAgrupar.addActionListener(e -> reconstruir());

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barra.add(campoFiltro);
        if (hayGrupos()) barra.add(casillaAgrupar);
        barra.add(etiquetaConteo);
        add(barra, BorderLayout.NORTH);

        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setDefaultRenderer(Object.class, new RenderizadorDeEncabezados());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !sincronizando && tabla.getSelectedRow() >= 0) elegir(tabla.getSelectedRow());
        });
        // Un clic en la fila que ya estaba seleccionada no cambia la selección: se atiende aquí.
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                if (fila >= 0) elegir(fila);
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        reconstruir();
    }

    // ---------- acceso para la ventana y las pruebas ----------

    JTable tabla() {
        return tabla;
    }

    boolean hayGrupos() {
        return FilasVisibles.hayGrupos(artefacto);
    }

    String conteo() {
        return etiquetaConteo.getText();
    }

    void filtrar(String texto) {
        campoFiltro.setText(texto);
    }

    void agrupar(boolean activo) {
        casillaAgrupar.setSelected(activo);
        reconstruir();
    }

    Estado estado() {
        return new Estado(campoFiltro.getText(), casillaAgrupar.isSelected());
    }

    void restaurar(Estado estado) {
        casillaAgrupar.setSelected(estado.agrupar());
        campoFiltro.setText(estado.filtro()); // si cambia, reconstruye solo; si no, ya está igual
        reconstruir();
    }

    // ---------- elegir una fila → resaltar en el editor ----------

    /** Atiende la fila de la vista: si es una fila con posición conocida, la resalta en el editor. */
    void elegir(int filaVista) {
        FilasVisibles.Entrada entrada = entradaDeVista(filaVista);
        if (entrada == null || entrada.fila() == null) return;
        Rango rango = entrada.fila().origen();
        if (!rango.esDesconocido()) alElegir.accept(rango);
    }

    private FilasVisibles.Entrada entradaDeVista(int filaVista) {
        if (filaVista < 0 || filaVista >= tabla.getRowCount()) return null;
        int modelo = tabla.convertRowIndexToModel(filaVista);
        return modelo < visibles.entradas().size() ? visibles.entradas().get(modelo) : null;
    }

    // ---------- cursor del editor → fila ----------

    @Override
    public void cursorEn(int linea, int columna) {
        int modelo = -1;
        List<FilasVisibles.Entrada> entradas = visibles.entradas();
        for (int i = 0; i < entradas.size() && modelo < 0; i++) {
            Fila f = entradas.get(i).fila();
            if (f != null && Rangos.contiene(f.origen(), linea, columna)) modelo = i;
        }
        for (int i = 0; i < entradas.size() && modelo < 0; i++) {
            Fila f = entradas.get(i).fila();
            if (f != null && Rangos.tocaAlFinal(f.origen(), linea, columna)) modelo = i;
        }
        sincronizando = true;
        try {
            if (modelo < 0) {
                tabla.clearSelection();
            } else {
                int vista = tabla.convertRowIndexToView(modelo);
                tabla.setRowSelectionInterval(vista, vista);
                Rectangle celda = tabla.getCellRect(vista, 0, true);
                tabla.scrollRectToVisible(celda);
            }
        } finally {
            sincronizando = false;
        }
    }

    // ---------- construcción del modelo ----------

    private void reconstruir() {
        boolean agrupado = hayGrupos() && casillaAgrupar.isSelected();
        visibles = FilasVisibles.de(artefacto, campoFiltro.getText(), agrupado);
        etiquetaConteo.setText(visibles.coincidencias() + " de " + visibles.total() + " filas");

        List<? extends RowSorter.SortKey> orden = !agrupado && tabla.getRowSorter() != null
                ? List.copyOf(tabla.getRowSorter().getSortKeys()) : List.of();
        sincronizando = true;
        try {
            tabla.setAutoCreateRowSorter(!agrupado);
            if (agrupado) tabla.setRowSorter(null); // los grupos van en el orden de aparición
            tabla.setModel(ModelosTabla.deEntradas(artefacto.columnas(), visibles.entradas()));
            if (!agrupado && !orden.isEmpty()) tabla.getRowSorter().setSortKeys(orden);
        } finally {
            sincronizando = false;
        }
    }

    /** Los encabezados de grupo se ven en negrita y con el fondo de los encabezados de tabla. */
    private final class RenderizadorDeEncabezados extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada, boolean foco,
                                                       int fila, int columna) {
            Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            FilasVisibles.Entrada entrada = entradaDeVista(fila);
            boolean encabezado = entrada != null && entrada.encabezado() != null;
            c.setFont(encabezado ? t.getFont().deriveFont(Font.BOLD) : t.getFont());
            if (encabezado && !seleccionada) {
                c.setBackground(UIManager.getColor("TableHeader.background"));
            } else if (!seleccionada) {
                c.setBackground(t.getBackground());
            }
            if (c instanceof JLabel l) l.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            return c;
        }
    }
}
