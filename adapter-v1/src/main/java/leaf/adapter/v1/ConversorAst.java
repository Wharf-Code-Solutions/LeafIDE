package leaf.adapter.v1;

import generado.Node;
import generado.SimpleNode;
import generado.Token;
import java.util.ArrayList;
import java.util.List;
import leaf.api.NodoVista;
import leaf.api.Rango;

/** Convierte el AST interno ({@link SimpleNode}) en {@link NodoVista} y genera su volcado de texto. */
public final class ConversorAst {

    private ConversorAst() {}

    /** @return el árbol convertido, o {@code null} si {@code raiz} es null. */
    public static NodoVista convertir(SimpleNode raiz) {
        if (raiz == null) return null;
        List<NodoVista> hijos = new ArrayList<>();
        for (int i = 0; i < raiz.jjtGetNumChildren(); i++) {
            Node hijo = raiz.jjtGetChild(i);
            if (hijo instanceof SimpleNode sn) hijos.add(convertir(sn));
        }
        Object valor = raiz.jjtGetValue();
        return new NodoVista(etiqueta(raiz), valor == null ? null : String.valueOf(valor), rango(raiz), hijos);
    }

    /** Nombre de la clase sin el prefijo "AST" (ASTDeclaracion → Declaracion). */
    static String etiqueta(SimpleNode nodo) {
        String simple = nodo.getClass().getSimpleName();
        if (simple.startsWith("AST") && simple.length() > 3) return simple.substring(3);
        return nodo.toString();
    }

    /** Desde el inicio del primer token hasta el final del último; tolera tokens nulos. */
    static Rango rango(SimpleNode nodo) {
        Token primero = nodo.jjtGetFirstToken();
        Token ultimo = nodo.jjtGetLastToken();
        if (primero == null && ultimo == null) return Rango.DESCONOCIDO;
        if (primero == null) primero = ultimo;
        if (ultimo == null) ultimo = primero;
        if (primero.beginLine <= 0) return Rango.DESCONOCIDO;
        int lineaFin = ultimo.endLine;
        int colFin = ultimo.endColumn;
        // Por seguridad: si el último token queda antes que el primero, se usa el final del primero.
        if (lineaFin < primero.beginLine || (lineaFin == primero.beginLine && colFin < primero.beginColumn)) {
            lineaFin = primero.endLine;
            colFin = primero.endColumn;
        }
        return new Rango(primero.beginLine, primero.beginColumn, lineaFin, colFin);
    }

    /** Volcado indentado (dos espacios por nivel) generado desde el {@link NodoVista}, sin tocar System.out. */
    public static String volcado(NodoVista raiz) {
        StringBuilder sb = new StringBuilder();
        if (raiz != null) volcar(raiz, 0, sb);
        return sb.toString();
    }

    private static void volcar(NodoVista nodo, int nivel, StringBuilder sb) {
        sb.append("  ".repeat(nivel)).append(nodo.etiqueta());
        if (nodo.valor() != null) sb.append(" [").append(nodo.valor()).append(']');
        if (!nodo.rango().esDesconocido()) {
            Rango r = nodo.rango();
            sb.append("  (").append(r.lineaIni()).append(':').append(r.colIni())
              .append('-').append(r.lineaFin()).append(':').append(r.colFin()).append(')');
        }
        sb.append('\n');
        for (NodoVista h : nodo.hijos()) volcar(h, nivel + 1, sb);
    }
}
