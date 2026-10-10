package leaf.adapter.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import generado.LeafConstants;
import generado.Token;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;
import org.junit.jupiter.api.Test;

class ConversorTokensTest {

    @Test
    void convierteTokensYExcluyeEof() {
        Token inicial = new Token();
        
        Token t1 = new Token(LeafConstants.DEFINIR, "definir");
        t1.beginLine = 1; t1.beginColumn = 1; t1.endLine = 1; t1.endColumn = 7;
        
        Token t2 = new Token(LeafConstants.EOF, "");
        
        inicial.next = t1;
        t1.next = t2;

        ArtefactoTabla tabla = ConversorTokens.tablaTokens(inicial);
        assertEquals(1, tabla.filas().size()); // Solo t1, EOF ignorado

        Fila f = tabla.filas().get(0);
        assertEquals("1", f.celdas().get(0));
        assertEquals("1", f.celdas().get(1)); // Línea
        assertEquals("1", f.celdas().get(2)); // Col
        assertEquals("DEFINIR", f.celdas().get(3));
        assertEquals("definir", f.celdas().get(4));
        assertEquals("Palabra reservada", f.celdas().get(5));
        
        assertEquals(1, f.origen().lineaIni());
        assertEquals(7, f.origen().colFin());
    }

    @Test
    void lasFilasDeTokensNoTienenGrupoParaQueLaUiNoLasReordene() {
        Token inicial = new Token();
        Token t1 = new Token(LeafConstants.DEFINIR, "definir");
        t1.beginLine = 1; t1.beginColumn = 1; t1.endLine = 1; t1.endColumn = 7;
        inicial.next = t1;
        t1.next = new Token(LeafConstants.EOF, "");

        assertEquals(null, ConversorTokens.tablaTokens(inicial).filas().get(0).grupo());
    }

    @Test
    void generaTablaConteo() {
        Token inicial = new Token();
        
        Token t1 = new Token(LeafConstants.ENTERO_T, "entero"); // Tipo
        Token t2 = new Token(LeafConstants.ID, "x"); // ID
        Token t3 = new Token(LeafConstants.ID, "y"); // ID
        
        inicial.next = t1; t1.next = t2; t2.next = t3;
        t3.next = new Token(LeafConstants.EOF, "");

        ArtefactoTabla tokens = ConversorTokens.tablaTokens(inicial);
        ArtefactoTabla conteo = ConversorTokens.tablaConteo(tokens);

        // Tipo de dato: 1, Identificador: 2, total: 3
        assertEquals(3, conteo.filas().size());
        
        assertEquals("Tipo de dato", conteo.filas().get(0).celdas().get(0));
        assertEquals("1", conteo.filas().get(0).celdas().get(1));
        
        assertEquals("Identificador", conteo.filas().get(1).celdas().get(0));
        assertEquals("2", conteo.filas().get(1).celdas().get(1));

        assertEquals("Total", conteo.filas().get(2).celdas().get(0));
        assertEquals("3", conteo.filas().get(2).celdas().get(1));
    }
}
