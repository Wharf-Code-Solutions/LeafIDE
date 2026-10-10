package leaf.ide.ui;

import io.github.andrewauclair.moderndocking.Dockable;
import io.github.andrewauclair.moderndocking.DockingRegion;
import io.github.andrewauclair.moderndocking.app.Docking;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/** Un panel del IDE que se puede mover, agrupar en pestañas y separar a otra ventana (envuelve a su contenido). */
final class PanelAcoplable extends JPanel implements Dockable {

    private final String id;
    private final boolean cerrable;
    private String titulo;

    PanelAcoplable(String id, String titulo, JComponent contenido, boolean cerrable) {
        super(new BorderLayout());
        this.id = id;
        this.titulo = titulo;
        this.cerrable = cerrable;
        add(contenido, BorderLayout.CENTER);
        Docking.registerDockable(this);
    }

    String titulo() {
        return titulo;
    }

    /** Cambia el texto de la pestaña (por ejemplo, "Léxico · OK" o "Problemas (2)"). */
    void titulo(String nuevo) {
        if (nuevo.equals(titulo)) return;
        titulo = nuevo;
        if (Docking.isDockableRegistered(id)) Docking.updateTabInfo(this);
    }

    boolean acoplado() {
        return Docking.isDocked(this);
    }

    /** En la ventana principal, separado en otra ventana, o sin mostrar. */
    boolean enVentanaPrincipal() {
        return acoplado() && SwingUtilities.getWindowAncestor(this) == Docking.getMainWindow();
    }

    /** Lo pasa a una ventana nueva, junto a la principal. */
    void separar() {
        Window principal = Docking.getMainWindow();
        Point p = principal.getLocationOnScreen();
        Dimension d = new Dimension(Math.max(500, principal.getWidth() / 2), Math.max(350, principal.getHeight() / 2));
        Docking.newWindow(this, new Point(p.x + 60, p.y + 60), d);
    }

    /** Lo devuelve a la ventana principal (como pestaña junto al editor si no hay otro sitio). */
    void acoplarAPrincipal() {
        if (acoplado()) Docking.undock(this);
        Docking.dock(this, Docking.getMainWindow(), DockingRegion.EAST);
    }

    // ---------- Dockable ----------

    @Override public String getPersistentID() { return id; }
    @Override public String getTabText() { return titulo; }
    @Override public String getTitleText() { return titulo; }
    @Override public boolean isClosable() { return cerrable; }
    @Override public boolean isFloatingAllowed() { return true; }
    @Override public boolean isWrappableInScrollpane() { return false; } // el contenido ya trae sus barras

    @Override public boolean hasMoreMenuOptions() { return true; }

    @Override
    public void addMoreOptions(JPopupMenu menu) {
        JMenuItem item = new JMenuItem(enVentanaPrincipal() ? "↗ Separar a una ventana" : "↙ Volver a la ventana principal");
        item.addActionListener(e -> {
            if (enVentanaPrincipal()) separar();
            else acoplarAPrincipal();
        });
        menu.add(item);
    }
}
