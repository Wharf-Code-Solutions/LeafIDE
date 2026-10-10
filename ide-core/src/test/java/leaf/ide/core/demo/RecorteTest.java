package leaf.ide.core.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import leaf.api.Artefacto;
import leaf.api.ArtefactoArbol;
import leaf.api.ArtefactoTabla;
import leaf.api.ArtefactoTexto;
import leaf.api.Fila;
import leaf.api.NodoVista;
import leaf.api.Rango;
import org.junit.jupiter.api.Test;

class RecorteTest {

    private static NodoVista nodo(String etiqueta, NodoVista... hijos) {
        return new NodoVista(etiqueta, null, Rango.punto(1, 1), List.of(hijos));
    }

    /** raíz(a(a1, a2), b(b1), c): 7 nodos; en orden de recorrido: raíz, a, a1, a2, b, b1, c. */
    private static ArtefactoArbol arbol() {
        return new ArtefactoArbol("AST", nodo("raiz", nodo("a", nodo("a1"), nodo("a2")), nodo("b", nodo("b1")), nodo("c")));
    }

    private static ArtefactoTabla tabla(int n) {
        List<Fila> filas = new ArrayList<>();
        for (int i = 1; i <= n; i++) filas.add(new Fila("f" + i, List.of("v" + i), Rango.punto(1, i), null, null));
        return new ArtefactoTabla("T", List.of("c"), filas);
    }

    private static ArtefactoTexto texto() {
        return new ArtefactoTexto("x", "txt", "uno\ndos\ntres", Map.of());
    }

    @Test
    void cuentaLasUnidadesDeCadaTipo() {
        assertEquals(5, Recorte.unidades(tabla(5)));
        assertEquals(7, Recorte.unidades(arbol()));
        assertEquals(3, Recorte.unidades(texto()));
        assertEquals(0, Recorte.unidades(new ArtefactoTexto("x", "txt", "", Map.of())));
    }

    @Test
    void recortaUnaTablaALasPrimerasFilas() {
        ArtefactoTabla r = (ArtefactoTabla) Recorte.primeros(tabla(5), 2);
        assertEquals(List.of("f1", "f2"), r.filas().stream().map(Fila::id).toList());
        assertEquals("T", r.titulo());
        assertEquals(List.of("c"), r.columnas());
        assertEquals(5, ((ArtefactoTabla) Recorte.primeros(tabla(5), 99)).filas().size());
        assertEquals(0, ((ArtefactoTabla) Recorte.primeros(tabla(5), 0)).filas().size());
    }

    @Test
    void recortaUnArbolEnOrdenDeRecorridoSinDejarHijosSueltos() {
        assertEquals(List.of("raiz"), etiquetas(Recorte.primeros(arbol(), 1)));
        assertEquals(List.of("raiz", "a", "a1"), etiquetas(Recorte.primeros(arbol(), 3)));
        assertEquals(List.of("raiz", "a", "a1", "a2", "b"), etiquetas(Recorte.primeros(arbol(), 5)));
        assertEquals(7, etiquetas(Recorte.primeros(arbol(), 99)).size());
    }

    private static List<String> etiquetas(Artefacto a) {
        List<String> r = new ArrayList<>();
        recoger(((ArtefactoArbol) a).raiz(), r);
        return r;
    }

    private static void recoger(NodoVista n, List<String> r) {
        r.add(n.etiqueta());
        for (NodoVista h : n.hijos()) recoger(h, r);
    }

    @Test
    void recortaUnTextoPorLineas() {
        assertEquals("uno\ndos", ((ArtefactoTexto) Recorte.primeros(texto(), 2)).contenido());
    }

    @Test
    void elRangoYElTextoDeLaUnidadK() {
        assertEquals(Rango.punto(1, 3), Recorte.rangoDe(tabla(5), 3));
        assertEquals("v3", Recorte.celdasDe(tabla(5), 3));
        assertEquals("c", Recorte.celdasDe(arbol(), 7));
        assertEquals("a2", Recorte.celdasDe(arbol(), 4));
        assertEquals("dos", Recorte.celdasDe(texto(), 2));
        assertEquals(Rango.DESCONOCIDO, Recorte.rangoDe(texto(), 1));
    }
}
