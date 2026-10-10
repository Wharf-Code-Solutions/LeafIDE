package leaf.ide.core;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import leaf.api.OpcionesCompilacion;
import leaf.api.ResultadoFase;

/**
 * Ejecutor serial (un solo hilo, no bloquea la UI). Genera un {@code idCompilacion} creciente
 * y descarta los resultados si al terminar la compilación ya se había pedido una más nueva.
 */
public final class EjecutorCompilacion {

    private final Pipeline pipeline;
    private final ExecutorService hiloCompilador;
    private final AtomicLong ultimaIdPedido = new AtomicLong(0);

    public EjecutorCompilacion(Pipeline pipeline) {
        this.pipeline = pipeline;
        this.hiloCompilador = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "HiloCompiladorLeaf");
            t.setDaemon(true);
            return t;
        });
    }

    public void solicitar(String codigo, OpcionesCompilacion opciones, Consumer<List<ResultadoFase>> callbackExitoso) {
        long idCompilacion = ultimaIdPedido.incrementAndGet();
        
        hiloCompilador.execute(() -> {
            List<ResultadoFase> resultados = pipeline.ejecutar(idCompilacion, codigo, opciones);
            
            // Si el ID pedido sigue siendo el nuestro, somos la versión más reciente.
            // Si es mayor, el usuario escribió más y ya hay otra compilación encolada.
            if (idCompilacion == ultimaIdPedido.get()) {
                callbackExitoso.accept(resultados);
            }
        });
    }

    public void apagar() {
        hiloCompilador.shutdownNow();
    }
}
