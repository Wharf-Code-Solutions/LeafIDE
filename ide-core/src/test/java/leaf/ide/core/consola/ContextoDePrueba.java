package leaf.ide.core.consola;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.ResultadoFase;
import leaf.ide.core.EventBus;
import leaf.ide.core.Pipeline;

/** El IDE falso: guarda lo que le pidieron los comandos y compila el "editor" con el compilador de prueba. */
final class ContextoDePrueba implements ContextoComandos {

    final ProveedorCompilador proveedor = ProveedorDePrueba.crear();
    final List<String> llamadas = new ArrayList<>();
    String codigoDelEditor = "definir $x";
    OpcionesCompilacion ultimasOpciones;
    boolean ultimoActualizar;
    RuntimeException fallar; // si no es null, el siguiente nuevo/abrir/guardar lo lanza

    @Override public ProveedorCompilador proveedor() { return proveedor; }

    @Override
    public List<ResultadoFase> compilarEditor(OpcionesCompilacion opciones, boolean actualizarVistas) {
        llamadas.add("compilarEditor");
        ultimasOpciones = opciones;
        ultimoActualizar = actualizarVistas;
        return new Pipeline(proveedor, new EventBus()).ejecutar(1, codigoDelEditor, opciones);
    }

    @Override public void nuevo() { comprobar(); llamadas.add("nuevo"); }
    @Override public void abrir(Path ruta) { comprobar(); llamadas.add("abrir:" + ruta); }

    @Override
    public Path guardar(Path ruta) {
        comprobar();
        if (ruta == null) throw new ErrorDeComando("Indica una ruta: guardar <ruta>");
        llamadas.add("guardar:" + ruta);
        return ruta;
    }

    @Override public void cambiarTema(boolean oscuro) { llamadas.add("tema:" + (oscuro ? "oscuro" : "claro")); }
    @Override public List<String> perspectivas() { return List.of("escritura", "analisis"); }
    @Override public void cambiarPerspectiva(String id) { llamadas.add("perspectiva:" + id); }
    @Override public void limpiarPantalla() { llamadas.add("limpiar"); }

    private void comprobar() {
        if (fallar != null) throw fallar;
    }
}
