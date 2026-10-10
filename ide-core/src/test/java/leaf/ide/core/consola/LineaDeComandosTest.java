package leaf.ide.core.consola;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LineaDeComandosTest {

    @Test
    void separaPorEspacios() {
        assertEquals(List.of("compilar", "--hasta=lexico"), LineaDeComandos.dividir("compilar --hasta=lexico"));
    }

    @Test
    void ignoraEspaciosSobrantesYLineasVacias() {
        assertEquals(List.of("a", "b"), LineaDeComandos.dividir("   a    b   "));
        assertEquals(List.of(), LineaDeComandos.dividir("   "));
        assertEquals(List.of(), LineaDeComandos.dividir(""));
    }

    @Test
    void lasComillasAgrupanUnArgumentoConEspacios() {
        assertEquals(List.of("abrir", "C:\\mis archivos\\a b.leaf"), LineaDeComandos.dividir("abrir \"C:\\mis archivos\\a b.leaf\""));
    }

    @Test
    void lasBarrasInvertidasDeWindowsSeConservanTalCual() {
        assertEquals(List.of("cd", "C:\\LeafIDE\\ejemplos"), LineaDeComandos.dividir("cd C:\\LeafIDE\\ejemplos"));
    }

    @Test
    void unaComillaSinCerrarAgrupaHastaElFinal() {
        assertEquals(List.of("abrir", "a b c"), LineaDeComandos.dividir("abrir \"a b c"));
    }

    @Test
    void unArgumentoVacioEntreComillasSeConserva() {
        assertEquals(List.of("guardar", ""), LineaDeComandos.dividir("guardar \"\""));
    }

    @Test
    void comillasPegadasAUnTextoSeUnenEnUnSoloArgumento() {
        assertEquals(List.of("--ruta=a b"), LineaDeComandos.dividir("--ruta=\"a b\""));
    }
}
