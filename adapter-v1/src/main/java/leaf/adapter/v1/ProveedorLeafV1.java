package leaf.adapter.v1;

import java.util.List;
import leaf.api.FaseCompilacion;
import leaf.api.LenguajeDef;
import leaf.api.ProveedorCompilador;

/**
 * Proveedor del compilador Leaf v1 (registrado en META-INF/services para {@link java.util.ServiceLoader}).
 * Es el único punto del IDE que conoce {@code generado.*}.
 */
public final class ProveedorLeafV1 implements ProveedorCompilador {

    /** Recurso (dentro del jar del compilador) con el commit completo del repositorio Leaf que se importó. */
    static final String RECURSO_VERSION = "/VERSION-IMPORTADA.txt";
    static final String VERSION_DESCONOCIDA = "sin versión";

    public static final String FASE_INTERMEDIO = "intermedio";
    public static final String FASE_OPTIMIZACION = "optimizacion";
    public static final String FASE_OBJETO = "objeto";

    private final List<FaseCompilacion> fases = List.of(
            new FaseLexica(),
            new FaseSintactica(),
            new FaseSemantica(),
            new FaseIntermedio(),
            new FaseNoImplementada(FASE_OPTIMIZACION, "Optimización", List.of(FASE_INTERMEDIO)),
            new FaseNoImplementada(FASE_OBJETO, "Código objeto", List.of(FASE_OPTIMIZACION)));

    private final LenguajeDef lenguaje = LenguajeLeaf.cargar();

    @Override public String nombre() { return "Leaf"; }
    /** Los 7 primeros caracteres del commit importado (lo anota el script actualizar-compilador.bat). */
    @Override public String version() { return versionDelCompilador(); }

    static String versionDelCompilador() {
        try (java.io.InputStream in = ProveedorLeafV1.class.getResourceAsStream(RECURSO_VERSION)) {
            if (in == null) return VERSION_DESCONOCIDA;
            String commit = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).strip();
            return commit.length() >= 7 ? commit.substring(0, 7) : VERSION_DESCONOCIDA;
        } catch (java.io.IOException e) {
            return VERSION_DESCONOCIDA;
        }
    }
    @Override public int apiVersion() { return ProveedorCompilador.API_VERSION; }
    @Override public List<FaseCompilacion> fases() { return fases; }
    @Override public LenguajeDef lenguaje() { return lenguaje; }
}
