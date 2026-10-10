package leaf.ide.core.demo;

import java.util.ArrayList;
import java.util.List;
import leaf.api.Artefacto;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;
import leaf.ide.core.NombresFase;

/**
 * Modo demostración: "reproduce" paso a paso una compilación ya hecha. Cada fase ejecutada tiene un paso de inicio, un
 * paso por cada elemento de su artefacto principal (fila, nodo o línea) y un paso de cierre con el resumen; las fases no
 * implementadas u omitidas no tienen pasos. No usa Swing: la ventana pide {@link #vista()} y la muestra.
 */
public final class Demostracion {

    /**
     * @param resultado índice de la fase dentro de la lista de resultados
     * @param mostrados cuántos elementos del artefacto principal se ven (0 en el inicio)
     * @param total     cuántos tiene en total
     * @param texto     narración con datos reales
     * @param rango     tramo del código del último elemento mostrado (desconocido en inicio y cierre)
     * @param cierre    el último paso de la fase: ya se ve completa
     */
    public record Paso(int resultado, String faseId, int mostrados, int total, String texto, Rango rango, boolean cierre) {}

    private final List<ResultadoFase> resultados;
    private final List<Paso> pasos = new ArrayList<>();
    private int posicion;

    public Demostracion(List<ResultadoFase> resultados) {
        this.resultados = List.copyOf(resultados);
        for (int i = 0; i < this.resultados.size(); i++) agregarPasos(i);
    }

    // ---------- estado ----------

    /** La compilación que se está reproduciendo. */
    public List<ResultadoFase> resultados() {
        return resultados;
    }

    public int cantidad() {
        return pasos.size();
    }

    /** Posición del paso actual (desde 0). */
    public int posicion() {
        return posicion;
    }

    /** El paso en el que está la demostración; {@code null} si no hay ninguno. */
    public Paso actual() {
        return pasos.isEmpty() ? null : pasos.get(posicion);
    }

    public boolean alPrincipio() {
        return posicion == 0;
    }

    public boolean alFinal() {
        return pasos.isEmpty() || posicion == pasos.size() - 1;
    }

    // ---------- movimiento ----------

    /** Un elemento más (o el inicio o cierre de la fase). @return {@code false} si ya estaba al final */
    public boolean avanzar() {
        if (alFinal()) return false;
        posicion++;
        return true;
    }

    /**
     * Completa la fase actual y pasa al inicio de la siguiente. Si no hay otra, deja la demostración en el cierre de la
     * última. @return {@code false} si ya estaba al final
     */
    public boolean siguienteFase() {
        if (alFinal()) return false;
        int fase = pasos.get(posicion).resultado();
        for (int i = posicion + 1; i < pasos.size(); i++) {
            if (pasos.get(i).resultado() != fase) {
                posicion = i;
                return true;
            }
        }
        posicion = pasos.size() - 1;
        return true;
    }

    public void reiniciar() {
        posicion = 0;
    }

    // ---------- lo que se ve ----------

    /**
     * Los resultados que deben mostrarse ahora: las fases anteriores completas, la actual a medias (solo su artefacto
     * principal hasta que cierra) y, al final, también las que no se ejecutaron (omitidas o no implementadas).
     */
    public List<ResultadoFase> vista() {
        Paso p = actual();
        if (p == null) return List.of();
        List<ResultadoFase> vista = new ArrayList<>();
        for (int i = 0; i < resultados.size(); i++) {
            ResultadoFase r = resultados.get(i);
            if (i < p.resultado()) {
                vista.add(r);
            } else if (i == p.resultado()) {
                if (p.cierre()) vista.add(r);
                else if (p.mostrados() > 0) vista.add(parcial(r, p.mostrados()));
            } else if (alFinal() && !ejecutada(r)) {
                vista.add(r);
            }
        }
        return vista;
    }

    private static ResultadoFase parcial(ResultadoFase r, int mostrados) {
        int principal = indicePrincipal(r);
        List<Artefacto> solo = principal < 0 ? List.of() : List.of(Recorte.primeros(r.artefactos().get(principal), mostrados));
        return new ResultadoFase(r.faseId(), r.estado(), List.of(), solo, r.milis());
    }

    // ---------- construcción de pasos ----------

    private static boolean ejecutada(ResultadoFase r) {
        return r.estado() != Estado.NO_IMPLEMENTADA && r.estado() != Estado.OMITIDA;
    }

    /** El artefacto que "se llena" durante la fase: el primero que tenga elementos; -1 si ninguno. */
    private static int indicePrincipal(ResultadoFase r) {
        for (int i = 0; i < r.artefactos().size(); i++) if (Recorte.unidades(r.artefactos().get(i)) > 0) return i;
        return -1;
    }

    private void agregarPasos(int i) {
        ResultadoFase r = resultados.get(i);
        if (!ejecutada(r)) return;
        String nombre = NombresFase.de(r.faseId());
        int principal = indicePrincipal(r);
        Artefacto a = principal < 0 ? null : r.artefactos().get(principal);
        int total = a == null ? 0 : Recorte.unidades(a);

        pasos.add(new Paso(i, r.faseId(), 0, total, nombre + ": comienza el análisis"
                + (a == null ? "." : " (" + a.titulo() + ": " + total + ")."), Rango.DESCONOCIDO, false));
        for (int k = 1; k <= total; k++) {
            String grupo = Recorte.grupoDe(a, k);
            String texto = nombre + ": " + k + " de " + total + " → " + recortar(Recorte.celdasDe(a, k))
                    + (grupo == null || grupo.isBlank() ? "" : "  [" + grupo + "]");
            pasos.add(new Paso(i, r.faseId(), k, total, texto, Recorte.rangoDe(a, k), false));
        }
        pasos.add(new Paso(i, r.faseId(), total, total, cierre(i, r, nombre, a, total), Rango.DESCONOCIDO, true));
    }

    private String cierre(int indice, ResultadoFase r, String nombre, Artefacto a, int total) {
        StringBuilder s = new StringBuilder(nombre).append(" terminó");
        if (a != null) s.append(" (").append(a.titulo()).append(": ").append(total).append(')');
        List<Diagnostico> errores = r.diagnosticos().stream().filter(d -> d.severidad() == Severidad.ERROR).toList();
        if (r.estado() == Estado.FALLO_INTERNO) {
            s.append(" con un fallo interno");
        } else if (errores.isEmpty()) {
            s.append(" sin errores");
        } else {
            s.append(" con ").append(errores.size()).append(errores.size() == 1 ? " error: " : " errores; el primero: ")
                    .append(errores.get(0).mensaje());
        }
        s.append('.');
        if (resultados.subList(indice + 1, resultados.size()).stream().anyMatch(x -> x.estado() == Estado.OMITIDA)) {
            s.append(" Las fases siguientes se omiten.");
        } else {
            for (int j = indice + 1; j < resultados.size(); j++) {
                if (ejecutada(resultados.get(j))) {
                    s.append(" Siguiente: ").append(NombresFase.de(resultados.get(j).faseId())).append('.');
                    break;
                }
            }
        }
        return s.toString();
    }

    private static String recortar(String texto) {
        String limpio = texto.replace('\n', ' ');
        return limpio.length() > 100 ? limpio.substring(0, 99) + "…" : limpio;
    }
}
