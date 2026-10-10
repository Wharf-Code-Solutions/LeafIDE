package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import javax.swing.table.TableModel;
import leaf.api.ArtefactoTabla;
import leaf.api.Diagnostico;
import leaf.api.Fila;
import leaf.api.Rango;
import leaf.api.Severidad;
import org.junit.jupiter.api.Test;

class ModelosTablaTest {

    @Test
    void lasEntradasSeConvierteEnModeloDeSoloLectura() {
        ArtefactoTabla t = new ArtefactoTabla("Tokens", List.of("#", "Lexema"), List.of(
                new Fila("1", List.of("1", "programa"), null, null, null),
                new Fila("2", List.of("2"), null, null, null)));

        TableModel m = ModelosTabla.deEntradas(t.columnas(), FilasVisibles.de(t, "", false).entradas());

        assertEquals(2, m.getColumnCount());
        assertEquals("Lexema", m.getColumnName(1));
        assertEquals(2, m.getRowCount());
        assertEquals("programa", m.getValueAt(0, 1));
        assertEquals("", m.getValueAt(1, 1), "una fila corta se completa con vacío");
        assertFalse(m.isCellEditable(0, 0));
    }

    @Test
    void unEncabezadoDeGrupoPoneSuTextoEnLaPrimeraColumna() {
        ArtefactoTabla t = new ArtefactoTabla("Símbolos", List.of("Nombre", "Tipo"), List.of(
                new Fila("1", List.of("$a", "entero"), null, null, "Global")));

        TableModel m = ModelosTabla.deEntradas(t.columnas(), FilasVisibles.de(t, "", true).entradas());

        assertEquals(2, m.getRowCount());
        assertEquals("Global (1)", m.getValueAt(0, 0));
        assertEquals("", m.getValueAt(0, 1));
        assertEquals("$a", m.getValueAt(1, 0));
    }

    @Test
    void problemasMuestraFaseLineaColumnaYMensaje() {
        TableModel m = ModelosTabla.deProblemas(List.of(
                new Diagnostico("lexico", Severidad.ERROR, Rango.punto(3, 5), "símbolo inválido"),
                new Diagnostico("sintactico", Severidad.ERROR, Rango.DESCONOCIDO, "token desconocido")));

        assertEquals(4, m.getColumnCount());
        assertEquals("Fase", m.getColumnName(0));
        assertEquals(2, m.getRowCount());
        assertEquals("Léxico", m.getValueAt(0, 0));
        assertEquals("Sintáctico", m.getValueAt(1, 0));
        assertEquals("3", m.getValueAt(0, 1));
        assertEquals("5", m.getValueAt(0, 2));
        assertEquals("símbolo inválido", m.getValueAt(0, 3));
        assertEquals("—", m.getValueAt(1, 1), "sin posición se muestra un guion");
        assertEquals("—", m.getValueAt(1, 2));
    }
}
