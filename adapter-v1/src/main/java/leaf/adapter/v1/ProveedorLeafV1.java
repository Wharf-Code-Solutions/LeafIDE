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

    /** Versión del compilador envuelto (etiqueta git {@code compilador-base}: léxico, sintáctico y semántico). */
    public static final String VERSION_COMPILADOR = "1.0";

    public static final String FASE_INTERMEDIO = "intermedio";
    public static final String FASE_OPTIMIZACION = "optimizacion";
    public static final String FASE_OBJETO = "objeto";

    private final List<FaseCompilacion> fases = List.of(
            new FaseLexica(),
            new FaseSintactica(),
            new FaseSemantica(),
            new FaseNoImplementada(FASE_INTERMEDIO, "Código intermedio (cuádruplos)",
                    List.of(ParserDiagnosticos.FASE_SEMANTICA)),
            new FaseNoImplementada(FASE_OPTIMIZACION, "Optimización", List.of(FASE_INTERMEDIO)),
            new FaseNoImplementada(FASE_OBJETO, "Código objeto", List.of(FASE_OPTIMIZACION)));

    private final LenguajeDef lenguaje = LenguajeLeaf.cargar();

    @Override public String nombre() { return "Leaf"; }
    @Override public String version() { return VERSION_COMPILADOR; }
    @Override public int apiVersion() { return ProveedorCompilador.API_VERSION; }
    @Override public List<FaseCompilacion> fases() { return fases; }
    @Override public LenguajeDef lenguaje() { return lenguaje; }
}
