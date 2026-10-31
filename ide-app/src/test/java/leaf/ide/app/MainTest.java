package leaf.ide.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import leaf.ide.core.Ajustes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @Test
    void laPrimeraVezSinLaCarpetaDelCompiladorArrancaConLaCopiaDelIde(@TempDir Path dir) {
        Ajustes sinCarpeta = Ajustes.porDefecto().conCompilador(true, dir.resolve("no-existe"));
        assertFalse(Main.compiladorDeCarpetaAlArrancar(sinCarpeta, false));

        Ajustes conCarpeta = Ajustes.porDefecto().conCompilador(true, dir);
        assertTrue(Main.compiladorDeCarpetaAlArrancar(conCarpeta, false), "si la carpeta existe, se usa");
    }

    @Test
    void conAjustesGuardadosSeRespetaLaEleccionAunqueFalteLaCarpeta(@TempDir Path dir) {
        Ajustes eligioCarpeta = Ajustes.porDefecto().conCompilador(true, dir.resolve("borrada"));
        assertTrue(Main.compiladorDeCarpetaAlArrancar(eligioCarpeta, true), "se respeta: el IDE avisará del error");

        Ajustes eligioCopia = Ajustes.porDefecto().conCompilador(false, dir);
        assertFalse(Main.compiladorDeCarpetaAlArrancar(eligioCopia, true));
    }

    @Test
    void laTerminalNoRecibeElClasspathDelIde() {
        // Como un cmd normal: java generado.Leaf usa las clases de la carpeta actual, no las del IDE.
        Map<String, String> entorno = Main.entornoParaLaTerminal();
        assertTrue(entorno.isEmpty(), entorno.toString());
    }
}
