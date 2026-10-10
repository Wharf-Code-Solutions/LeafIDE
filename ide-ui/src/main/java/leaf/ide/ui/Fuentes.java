package leaf.ide.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Set;

/** Fuente de código: Consolas si existe (Windows), y si no la monoespaciada genérica. */
final class Fuentes {

    private static final String FAMILIA = elegirFamilia();

    private Fuentes() {}

    static Font codigo(int tamano) {
        return new Font(FAMILIA, Font.PLAIN, tamano);
    }

    private static String elegirFamilia() {
        try {
            Set<String> familias = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            if (familias.contains("Consolas")) return "Consolas";
        } catch (RuntimeException e) {
            // Sin entorno gráfico se usa la genérica.
        }
        return Font.MONOSPACED;
    }
}
