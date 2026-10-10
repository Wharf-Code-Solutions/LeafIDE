package leaf.ide.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void sinCorrerDesdeElJarNoSeAnadeNingunaVariable() {
        // Las pruebas corren desde target/classes, no desde el jar: no debe inventar un CLASSPATH.
        Map<String, String> entorno = Main.entornoParaLaTerminal();
        assertTrue(entorno.isEmpty(), entorno.toString());
    }
}
