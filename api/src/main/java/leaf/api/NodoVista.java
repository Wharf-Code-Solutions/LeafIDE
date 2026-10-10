package leaf.api;

import java.util.List;

/** Nodo de un árbol mostrable (AST u otro). {@code valor} puede ser {@code null}. */
public record NodoVista(String etiqueta, String valor, Rango rango, List<NodoVista> hijos) {
    public NodoVista {
        if (rango == null) rango = Rango.DESCONOCIDO;
        hijos = Copias.lista(hijos);
    }
}
