package leaf.adapter.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import generado.LeafConstants;
import generado.SimpleNode;
import generado.Token;
import leaf.api.NodoVista;
import org.junit.jupiter.api.Test;

class ConversorAstTest {

    @Test
    void convierteNodoSimple() {
        SimpleNode sn = new SimpleNode(0);
        sn.jjtSetValue("valor test");
        
        Token t1 = new Token(LeafConstants.ID, "a");
        t1.beginLine = 1; t1.beginColumn = 2; t1.endLine = 1; t1.endColumn = 3;
        
        sn.jjtSetFirstToken(t1);
        sn.jjtSetLastToken(t1);

        NodoVista nodo = ConversorAst.convertir(sn);
        assertEquals("void", nodo.etiqueta());
        assertEquals("valor test", nodo.valor());
        assertEquals(1, nodo.rango().lineaIni());
        assertEquals(2, nodo.rango().colIni());
        assertEquals(3, nodo.rango().colFin());
        assertTrue(nodo.hijos().isEmpty());
    }

    @Test
    void etiquetaQuitaPrefijoAst() {
        // En lugar de instanciar un tipo autogenerado que pueda faltar, simulamos su nombre
        class ASTFalso extends SimpleNode {
            public ASTFalso() { super(0); }
        }
        ASTFalso falso = new ASTFalso();
        assertEquals("Falso", ConversorAst.etiqueta(falso));
    }

    @Test
    void volcadoConIndentacion() {
        NodoVista hijo = new NodoVista("Hijo", null, null, null);
        NodoVista raiz = new NodoVista("Raiz", "val", null, java.util.List.of(hijo));
        
        String volcado = ConversorAst.volcado(raiz);
        assertTrue(volcado.contains("Raiz [val]"));
        assertTrue(volcado.contains("  Hijo")); // Indentado
    }
}
