package leaf.ide.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.EventoFase;
import leaf.api.FaseCompilacion;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;

/**
 * Ejecuta las fases en orden de dependencias. Nunca lanza excepciones: encapsula todo en {@link Estado#FALLO_INTERNO}.
 */
public final class Pipeline {

    private final ProveedorCompilador proveedor;
    private final EventBus bus;

    public Pipeline(ProveedorCompilador proveedor, EventBus bus) {
        this.proveedor = proveedor;
        this.bus = bus;
    }

    /** Ordena topológicamente usando una búsqueda en profundidad (DFS) básica. */
    static List<FaseCompilacion> ordenarFases(List<FaseCompilacion> todas) {
        List<FaseCompilacion> ordenadas = new ArrayList<>();
        Map<String, FaseCompilacion> mapa = new HashMap<>();
        for (FaseCompilacion f : todas) mapa.put(f.id(), f);
        List<String> visitados = new ArrayList<>();

        for (FaseCompilacion f : todas) visitar(f.id(), mapa, visitados, ordenadas);
        return ordenadas;
    }

    private static void visitar(String id, Map<String, FaseCompilacion> mapa, List<String> visitados, List<FaseCompilacion> ordenadas) {
        if (visitados.contains(id)) return;
        FaseCompilacion f = mapa.get(id);
        if (f != null) {
            for (String dep : f.dependeDe()) visitar(dep, mapa, visitados, ordenadas);
            visitados.add(id);
            ordenadas.add(f);
        }
    }

    public List<ResultadoFase> ejecutar(long idCompilacion, String codigo, OpcionesCompilacion opciones) {
        OpcionesCompilacion opt = opciones != null ? opciones : OpcionesCompilacion.porDefecto();
        List<FaseCompilacion> fases = ordenarFases(proveedor.fases());
        Contexto ctx = new Contexto(idCompilacion, codigo, bus);

        for (FaseCompilacion fase : fases) {
            bus.publicar(new EventoFase.FaseIniciada(idCompilacion, fase.id()));
            long t0 = System.currentTimeMillis();

            ResultadoFase resultado;
            if (!fase.implementada()) {
                resultado = new ResultadoFase(fase.id(), Estado.NO_IMPLEMENTADA, List.of(), List.of(), 0);
            } else if (debeOmitir(fase, ctx, opt)) {
                resultado = new ResultadoFase(fase.id(), Estado.OMITIDA, List.of(), List.of(), 0);
            } else {
                try {
                    resultado = fase.ejecutar(ctx);
                } catch (Throwable t) {
                    Diagnostico d = new Diagnostico(fase.id(), Severidad.ERROR, Rango.DESCONOCIDO,
                            "Fallo interno en fase " + fase.id() + ": " + t.getMessage());
                    resultado = new ResultadoFase(fase.id(), Estado.FALLO_INTERNO, List.of(d), List.of(), 0);
                }
            }

            resultado = resultado.conMilis(System.currentTimeMillis() - t0);
            ctx.resultados.put(fase.id(), resultado);
            bus.publicar(new EventoFase.FaseTerminada(idCompilacion, resultado));

            if (opt.hastaFaseId() != null && opt.hastaFaseId().equals(fase.id())) {
                break;
            }
        }
        return new ArrayList<>(ctx.resultados.values());
    }

    /** 
     * Política de omisión: se omite si una dependencia está CON_ERRORES (salvo si toleraErroresPrevios es true),
     * OMITIDA, NO_IMPLEMENTADA o FALLO_INTERNO.
     */
    private boolean debeOmitir(FaseCompilacion fase, Contexto ctx, OpcionesCompilacion opciones) {
        for (String depId : fase.dependeDe()) {
            ResultadoFase depRes = ctx.resultados.get(depId);
            if (depRes == null) continue; // Dependencia no ejecutada (por ejemplo, cortada antes)
            Estado est = depRes.estado();
            if (est == Estado.OMITIDA || est == Estado.NO_IMPLEMENTADA || est == Estado.FALLO_INTERNO) {
                return true;
            }
            if (est == Estado.CON_ERRORES && !fase.toleraErroresPrevios(opciones)) {
                return true;
            }
        }
        return false;
    }

    private static class Contexto implements ContextoCompilacion {
        private final long idCompilacion;
        private final String codigo;
        private final EventBus bus;
        final Map<String, ResultadoFase> resultados = new java.util.LinkedHashMap<>();
        private final Map<String, Object> internos = new HashMap<>();

        Contexto(long idCompilacion, String codigo, EventBus bus) {
            this.idCompilacion = idCompilacion;
            this.codigo = codigo;
            this.bus = bus;
        }

        @Override public String codigo() { return codigo; }
        
        @Override
        public Optional<ResultadoFase> resultado(String faseId) {
            return Optional.ofNullable(resultados.get(faseId));
        }

        @Override
        public <T> T dato(String clave, Class<T> tipo) {
            Object obj = internos.get(clave);
            return tipo.isInstance(obj) ? tipo.cast(obj) : null;
        }

        @Override
        public void guardar(String clave, Object valor) {
            internos.put(clave, valor);
        }

        @Override
        public void publicar(EventoFase evento) {
            if (evento.idCompilacion() == this.idCompilacion) {
                bus.publicar(evento);
            }
        }
    }
}
