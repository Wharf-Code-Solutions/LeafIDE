package leaf.ide.ui;

import io.github.andrewauclair.moderndocking.Dockable;
import io.github.andrewauclair.moderndocking.DockingRegion;
import io.github.andrewauclair.moderndocking.app.Docking;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma cada {@link Perspectiva} con el docking: quita todos los paneles y los vuelve a colocar. Las fases se ubican
 * por su id, así que una fase nueva del compilador se añade aquí sin tocar el resto de la UI.
 */
final class Disposiciones {

    private static final List<String> ANALISIS = List.of("lexico", "sintactico", "semantico");
    private static final List<String> SINTESIS = List.of("intermedio", "optimizacion", "objeto");

    private final PanelAcoplable editor;
    private final List<PanelAcoplable> consolas; // Problemas, Leaf, Sistema
    private final GrupoFases fases;

    Disposiciones(PanelAcoplable editor, List<PanelAcoplable> consolas, GrupoFases fases) {
        this.editor = editor;
        this.consolas = List.copyOf(consolas);
        this.fases = fases;
    }

    /** Todos los paneles que existen, en el orden del menú Ver. */
    List<PanelAcoplable> todos() {
        List<PanelAcoplable> r = new ArrayList<>();
        r.add(editor);
        for (int i = 0; i < fases.cantidad(); i++) r.add(fases.acoplable(i));
        r.addAll(consolas);
        return r;
    }

    void aplicar(Perspectiva p) {
        Window principal = Docking.getMainWindow();
        for (PanelAcoplable panel : todos()) if (panel.acoplado()) Docking.undock(panel);

        Docking.dock(editor, principal);
        switch (p) {
            case ESCRITURA -> {
                consolasAbajo(principal, 0.30);
            }
            case ANALISIS -> {
                enPestanas(editor, DockingRegion.EAST, 0.55, ANALISIS);
                consolasAbajo(principal, 0.30);
            }
            case SINTESIS -> {
                columnas(editor, SINTESIS, 0.75);
                consolasAbajo(principal, 0.30);
            }
            case EVALUACION -> {
                enPestanas(editor, DockingRegion.EAST, 0.55, ANALISIS);
                consolasAbajo(principal, 0.30);
                ventanaAparte(List.of("intermedio", "optimizacion"), 1);
                ventanaAparte(List.of("objeto"), 2);
            }
        }
        Docking.bringToFront(editor);
    }

    /** Problemas, Leaf y Sistema como pestañas, a lo ancho de toda la ventana. */
    private void consolasAbajo(Window ventana, double proporcion) {
        PanelAcoplable primera = null;
        for (PanelAcoplable c : consolas) {
            if (primera == null) {
                Docking.dock(c, ventana, DockingRegion.SOUTH, proporcion);
                primera = c;
            } else {
                Docking.dock(c, primera, DockingRegion.CENTER);
            }
        }
        if (primera != null) Docking.bringToFront(primera);
    }

    /** Las fases indicadas (las que existan) como pestañas de un mismo grupo junto a {@code ancla}. */
    private void enPestanas(Dockable ancla, DockingRegion region, double proporcion, List<String> ids) {
        PanelAcoplable primera = null;
        for (PanelAcoplable f : acoplablesDe(ids)) {
            if (primera == null) {
                Docking.dock(f, ancla, region, proporcion);
                primera = f;
            } else {
                Docking.dock(f, primera, DockingRegion.CENTER);
            }
        }
        if (primera != null) Docking.bringToFront(primera);
    }

    /** Las fases indicadas, una al lado de la otra a la derecha de {@code ancla}; {@code proporcion} es la de todas juntas. */
    private void columnas(Dockable ancla, List<String> ids, double proporcion) {
        List<PanelAcoplable> lista = acoplablesDe(ids);
        Dockable previa = ancla;
        for (int i = 0; i < lista.size(); i++) {
            // cada columna toma 1/(las que faltan) del espacio que queda
            double parte = i == 0 ? proporcion : 1.0 / (lista.size() - i + 1);
            Docking.dock(lista.get(i), previa, DockingRegion.EAST, parte);
            previa = lista.get(i);
        }
    }

    /** Una ventana nueva con esas fases como pestañas; en la pantalla {@code numeroPantalla} si hay varias. */
    private void ventanaAparte(List<String> ids, int numeroPantalla) {
        List<PanelAcoplable> lista = acoplablesDe(ids);
        if (lista.isEmpty()) return;
        Rectangle area = areaDePantalla(numeroPantalla);
        boolean otraPantalla = numeroPantalla < pantallas().length;
        Point origen = otraPantalla
                ? new Point(area.x + 40, area.y + 40)
                : new Point(area.x + 60 * numeroPantalla, area.y + 60 * numeroPantalla);
        Dimension tam = otraPantalla
                ? new Dimension(Math.max(400, area.width - 80), Math.max(300, area.height - 80))
                : new Dimension(Math.max(500, area.width / 2), Math.max(350, area.height / 2));
        PanelAcoplable primera = lista.get(0);
        Docking.dock(primera, Docking.getMainWindow(), DockingRegion.EAST); // newWindow solo mueve lo que ya está acoplado
        Docking.newWindow(primera, origen, tam);
        for (int i = 1; i < lista.size(); i++) Docking.dock(lista.get(i), primera, DockingRegion.CENTER);
        Docking.bringToFront(primera);
    }

    private List<PanelAcoplable> acoplablesDe(List<String> ids) {
        List<PanelAcoplable> r = new ArrayList<>();
        for (String id : ids) {
            for (int i = 0; i < fases.cantidad(); i++) if (fases.idDe(i).equals(id)) r.add(fases.acoplable(i));
        }
        return r;
    }

    private static GraphicsDevice[] pantallas() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    }

    /** Límites de la pantalla n (0 = la de la ventana principal). Con una sola pantalla, la principal. */
    private static Rectangle areaDePantalla(int n) {
        GraphicsDevice[] ps = pantallas();
        if (n < ps.length) return ps[n].getDefaultConfiguration().getBounds();
        Window w = Docking.getMainWindow();
        return w != null ? w.getBounds() : new Rectangle(0, 0, 1280, 800);
    }
}
