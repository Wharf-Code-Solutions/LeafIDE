package leaf.api;

import java.util.List;

public interface ProveedorCompilador {
    /** (Aditivo, M2) Versión de esta API. Un proveedor con otra {@link #apiVersion()} se rechaza al cargarse. */
    int API_VERSION = 1;

    String nombre(); String version(); int apiVersion();
    List<FaseCompilacion> fases();
    LenguajeDef lenguaje();
}
