package leaf.ide.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import leaf.api.ContextoCompilacion;
import leaf.api.FaseCompilacion;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.ResultadoFase;
import org.junit.jupiter.api.Test;

class EjecutorCompilacionTest {

    @Test
    void descartaResultadosViejos() throws InterruptedException {
        // Simulamos un pipeline que tarda un poco
        ProveedorCompilador prov = new ProveedorCompilador() {
            @Override public int apiVersion() { return 1; }
            @Override public String nombre() { return "Lento"; }
            @Override public String version() { return "1.0"; }
            @Override public leaf.api.LenguajeDef lenguaje() { return null; }
            @Override public List<FaseCompilacion> fases() {
                return List.of(new FaseCompilacion() {
                    @Override public String id() { return "lenta"; }
                    @Override public String nombre() { return "lenta"; }
                    @Override public List<String> dependeDe() { return List.of(); }
                    @Override public boolean implementada() { return true; }
                    @Override public boolean toleraErroresPrevios(OpcionesCompilacion opciones) { return false; }
                    @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) {
                        try { Thread.sleep(50); } catch (InterruptedException e) {}
                        return new ResultadoFase("lenta", leaf.api.Estado.OK, List.of(), List.of(), 0);
                    }
                });
            }
        };

        Pipeline pipeline = new Pipeline(prov, new EventBus());
        EjecutorCompilacion ejecutor = new EjecutorCompilacion(pipeline);

        AtomicBoolean callback1Llamado = new AtomicBoolean(false);
        AtomicBoolean callback2Llamado = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        // Pedimos compilación 1
        ejecutor.solicitar("A", null, res -> {
            callback1Llamado.set(true);
        });

        // Inmediatamente pedimos compilación 2
        ejecutor.solicitar("B", null, res -> {
            callback2Llamado.set(true);
            latch.countDown();
        });

        latch.await(2, TimeUnit.SECONDS);

        // La compilación 1 fue ignorada porque se solicitó la 2 antes de que terminara
        assertFalse(callback1Llamado.get());
        assertTrue(callback2Llamado.get());
        
        ejecutor.apagar();
    }
}
