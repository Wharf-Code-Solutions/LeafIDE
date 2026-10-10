package leaf.ide.core;

import java.util.Optional;
import java.util.ServiceLoader;
import leaf.api.ProveedorCompilador;

/** Carga los proveedores de compilación vía {@link ServiceLoader}. */
public final class RegistroProveedores {

    private RegistroProveedores() {}

    /** @return el primer proveedor compatible con la API, o vacío si no hay ninguno. */
    public static Optional<ProveedorCompilador> cargar() {
        for (ProveedorCompilador p : ServiceLoader.load(ProveedorCompilador.class)) {
            if (p.apiVersion() == ProveedorCompilador.API_VERSION) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
