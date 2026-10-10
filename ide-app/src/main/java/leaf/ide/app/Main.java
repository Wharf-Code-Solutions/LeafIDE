package leaf.ide.app;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import leaf.api.ProveedorCompilador;
import leaf.ide.core.RegistroProveedores;
import leaf.ide.ui.Aplicacion;

/** Arranque de LeafIDE: busca el compilador por {@code ServiceLoader} y abre la ventana. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Optional<ProveedorCompilador> proveedor = RegistroProveedores.cargar();
        if (proveedor.isEmpty()) {
            Aplicacion.errorFatal("No se encontró ningún compilador Leaf compatible (ProveedorCompilador con API "
                    + ProveedorCompilador.API_VERSION + ").");
            return;
        }
        Aplicacion.iniciar(proveedor.get(), entornoParaLaTerminal());
    }

    /**
     * Variables que se añaden a los programas lanzados desde la terminal Sistema. Si el IDE corre desde su jar, el
     * CLASSPATH incluye las bibliotecas de {@code lib/} (entre ellas el compilador): así se puede escribir
     * {@code java generado.Leaf archivo} tal como en el manual. Un CLASSPATH que ya existiera se conserva al final.
     */
    static Map<String, String> entornoParaLaTerminal() {
        try {
            Path jar = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path lib = jar.resolveSibling("lib");
            if (Files.isRegularFile(jar) && Files.isDirectory(lib)) {
                String previo = System.getenv("CLASSPATH");
                String nuevo = lib.toAbsolutePath() + File.separator + "*";
                return Map.of("CLASSPATH", previo == null || previo.isBlank() ? nuevo : nuevo + File.pathSeparator + previo);
            }
        } catch (URISyntaxException | RuntimeException e) {
            // sin ruta del jar no hay nada que añadir
        }
        return Map.of();
    }
}
