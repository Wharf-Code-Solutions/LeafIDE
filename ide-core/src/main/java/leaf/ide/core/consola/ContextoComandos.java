package leaf.ide.core.consola;

import java.nio.file.Path;
import java.util.List;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.ResultadoFase;

/**
 * Lo que el IDE ofrece a los comandos de Leaf. Lo implementa la ventana; el intérprete no conoce Swing. Los métodos se
 * llaman fuera del hilo de la ventana y pueden bloquearse hasta que el IDE termine (por ejemplo, tras un diálogo).
 * Si el IDE no puede cumplir una petición lanza {@link ErrorDeComando} con el motivo.
 */
public interface ContextoComandos {

    ProveedorCompilador proveedor();

    /**
     * Compila el texto que hay en el editor. Con {@code actualizarVistas} deja las pestañas de fase, Problemas y el
     * subrayado como si se hubiera pulsado Compilar; sin él, solo devuelve los resultados.
     */
    List<ResultadoFase> compilarEditor(OpcionesCompilacion opciones, boolean actualizarVistas);

    /** Documento en blanco (pregunta por los cambios sin guardar). */
    void nuevo();

    void abrir(Path ruta);

    /** Guarda el documento; con {@code ruta == null}, en su archivo actual. @return dónde quedó guardado */
    Path guardar(Path ruta);

    void cambiarTema(boolean oscuro);

    /** Ids de las perspectivas disponibles (minúsculas y sin acentos), por ejemplo {@code escritura} o {@code evaluacion}. */
    List<String> perspectivas();

    /** Coloca los paneles según la perspectiva {@code id} (uno de {@link #perspectivas()}). */
    void cambiarPerspectiva(String id);

    /** Borra la pantalla de la consola Leaf. */
    void limpiarPantalla();
}
