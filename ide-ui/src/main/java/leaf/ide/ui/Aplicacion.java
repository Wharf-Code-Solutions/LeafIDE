package leaf.ide.ui;

import com.formdev.flatlaf.FlatLightLaf;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import leaf.api.ProveedorCompilador;
import leaf.ide.core.AlmacenAjustes;

/** Punto de entrada de la UI: lo usa {@code ide-app}. */
public final class Aplicacion {

    private Aplicacion() {}

    public static void iniciar(ProveedorCompilador proveedor) {
        iniciar(proveedor, Map.of());
    }

    /**
     * @param entornoDeProcesos variables de entorno que se añaden a los programas lanzados desde la terminal Sistema
     *                          (por ejemplo CLASSPATH, para poder escribir {@code java generado.Leaf archivo})
     */
    public static void iniciar(ProveedorCompilador proveedor, Map<String, String> entornoDeProcesos) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            new VentanaPrincipal(proveedor, 500, entornoDeProcesos, AlmacenAjustes.carpetaDelUsuario()).setVisible(true);
        });
    }

    /** Mensaje de error antes de que exista la ventana (por ejemplo, si no hay compilador). */
    public static void errorFatal(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            JOptionPane.showMessageDialog(null, mensaje, "LeafIDE", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
    }
}
