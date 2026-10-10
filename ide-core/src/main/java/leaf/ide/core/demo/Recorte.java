package leaf.ide.core.demo;

import java.util.ArrayList;
import java.util.List;
import leaf.api.Artefacto;
import leaf.api.ArtefactoArbol;
import leaf.api.ArtefactoTabla;
import leaf.api.ArtefactoTexto;
import leaf.api.Fila;
import leaf.api.NodoVista;
import leaf.api.Rango;

/**
 * Muestra un artefacto "a medias" para la demostración: la unidad de una tabla es la fila, la de un árbol el nodo (en
 * orden de recorrido: primero el padre, luego sus hijos de izquierda a derecha) y la de un texto la línea. Las vistas
 * no cambian: reciben el artefacto recortado como si fuera el resultado completo.
 */
public final class Recorte {

    private Recorte() {}

    public static int unidades(Artefacto a) {
        return switch (a) {
            case ArtefactoTabla t -> t.filas().size();
            case ArtefactoArbol t -> contar(t.raiz());
            case ArtefactoTexto t -> lineas(t.contenido()).size();
        };
    }

    /** El artefacto con solo sus primeras {@code k} unidades. */
    public static Artefacto primeros(Artefacto a, int k) {
        return switch (a) {
            case ArtefactoTabla t -> new ArtefactoTabla(t.titulo(), t.columnas(),
                    t.filas().subList(0, Math.clamp(k, 0, t.filas().size())));
            case ArtefactoArbol t -> new ArtefactoArbol(t.titulo(), podar(t.raiz(), new int[] {k}));
            case ArtefactoTexto t -> {
                List<String> ls = lineas(t.contenido());
                yield new ArtefactoTexto(t.titulo(), t.lenguaje(),
                        String.join("\n", ls.subList(0, Math.clamp(k, 0, ls.size()))), t.lineaARefs());
            }
        };
    }

    /** Rango del código de la unidad número {@code k} (1 = la primera); desconocido si no tiene. */
    public static Rango rangoDe(Artefacto a, int k) {
        return switch (a) {
            case ArtefactoTabla t -> k >= 1 && k <= t.filas().size() ? t.filas().get(k - 1).origen() : Rango.DESCONOCIDO;
            case ArtefactoArbol t -> {
                NodoVista n = nodo(t.raiz(), k);
                yield n == null ? Rango.DESCONOCIDO : n.rango();
            }
            case ArtefactoTexto t -> Rango.DESCONOCIDO;
        };
    }

    /** Texto corto de la unidad número {@code k}, para la narración. */
    public static String celdasDe(Artefacto a, int k) {
        return switch (a) {
            case ArtefactoTabla t -> k >= 1 && k <= t.filas().size() ? unir(t.filas().get(k - 1)) : "";
            case ArtefactoArbol t -> {
                NodoVista n = nodo(t.raiz(), k);
                yield n == null ? "" : (n.valor() == null || n.valor().isBlank() ? n.etiqueta() : n.etiqueta() + " = " + n.valor());
            }
            case ArtefactoTexto t -> {
                List<String> ls = lineas(t.contenido());
                yield k >= 1 && k <= ls.size() ? ls.get(k - 1) : "";
            }
        };
    }

    /** Grupo (por ejemplo, el ámbito) de la fila {@code k} de una tabla; {@code null} si no hay. */
    public static String grupoDe(Artefacto a, int k) {
        return a instanceof ArtefactoTabla t && k >= 1 && k <= t.filas().size() ? t.filas().get(k - 1).grupo() : null;
    }

    private static String unir(Fila f) {
        return String.join(" · ", f.celdas());
    }

    private static List<String> lineas(String contenido) {
        if (contenido.isEmpty()) return List.of();
        return List.of(contenido.split("\n", -1));
    }

    private static int contar(NodoVista n) {
        int r = 1;
        for (NodoVista h : n.hijos()) r += contar(h);
        return r;
    }

    /** El nodo número {@code k} en orden de recorrido (1 = la raíz); {@code null} si no existe. */
    private static NodoVista nodo(NodoVista raiz, int k) {
        List<NodoVista> orden = new ArrayList<>();
        aplanar(raiz, orden);
        return k >= 1 && k <= orden.size() ? orden.get(k - 1) : null;
    }

    private static void aplanar(NodoVista n, List<NodoVista> destino) {
        destino.add(n);
        for (NodoVista h : n.hijos()) aplanar(h, destino);
    }

    /** Copia del árbol con los primeros {@code restantes[0]} nodos en orden de recorrido; consume el contador. */
    private static NodoVista podar(NodoVista n, int[] restantes) {
        restantes[0]--;
        List<NodoVista> hijos = new ArrayList<>();
        for (NodoVista h : n.hijos()) {
            if (restantes[0] <= 0) break;
            hijos.add(podar(h, restantes));
        }
        return new NodoVista(n.etiqueta(), n.valor(), n.rango(), hijos);
    }
}
