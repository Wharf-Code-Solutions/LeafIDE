package leaf.adapter.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import generado.LeafConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import leaf.api.LenguajeDef;
import org.junit.jupiter.api.Test;

class LenguajeLeafTest {

    private final LenguajeDef lenguaje = LenguajeLeaf.cargar();

    @Test
    void cargaLosDatosBasicos() {
        assertEquals("Leaf", lenguaje.nombre());
        assertEquals(List.of("leaf", "txt"), lenguaje.extensiones());
        assertEquals("//", lenguaje.comentarioLinea());
        assertEquals("/*", lenguaje.comentarioBloqueInicio());
        assertEquals("*/", lenguaje.comentarioBloqueFin());
        assertEquals("$", lenguaje.prefijoVariable());
    }

    @Test
    void agrupaLasPalabrasPorCategoria() {
        assertTrue(lenguaje.palabras(LenguajeDef.RESERVADA).containsAll(List.of("definir", "con valor de", "sino si", "ingresar")));
        assertTrue(lenguaje.palabras(LenguajeDef.TIPO).containsAll(List.of("entero", "logico", "vacio")));
        assertEquals(List.of("verdadero", "falso"), lenguaje.palabras(LenguajeDef.LITERAL));
        assertTrue(lenguaje.palabras(LenguajeDef.OPERADOR).containsAll(List.of("Y", "O", "NO", "mayor o igual que")));
    }

    /**
     * Prueba de deriva: toda palabra o símbolo literal de la gramática debe estar en leaf-lenguaje.json y al revés.
     * Si falla porque añadiste una palabra a Leaf.jjt, agrégala al JSON (en la categoría que corresponda).
     */
    @Test
    void elJsonCoincideConLaGramatica() {
        Set<String> gramatica = literalesDeLaGramatica();
        Set<String> json = new TreeSet<>(lenguaje.todasLasPalabras());
        json.addAll(lenguaje.operadores());
        json.addAll(lenguaje.delimitadores());
        // Los marcadores de comentario de bloque y el prefijo de variable también son literales sueltos de la gramática.
        json.add(lenguaje.comentarioBloqueInicio());
        json.add(lenguaje.comentarioBloqueFin());
        json.add(lenguaje.prefijoVariable());

        Set<String> faltanEnElJson = new TreeSet<>(gramatica);
        faltanEnElJson.removeAll(json);
        Set<String> sobranEnElJson = new TreeSet<>(json);
        sobranEnElJson.removeAll(gramatica);

        assertTrue(faltanEnElJson.isEmpty(), "En Leaf.jjt pero no en leaf-lenguaje.json: " + faltanEnElJson);
        assertTrue(sobranEnElJson.isEmpty(), "En leaf-lenguaje.json pero no en Leaf.jjt: " + sobranEnElJson);
    }

    @Test
    void ningunaPalabraSeRepiteEntreCategorias() {
        List<String> todas = new ArrayList<>(lenguaje.todasLasPalabras());
        Set<String> unicas = new TreeSet<>(todas);
        assertEquals(unicas.size(), todas.size(), "palabras repetidas en el JSON");
    }

    /** Imágenes {@code "texto"} de {@link LeafConstants#tokenImage}, sin las que llevan escapes o son espacios. */
    private static Set<String> literalesDeLaGramatica() {
        Set<String> salida = new TreeSet<>();
        for (String imagen : LeafConstants.tokenImage) {
            if (imagen == null || imagen.length() < 3 || !imagen.startsWith("\"") || !imagen.endsWith("\"")) continue;
            String interior = imagen.substring(1, imagen.length() - 1);
            if (interior.isBlank() || interior.contains("\\") || interior.contains("\"")) continue;
            salida.add(interior);
        }
        return salida;
    }
}
