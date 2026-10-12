package leaf.ide.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import leaf.ide.core.ListadoCarpeta;

/**
 * Árbol de la carpeta de trabajo: carpetas y programas Leaf ({@code .leaf} y {@code .txt}). Cada carpeta se lee al
 * expandirla. Doble clic o Enter sobre un archivo avisa a quien lo creó para que lo abra.
 */
final class PanelExplorador extends JPanel {

    private static final String SIN_CARPETA = "Sin carpeta. Usa Archivo → Abrir carpeta…";

    private final Consumer<Path> alAbrir;
    private final JLabel titulo = new JLabel(SIN_CARPETA);
    private final DefaultTreeModel modelo = new DefaultTreeModel(null);
    private final JTree arbol = new JTree(modelo);
    private Path raiz;

    /** @param alAbrir qué hacer con un archivo elegido (doble clic o Enter) */
    PanelExplorador(Consumer<Path> alAbrir) {
        super(new BorderLayout());
        this.alAbrir = alAbrir;

        titulo.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        JButton refrescar = new JButton("↻");
        refrescar.setToolTipText("Vuelve a leer la carpeta");
        refrescar.setFocusable(false);
        refrescar.addActionListener(e -> refrescar());
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(titulo, BorderLayout.CENTER);
        arriba.add(refrescar, BorderLayout.EAST);

        arbol.setRootVisible(true);
        arbol.setShowsRootHandles(true);
        arbol.setCellRenderer(new Dibujante());
        arbol.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override public void treeWillExpand(javax.swing.event.TreeExpansionEvent e) {
                cargarHijos((DefaultMutableTreeNode) e.getPath().getLastPathComponent());
            }
            @Override public void treeWillCollapse(javax.swing.event.TreeExpansionEvent e) { }
        });
        arbol.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirSeleccion();
            }
        });
        arbol.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) abrirSeleccion();
            }
        });

        add(arriba, BorderLayout.NORTH);
        add(new JScrollPane(arbol), BorderLayout.CENTER);
    }

    /** Muestra esa carpeta como raíz del árbol (o el aviso de «sin carpeta» si es {@code null}). */
    void fijarRaiz(Path carpeta) {
        raiz = carpeta;
        if (carpeta == null) {
            titulo.setText(SIN_CARPETA);
            titulo.setToolTipText(null);
            modelo.setRoot(null);
            return;
        }
        Path nombre = carpeta.getFileName();
        titulo.setText(nombre == null ? carpeta.toString() : nombre.toString());
        titulo.setToolTipText(carpeta.toString());
        DefaultMutableTreeNode nodo = nodoDe(carpeta);
        modelo.setRoot(nodo);
        cargarHijos(nodo);
        arbol.expandPath(new TreePath(nodo.getPath()));
    }

    void refrescar() {
        if (raiz != null) fijarRaiz(raiz);
    }

    /** Nombres de lo que hay dentro de la carpeta raíz (para las pruebas). */
    List<String> nombresDeLaRaiz() {
        List<String> r = new ArrayList<>();
        if (modelo.getRoot() instanceof DefaultMutableTreeNode n) {
            for (int i = 0; i < n.getChildCount(); i++) {
                Object o = ((DefaultMutableTreeNode) n.getChildAt(i)).getUserObject();
                if (o instanceof Path p) r.add(p.getFileName().toString());
            }
        }
        return r;
    }

    /** Hace lo mismo que el doble clic sobre ese archivo (para las pruebas y el menú). */
    void abrir(Path archivo) {
        if (Files.isRegularFile(archivo)) alAbrir.accept(archivo);
    }

    private void abrirSeleccion() {
        TreePath t = arbol.getSelectionPath();
        if (t == null) return;
        Object o = ((DefaultMutableTreeNode) t.getLastPathComponent()).getUserObject();
        if (o instanceof Path p) abrir(p);
    }

    // ---------- nodos ----------

    private static DefaultMutableTreeNode nodoDe(Path ruta) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(ruta);
        if (Files.isDirectory(ruta) && ListadoCarpeta.tieneContenido(ruta)) {
            n.add(new DefaultMutableTreeNode("…")); // marcador: se reemplaza al expandir
        }
        return n;
    }

    private void cargarHijos(DefaultMutableTreeNode nodo) {
        if (!(nodo.getUserObject() instanceof Path carpeta)) return;
        nodo.removeAllChildren();
        for (Path hijo : ListadoCarpeta.hijos(carpeta)) nodo.add(nodoDe(hijo));
        modelo.nodeStructureChanged(nodo);
    }

    /** Muestra solo el nombre de cada archivo o carpeta, con los íconos del tema. */
    private final class Dibujante extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree t, Object valor, boolean sel, boolean exp, boolean hoja,
                int fila, boolean foco) {
            super.getTreeCellRendererComponent(t, valor, sel, exp, hoja, fila, foco);
            Object o = ((DefaultMutableTreeNode) valor).getUserObject();
            if (o instanceof Path p) {
                Path n = p.getFileName();
                setText(n == null ? p.toString() : n.toString());
                boolean carpeta = Files.isDirectory(p);
                Icon icono = UIManager.getIcon(carpeta ? (exp ? "Tree.openIcon" : "Tree.closedIcon") : "Tree.leafIcon");
                if (icono != null) setIcon(icono);
            }
            setHorizontalAlignment(SwingConstants.LEFT);
            return this;
        }
    }
}
