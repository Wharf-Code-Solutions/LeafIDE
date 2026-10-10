package leaf.ide.core.consola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;
import org.junit.jupiter.api.Test;

class FormatoTextoTest {

    private static Fila fila(String... celdas) {
        return new Fila("x", List.of(celdas), null, null, null);
    }

    @Test
    void alineaLasColumnasConEncabezadoYSeparador() {
        ArtefactoTabla t = new ArtefactoTabla("Tokens", List.of("#", "Lexema"), List.of(
                fila("1", "definir"), fila("10", "$x")));

        String esperado = String.join("\n",
                "#   Lexema",
                "--  -------",
                "1   definir",
                "10  $x",
                "",
                "2 filas",
                "");
        assertEquals(esperado, FormatoTexto.tabla(t));
    }

    @Test
    void unaSolaFilaUsaElSingular() {
        ArtefactoTabla t = new ArtefactoTabla("T", List.of("A"), List.of(fila("uno")));
        assertTrue(FormatoTexto.tabla(t).endsWith("\n1 fila\n"));
    }

    @Test
    void unaTablaSinFilasLoDice() {
        ArtefactoTabla t = new ArtefactoTabla("T", List.of("A", "B"), List.of());
        String texto = FormatoTexto.tabla(t);
        assertTrue(texto.startsWith("A  B\n"), texto);
        assertTrue(texto.endsWith("\n(sin filas)\n"), texto);
    }

    @Test
    void unaFilaCortaSeCompletaConVacio() {
        ArtefactoTabla t = new ArtefactoTabla("T", List.of("A", "B"), List.of(fila("x")));
        assertTrue(FormatoTexto.tabla(t).contains("\nx\n"), FormatoTexto.tabla(t));
    }

    @Test
    void unaCeldaMuyLargaSeRecortaConPuntosSuspensivos() {
        String larga = "a".repeat(100);
        ArtefactoTabla t = new ArtefactoTabla("T", List.of("A", "B"), List.of(fila(larga, "fin")));
        String linea = FormatoTexto.tabla(t).lines().filter(l -> l.startsWith("aaa")).findFirst().orElseThrow();
        assertTrue(linea.startsWith("a".repeat(59) + "…  fin"), linea);
    }
}
