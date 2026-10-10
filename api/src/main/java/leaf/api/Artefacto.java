package leaf.api;

/** Todo artefacto de una fase es una tabla, un árbol o un texto. */
public sealed interface Artefacto permits ArtefactoTabla, ArtefactoArbol, ArtefactoTexto {
    String titulo();
}
