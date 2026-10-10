package leaf.ide.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import leaf.api.Estado;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.ResultadoFase;
import leaf.ide.core.EjecutorCompilacion;
import leaf.ide.core.EventBus;
import leaf.ide.core.Pipeline;
import leaf.ide.core.RegistroProveedores;
import org.junit.jupiter.api.Test;

class IntegracionPipelineTest {

    @Test
    void compilacionExitosaCompleta() throws InterruptedException {
        Optional<ProveedorCompilador> provOpt = RegistroProveedores.cargar();
        assertTrue(provOpt.isPresent(), "Debe cargar el ProveedorLeafV1 via ServiceLoader");

        ProveedorCompilador proveedor = provOpt.get();
        assertEquals("Leaf", proveedor.nombre());
        assertEquals("1.0", proveedor.version());

        EventBus bus = new EventBus();
        Pipeline pipeline = new Pipeline(proveedor, bus);
        EjecutorCompilacion ejecutor = new EjecutorCompilacion(pipeline);

        String codigoValido = "programa prueba \n definir entero x; \n finprograma";
        
        CountDownLatch latch = new CountDownLatch(1);
        List<ResultadoFase> finales = new ArrayList<>();
        
        ejecutor.solicitar(codigoValido, null, res -> {
            finales.addAll(res);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // lex, sin, sem
        assertEquals(6, finales.size()); // léxico, sintáctico, semántico, intermedio, optimizacion, objeto
        // El tamaño exacto puede variar si se implementan más fases, pero al menos lex, sin, sem.
        
        ResultadoFase sem = finales.stream().filter(f -> f.faseId().equals("semantico")).findFirst().get();
        assertEquals(Estado.OK, sem.estado());
        assertTrue(sem.diagnosticos().isEmpty());
        
        ejecutor.apagar();
    }
    
    @Test
    void compilacionConErrorSintacticoCortaElPipeline() throws InterruptedException {
        ProveedorCompilador proveedor = RegistroProveedores.cargar().get();
        Pipeline pipeline = new Pipeline(proveedor, new EventBus());
        EjecutorCompilacion ejecutor = new EjecutorCompilacion(pipeline);

        String codigoInvalido = "programa prueba \n defini x; \n finprograma"; // Error: 'defini' en vez de 'definir'
        
        CountDownLatch latch = new CountDownLatch(1);
        List<ResultadoFase> finales = new ArrayList<>();
        
        // Opciones nulas -> porDefecto -> ejecutarSemanticoConErroresSintacticos = true
        // Para que se corte, forzamos false
        OpcionesCompilacion opciones = new OpcionesCompilacion(null, false);
        
        ejecutor.solicitar(codigoInvalido, opciones, res -> {
            finales.addAll(res);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        ResultadoFase sin = finales.stream().filter(f -> f.faseId().equals("sintactico")).findFirst().get();
        assertEquals(Estado.CON_ERRORES, sin.estado());
        assertFalse(sin.diagnosticos().isEmpty());

        ResultadoFase sem = finales.stream().filter(f -> f.faseId().equals("semantico")).findFirst().get();
        assertEquals(Estado.OMITIDA, sem.estado());
        
        ejecutor.apagar();
    }
}
