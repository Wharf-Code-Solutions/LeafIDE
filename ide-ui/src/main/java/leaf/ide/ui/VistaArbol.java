package leaf.ide.ui;

import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import leaf.api.NodoVista;
import leaf.api.Rango;

/**
 * Árbol de un {@link NodoVista}. Elegir un nodo resalta su tramo en el editor, y el cursor del editor selecciona el
 * nodo más profundo que lo contiene.
 */
final class VistaArbol extends JScrollPane implements Sincronizable {

    /** Lo que guarda cada nodo del árbol de Swing: el nodo de la API, mostrado como "Etiqueta : valor". */
    record NodoUi(NodoVista nodo) {
        @Override
        public String toString() {
            String valor = nodo.valor() == null || nodo.valor().isBlank() ? "" : " : " + nodo.valor();
            return nodo.etiqueta() + valor;
        }
    }

    private final JTree arbol;
    private final Consumer<Rango> alElegir;
    private boolean sincronizando;

    VistaArbol(NodoVista raiz, Consumer<Rango> alElegir) {
        this.alElegir = alElegir;
        this.arbol = new JTree(new DefaultTreeModel(construir(raiz)));
        arbol.setRootVisible(true);
        for (int i = 0; i < arbol.getRowCount(); i++) arbol.expandRow(i); // expande todo; el contador crece al abrir
        arbol.addTreeSelectionListener(e -> {
            if (!sincronizando) elegir(e.getPath());
        });
        // Un clic en el nodo que ya estaba seleccionado no cambia la selección: se atiende aquí.
        arbol.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                TreePath ruta = arbol.getPathForLocation(e.getX(), e.getY());
                if (ruta != null) elegir(ruta);
            }
        });
        setViewportView(arbol);
    }

    JTree arbol() {
        return arbol;
    }

    /** Primera ruta cuyo nodo tiene esa etiqueta (para las pruebas). */
    TreePath rutaDe(String etiqueta) {
        return buscar(new TreePath(arbol.getModel().getRoot()), etiqueta);
    }

    private static TreePath buscar(TreePath ruta, String etiqueta) {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) ruta.getLastPathComponent();
        if (((NodoUi) n.getUserObject()).nodo().etiqueta().equals(etiqueta)) return ruta;
        for (int i = 0; i < n.getChildCount(); i++) {
            TreePath r = buscar(ruta.pathByAddingChild(n.getChildAt(i)), etiqueta);
            if (r != null) return r;
        }
        return null;
    }

    private static DefaultMutableTreeNode construir(NodoVista nodo) {
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(new NodoUi(nodo));
        for (NodoVista hijo : nodo.hijos()) n.add(construir(hijo));
        return n;
    }

    // ---------- elegir un nodo → resaltar en el editor ----------

    void elegir(TreePath ruta) {
        Object ultimo = ruta.getLastPathComponent();
        if (!(ultimo instanceof DefaultMutableTreeNode n) || !(n.getUserObject() instanceof NodoUi ui)) return;
        if (!ui.nodo().rango().esDesconocido()) alElegir.accept(ui.nodo().rango());
    }

    // ---------- cursor del editor → nodo ----------

    @Override
    public void cursorEn(int linea, int columna) {
        TreePath ruta = masProfundo(new TreePath(arbol.getModel().getRoot()), linea, columna, true);
        if (ruta == null) ruta = masProfundo(new TreePath(arbol.getModel().getRoot()), linea, columna, false);
        sincronizando = true;
        try {
            if (ruta == null) {
                arbol.clearSelection();
            } else {
                arbol.setSelectionPath(ruta);
                Rectangle r = arbol.getPathBounds(ruta);
                if (r != null) arbol.scrollRectToVisible(r);
            }
        } finally {
            sincronizando = false;
        }
    }

    /** El nodo más profundo que contiene el cursor (o, con {@code dentro == false}, que termina justo antes de él). */
    private TreePath masProfundo(TreePath ruta, int linea, int columna, boolean dentro) {
        DefaultMutableTreeNode n = (DefaultMutableTreeNode) ruta.getLastPathComponent();
        Rango r = ((NodoUi) n.getUserObject()).nodo().rango();
        boolean coincide = dentro ? Rangos.contiene(r, linea, columna) : Rangos.tocaAlFinal(r, linea, columna);
        TreePath mejor = coincide ? ruta : null;
        for (int i = 0; i < n.getChildCount(); i++) {
            TreeNode hijo = n.getChildAt(i);
            TreePath enHijo = masProfundo(ruta.pathByAddingChild(hijo), linea, columna, dentro);
            if (enHijo != null) mejor = enHijo; // los hijos son más profundos que el padre
        }
        return mejor;
    }
}
