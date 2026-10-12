package leaf.ide.ui;

import static leaf.ide.ui.PerspectivasTest.abrir;
import static leaf.ide.ui.PerspectivasTest.acoplada;
import static leaf.ide.ui.PerspectivasTest.cerrar;
import static leaf.ide.ui.PerspectivasTest.enEdt;
import static leaf.ide.ui.PerspectivasTest.enPrincipal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.andrewauclair.moderndocking.app.Docking;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Criterio de M7: «Evaluación reproduce la disposición de 3 pantallas y se restaura al reiniciar». Va en su propia clase
 * porque la ventana que guarda debe ser la primera de su JVM (ver el pom de ide-ui).
 */
class PersistenciaDisposicionTest {

    @Test
    void evaluacionUsaTresVentanasYSeRestauraAlReiniciar(@TempDir Path datos) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = abrir(datos);
        try {
            SwingUtilities.invokeAndWait(() -> v.aplicarPerspectiva(Perspectiva.EVALUACION));
            assertEquals(3, enEdt(() -> Docking.getRootPanels().size()), "principal + optimización + objeto");
            assertTrue(enEdt(() -> enPrincipal(v, "lexico")));
            assertTrue(enEdt(() -> enPrincipal(v, "intermedio")), "el código intermedio va con las fases de la pantalla 1");
            assertFalse(enEdt(() -> enPrincipal(v, "optimizacion")));
            assertTrue(enEdt(() -> acoplada(v, "optimizacion") && acoplada(v, "objeto")));
        } finally {
            cerrar(v); // guarda la disposición y los ajustes
        }
        assertTrue(Files.isRegularFile(datos.resolve("layouts").resolve("ultima.xml")));
        assertTrue(Files.isRegularFile(datos.resolve("ajustes.properties")));

        VentanaPrincipal otra = abrir(datos); // "reiniciar"
        try {
            assertEquals(3, enEdt(() -> Docking.getRootPanels().size()), "las 3 ventanas vuelven solas");
            assertTrue(enEdt(() -> enPrincipal(otra, "lexico") && otra.dockLeaf.enVentanaPrincipal()));
            assertFalse(enEdt(() -> enPrincipal(otra, "objeto")));
            assertTrue(enEdt(() -> acoplada(otra, "objeto")));
        } finally {
            cerrar(otra);
        }
    }
}
