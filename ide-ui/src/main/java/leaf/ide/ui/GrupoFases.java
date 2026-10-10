package leaf.ide.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.Rango;
import leaf.api.ResultadoFase;

/**
 * Un panel acoplable por fase del proveedor. Las no implementadas salen en gris con "próximamente". Al ser paneles
 * sueltos, cada perspectiva decide dónde poner cada fase (en pestañas, en columnas, en otra ventana…).
 */
final class GrupoFases implements Sincronizable {

    private final List<FaseCompilacion> fases;
    private final List<PanelFase> paneles = new ArrayList<>();
    private final List<PanelAcoplable> acoplables = new ArrayList<>();
    private final List<Estado> estados = new ArrayList<>(); // null = todavía sin resultado
    private int activa = -1; // fase que la demostración está mostrando
    private boolean activaEnCurso; // todavía no termina: su título no dice "OK" a medias

    /** @param alElegir qué hacer con el rango de lo que el usuario elija en las vistas (resaltarlo en el editor) */
    GrupoFases(List<FaseCompilacion> fases, Consumer<Rango> alElegir) {
        this.fases = List.copyOf(fases);
        for (FaseCompilacion f : this.fases) {
            PanelFase panel = new PanelFase(alElegir);
            paneles.add(panel);
            estados.add(null);
            acoplables.add(new PanelAcoplable("fase." + f.id(), f.nombre(), panel, true));
        }
        for (int i = 0; i < this.fases.size(); i++) reiniciar(i);
    }

    int cantidad() {
        return fases.size();
    }

    String idDe(int i) {
        return fases.get(i).id();
    }

    /** El panel de la fase con ese id; {@code null} si no existe. */
    PanelAcoplable acoplablePorId(String faseId) {
        for (int i = 0; i < fases.size(); i++) if (fases.get(i).id().equals(faseId)) return acoplables.get(i);
        return null;
    }

    PanelAcoplable acoplable(int i) {
        return acoplables.get(i);
    }

    /** El contenido de la fase (para las pruebas). */
    PanelFase panel(int i) {
        return paneles.get(i);
    }

    String titulo(int i) {
        return acoplables.get(i).titulo();
    }

    /** El cursor del editor está en (línea, columna): todas las vistas de todas las fases lo siguen. */
    @Override
    public void cursorEn(int linea, int columna) {
        for (PanelFase p : paneles) p.cursorEn(linea, columna);
    }

    /** Vuelve todos los paneles al estado previo a compilar. */
    void limpiar() {
        for (int i = 0; i < fases.size(); i++) reiniciar(i);
    }

    void mostrar(List<ResultadoFase> resultados) {
        mostrar(resultados, false);
    }

    /** Como {@link #mostrar(List)}, pero las fases que aún no les toca dicen que esperan su turno. */
    void mostrarDemostracion(List<ResultadoFase> resultados) {
        mostrar(resultados, true);
    }

    /**
     * Marca (con ▶ en su título) la fase que la demostración está mostrando; {@code null} la quita.
     *
     * @param enCurso la fase aún no termina: se muestra sin estado hasta que cierre
     */
    void marcarActiva(String faseId, boolean enCurso) {
        activa = -1;
        activaEnCurso = enCurso;
        for (int i = 0; i < fases.size(); i++) if (fases.get(i).id().equals(faseId)) activa = i;
        for (int i = 0; i < fases.size(); i++) actualizarTitulo(i);
    }

    private void mostrar(List<ResultadoFase> resultados, boolean demostracion) {
        Map<String, ResultadoFase> porId = new HashMap<>();
        for (ResultadoFase r : resultados) porId.put(r.faseId(), r);
        for (int i = 0; i < fases.size(); i++) {
            ResultadoFase r = porId.get(fases.get(i).id());
            if (r == null && demostracion && fases.get(i).implementada()) {
                estados.set(i, null);
                paneles.get(i).enEspera();
                actualizarTitulo(i);
            } else if (r == null) {
                reiniciar(i);
            } else {
                estados.set(i, r.estado());
                paneles.get(i).mostrar(r);
                actualizarTitulo(i);
            }
        }
    }

    private void reiniciar(int i) {
        boolean implementada = fases.get(i).implementada();
        estados.set(i, implementada ? null : Estado.NO_IMPLEMENTADA);
        paneles.get(i).inicial(implementada);
        actualizarTitulo(i);
    }

    private void actualizarTitulo(int i) {
        Estado e = estados.get(i);
        acoplables.get(i).titulo((i == activa ? "▶ " : "") + fases.get(i).nombre() + (e == null || (i == activa && activaEnCurso) ? "" : " · " + ResumenConsola.etiqueta(e)));
    }
}
