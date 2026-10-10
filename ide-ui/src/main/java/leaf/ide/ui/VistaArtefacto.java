package leaf.ide.ui;

import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import leaf.api.Artefacto;
import leaf.api.ArtefactoArbol;
import leaf.api.ArtefactoTabla;
import leaf.api.ArtefactoTexto;
import leaf.api.Rango;

/** Las tres vistas genéricas (tabla, árbol y texto): una fase nueva no necesita código de UI. */
final class VistaArtefacto {

    private VistaArtefacto() {}

    /**
     * @param alElegir se llama con el rango de lo que el usuario elija (una fila, un nodo) para resaltarlo en el editor
     * @return la vista; si implementa {@link Sincronizable} también sigue al cursor del editor
     */
    static JComponent crear(Artefacto artefacto, Consumer<Rango> alElegir) {
        return switch (artefacto) {
            case ArtefactoTabla t -> new VistaTabla(t, alElegir);
            case ArtefactoArbol a -> new VistaArbol(a.raiz(), alElegir);
            case ArtefactoTexto x -> texto(x);
        };
    }

    private static JComponent texto(ArtefactoTexto x) {
        JTextArea area = new JTextArea(x.contenido());
        area.setEditable(false);
        area.setFont(Fuentes.codigo(14));
        area.setCaretPosition(0);
        return new JScrollPane(area);
    }
}
