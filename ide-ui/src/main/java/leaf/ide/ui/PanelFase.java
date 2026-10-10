package leaf.ide.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import leaf.api.Artefacto;
import leaf.api.Rango;
import leaf.api.ResultadoFase;

/** Contenido de la pestaña de una fase: sus artefactos, o un aviso si no hay nada que mostrar. */
final class PanelFase extends JPanel implements Sincronizable {

    private final Consumer<Rango> alElegir;
    private final List<Sincronizable> sincronizables = new ArrayList<>();

    /** @param alElegir qué hacer con el rango de lo que el usuario elija en las vistas (resaltarlo en el editor) */
    PanelFase(Consumer<Rango> alElegir) {
        super(new BorderLayout());
        this.alElegir = alElegir;
    }

    /** Estado antes de compilar (o cuando la compilación se cortó antes de llegar a esta fase). */
    void inicial(boolean implementada) {
        aviso(implementada
                ? "Pulsa Compilar para ver esta fase."
                : "Próximamente: esta fase aún no está implementada en el compilador.");
    }

    /** Modo demostración: la fase todavía no tiene su turno. */
    void enEspera() {
        aviso("Esperando su turno en la demostración.");
    }

    void mostrar(ResultadoFase r) {
        String mensaje = switch (r.estado()) {
            case NO_IMPLEMENTADA -> "Próximamente: esta fase aún no está implementada en el compilador.";
            case OMITIDA -> "Fase omitida: una fase anterior terminó con errores o no se ejecutó.";
            case FALLO_INTERNO -> "Fallo interno: " + (r.diagnosticos().isEmpty() ? "(sin detalle)" : r.diagnosticos().get(0).mensaje());
            default -> r.artefactos().isEmpty() ? "Esta fase no produjo artefactos." : null;
        };
        if (r.artefactos().isEmpty()) {
            aviso(mensaje);
            return;
        }
        JTabbedPane anterior = componenteArtefactos();
        int seleccionada = anterior != null ? anterior.getSelectedIndex() : 0;
        Map<String, VistaTabla.Estado> reponer = estadosDeLasTablas(anterior);

        removeAll();
        sincronizables.clear();
        if (mensaje != null) add(etiqueta(mensaje), BorderLayout.NORTH);
        JTabbedPane artefactos = new JTabbedPane();
        for (Artefacto a : r.artefactos()) {
            JComponent vista = VistaArtefacto.crear(a, alElegir);
            if (vista instanceof Sincronizable s) sincronizables.add(s);
            if (vista instanceof VistaTabla t && reponer.containsKey(a.titulo())) t.restaurar(reponer.get(a.titulo()));
            artefactos.addTab(a.titulo(), vista);
        }
        if (seleccionada >= 0 && seleccionada < artefactos.getTabCount()) artefactos.setSelectedIndex(seleccionada);
        add(artefactos, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /** Los demás componentes de la vista siguen al cursor del editor. */
    @Override
    public void cursorEn(int linea, int columna) {
        for (Sincronizable s : sincronizables) s.cursorEn(linea, columna);
    }

    /** El filtro y la agrupación que el usuario dejó puestos, por título de tabla (para no perderlos al reanalizar). */
    private static Map<String, VistaTabla.Estado> estadosDeLasTablas(JTabbedPane artefactos) {
        Map<String, VistaTabla.Estado> estados = new HashMap<>();
        if (artefactos == null) return estados;
        for (int i = 0; i < artefactos.getTabCount(); i++) {
            Component c = artefactos.getComponentAt(i);
            if (c instanceof VistaTabla t) estados.put(artefactos.getTitleAt(i), t.estado());
        }
        return estados;
    }

    private JTabbedPane componenteArtefactos() {
        return getComponentCount() > 0 && getComponent(getComponentCount() - 1) instanceof JTabbedPane t ? t : null;
    }

    private void aviso(String texto) {
        removeAll();
        sincronizables.clear();
        JLabel l = etiqueta(texto);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        add(l, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return l;
    }
}
