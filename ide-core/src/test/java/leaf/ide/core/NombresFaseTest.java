package leaf.ide.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NombresFaseTest {

    @Test
    void fasesConocidasLlevanAcento() {
        assertEquals("Léxico", NombresFase.de("lexico"));
        assertEquals("Sintáctico", NombresFase.de("sintactico"));
        assertEquals("Semántico", NombresFase.de("semantico"));
        assertEquals("Intermedio", NombresFase.de("intermedio"));
        assertEquals("Optimización", NombresFase.de("optimizacion"));
        assertEquals("Objeto", NombresFase.de("objeto"));
        assertEquals("Interno", NombresFase.de("interno"));
    }

    @Test
    void faseDesconocidaSeMuestraConInicialMayuscula() {
        assertEquals("Nueva", NombresFase.de("nueva"));
        assertEquals("", NombresFase.de(""));
    }
}
