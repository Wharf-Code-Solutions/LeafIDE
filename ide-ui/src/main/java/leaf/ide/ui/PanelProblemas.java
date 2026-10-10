package leaf.ide.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import leaf.api.Diagnostico;

/** Tabla de diagnósticos de todas las fases. Doble clic en uno con posición pide ir a esa posición. */
final class PanelProblemas extends JScrollPane {

    private final JTable tabla = new JTable(ModelosTabla.deProblemas(List.of()));
    private List<Diagnostico> actuales = List.of();
    private Consumer<Diagnostico> alElegir = d -> {};

    PanelProblemas() {
        setViewportView(tabla);
        tabla.setFillsViewportHeight(true);
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                int fila = tabla.rowAtPoint(e.getPoint());
                if (fila < 0 || fila >= actuales.size()) return;
                Diagnostico d = actuales.get(fila);
                if (!d.rango().esDesconocido()) alElegir.accept(d);
            }
        });
    }

    int cantidad() {
        return actuales.size();
    }

    void alElegir(Consumer<Diagnostico> accion) {
        this.alElegir = accion;
    }

    void mostrar(List<Diagnostico> diagnosticos) {
        actuales = List.copyOf(diagnosticos);
        tabla.setModel(ModelosTabla.deProblemas(actuales));
        tabla.getColumnModel().getColumn(0).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(50);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(50);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(700);
    }
}
