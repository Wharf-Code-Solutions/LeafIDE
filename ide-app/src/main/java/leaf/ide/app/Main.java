package leaf.ide.app;

import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import leaf.api.ProveedorCompilador;
import leaf.ide.core.Ajustes;
import leaf.ide.core.AlmacenAjustes;
import leaf.ide.core.RegistroProveedores;
import leaf.ide.ui.Aplicacion;

/** Arranque de LeafIDE: busca el compilador (el de la carpeta elegida o la copia del IDE) y abre la ventana. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        Optional<ProveedorCompilador> proveedor = RegistroProveedores.cargar();
        if (proveedor.isEmpty()) {
            Aplicacion.errorFatal("No se encontró ningún compilador Leaf compatible (ProveedorCompilador con API "
                    + ProveedorCompilador.API_VERSION + ").");
            return;
        }
        // Por defecto se usa el compilador base de C:\Leaf (sus .class); la copia del IDE queda de respaldo.
        AlmacenAjustes almacen = new AlmacenAjustes(AlmacenAjustes.carpetaDelUsuario());
        Ajustes ajustes = almacen.leer();
        boolean deCarpeta = compiladorDeCarpetaAlArrancar(ajustes, almacen.hayAjustesGuardados());
        ProveedorConmutable conmutable = new ProveedorConmutable(proveedor.get(), deCarpeta, ajustes.rutaCompilador());
        Aplicacion.iniciar(conmutable, entornoParaLaTerminal());
    }

    /**
     * Si el IDE arranca con el compilador de la carpeta o con su copia. Solo la primera vez en un equipo (sin ajustes
     * guardados) y si la carpeta del compilador no existe (por ejemplo, alguien que descargó el IDE y no tiene
     * {@code C:\Leaf}), arranca con la copia del IDE para que funcione de inmediato. Una vez guardada la elección, se
     * respeta siempre: el IDE nunca cambia de compilador por su cuenta mientras se usa.
     */
    static boolean compiladorDeCarpetaAlArrancar(Ajustes ajustes, boolean hayAjustesGuardados) {
        if (hayAjustesGuardados) return ajustes.compiladorDeCarpeta();
        return Files.isDirectory(ajustes.rutaCompilador());
    }

    /**
     * Variables que se añaden a los programas lanzados desde la terminal Sistema: ninguna. La terminal se comporta
     * como un {@code cmd} normal, así que {@code java generado.Leaf archivo} ejecuta las clases del compilador que haya
     * en la carpeta actual (por ejemplo {@code C:\Leaf\src}) y no la copia que el IDE lleva dentro: si faltan
     * archivos del compilador, el error se ve igual que fuera del IDE (decisión 38).
     */
    static Map<String, String> entornoParaLaTerminal() {
        return Map.of();
    }
}
