package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;
import leaf.api.NodoVista;
import leaf.api.Rango;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tabla y árbol enlazados al editor: clic hacia el editor, cursor hacia la vista, filtro y agrupación. */
class VistasEnlazadasTest {

    private final List<Rango> elegidos = new ArrayList<>();

    @BeforeEach
    void requierePantalla() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
    }

    private static Fila fila(String id, Rango origen, String grupo, String... celdas) {
        return new Fila(id, List.of(celdas), origen, null, grupo);
    }

    /** definir $x como entero.  → tokens en la línea 1. */
    private static ArtefactoTabla tokens() {
        return new ArtefactoTabla("Tokens", List.of("#", "Lexema"), List.of(
                fila("1", new Rango(1, 1, 1, 7), null, "1", "definir"),
                fila("2", new Rango(1, 9, 1, 10), null, "2", "$x"),
                fila("3", new Rango(1, 12, 1, 15), null, "3", "como"),
                fila("4", Rango.DESCONOCIDO, null, "4", "sin posición")));
    }

    private static ArtefactoTabla simbolos() {
        return new ArtefactoTabla("Símbolos", List.of("Nombre", "Ámbito"), List.of(
                fila("1", new Rango(1, 0, 1, 0), "Global", "$a", "Global"),
                fila("2", new Rango(4, 0, 4, 0), "area", "$b", "area"),
                fila("3", new Rango(2, 0, 2, 0), "Global", "$c", "Global")));
    }

    // ---------- tabla ----------

    @Test
    void elegirUnaFilaConPosicionAvisaAlEditor() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        v.elegir(1);
        assertEquals(List.of(new Rango(1, 9, 1, 10)), elegidos);
    }

    @Test
    void elegirLaMismaFilaDosVecesAvisaDosVeces() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        v.elegir(0);
        v.elegir(0);
        assertEquals(2, elegidos.size());
    }

    @Test
    void unaFilaSinPosicionNoAvisaAlEditor() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        v.elegir(3);
        assertTrue(elegidos.isEmpty());
    }

    @Test
    void seleccionarConElTecladoTambienAvisa() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        v.tabla().setRowSelectionInterval(2, 2);
        assertEquals(List.of(new Rango(1, 12, 1, 15)), elegidos);
    }

    @Test
    void elCursorSeleccionaElTokenQueLoContieneSinAvisarAlEditor() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        v.cursorEn(1, 10);
        assertEquals(1, v.tabla().getSelectedRow());
        v.cursorEn(1, 3);
        assertEquals(0, v.tabla().getSelectedRow());
        assertTrue(elegidos.isEmpty(), "mover el cursor no debe volver a resaltar en el editor");
    }

    @Test
    void justoDespuesDelTokenSeSeleccionaElQueTermina() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        v.cursorEn(1, 8); // entre "definir" y "$x": hay un espacio, nada lo contiene; el cursor toca el final de "definir"
        assertEquals(0, v.tabla().getSelectedRow());
    }

    @Test
    void sinTokenBajoElCursorSeQuitaLaSeleccion() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        v.cursorEn(1, 1);
        assertEquals(0, v.tabla().getSelectedRow());
        v.cursorEn(7, 7);
        assertEquals(-1, v.tabla().getSelectedRow());
    }

    @Test
    void lasFilasDeLineaCompletaNoSeSeleccionanConElCursor() {
        VistaTabla v = new VistaTabla(simbolos(), elegidos::add);
        v.cursorEn(1, 3);
        assertEquals(-1, v.tabla().getSelectedRow());
    }

    @Test
    void elFiltroReduceLasFilasYMuestraElConteo() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        assertEquals(4, v.tabla().getRowCount());
        assertEquals("4 de 4 filas", v.conteo());
        v.filtrar("co");
        assertEquals(1, v.tabla().getRowCount());
        assertEquals("1 de 4 filas", v.conteo());
        v.filtrar("");
        assertEquals(4, v.tabla().getRowCount());
    }

    @Test
    void conElFiltroPuestoElegirUsaLaFilaVisible() {
        VistaTabla v = new VistaTabla(tokens(), elegidos::add);
        v.filtrar("como");
        v.elegir(0);
        assertEquals(List.of(new Rango(1, 12, 1, 15)), elegidos);
    }

    @Test
    void laTablaConGruposSeAgrupaPorDefectoYSePuedeAplanar() {
        VistaTabla v = new VistaTabla(simbolos(), elegidos::add);
        assertTrue(v.hayGrupos());
        assertEquals(5, v.tabla().getRowCount()); // 2 encabezados + 3 filas
        assertEquals("Global (2)", v.tabla().getValueAt(0, 0));
        assertEquals("$a", v.tabla().getValueAt(1, 0));
        assertEquals("area (1)", v.tabla().getValueAt(3, 0));

        v.agrupar(false);
        assertEquals(3, v.tabla().getRowCount());
        assertEquals("$a", v.tabla().getValueAt(0, 0));
    }

    @Test
    void clicEnUnEncabezadoDeGrupoNoHaceNada() {
        VistaTabla v = new VistaTabla(simbolos(), elegidos::add);
        v.elegir(0); // encabezado "Global (2)"
        assertTrue(elegidos.isEmpty());
        v.elegir(3); // encabezado "area (1)"
        assertTrue(elegidos.isEmpty());
        v.elegir(4); // "$b" (línea 4)
        assertEquals(List.of(new Rango(4, 0, 4, 0)), elegidos);
    }

    @Test
    void unaTablaSinGruposNoOfreceAgrupar() {
        assertFalse(new VistaTabla(tokens(), elegidos::add).hayGrupos());
    }

    @Test
    void elEstadoDelFiltroSePuedeRestaurarTrasReconstruirLaVista() {
        VistaTabla a = new VistaTabla(simbolos(), elegidos::add);
        a.filtrar("area");
        a.agrupar(false);

        VistaTabla b = new VistaTabla(simbolos(), elegidos::add);
        b.restaurar(a.estado());
        assertEquals("area", b.estado().filtro());
        assertEquals(false, b.estado().agrupar());
        assertEquals(1, b.tabla().getRowCount());
    }

    // ---------- árbol ----------

    private static NodoVista arbol() {
        // Programa (1:1-2:20) > Declaracion (1:1-1:20) > [Variable $x (1:9-1:10), Tipo entero (1:12-1:17)]; Mostrar (2:1-2:20)
        return new NodoVista("Programa", null, new Rango(1, 1, 2, 20), List.of(
                new NodoVista("Declaracion", null, new Rango(1, 1, 1, 20), List.of(
                        new NodoVista("Variable", "$x", new Rango(1, 9, 1, 10), null),
                        new NodoVista("Tipo", "entero", new Rango(1, 12, 1, 17), null))),
                new NodoVista("Mostrar", null, new Rango(2, 1, 2, 20), null)));
    }

    private static NodoVista etiquetaDe(TreePath p) {
        return ((VistaArbol.NodoUi) ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject()).nodo();
    }

    @Test
    void elegirUnNodoAvisaAlEditorConSuRango() {
        VistaArbol v = new VistaArbol(arbol(), elegidos::add);
        TreePath variable = v.rutaDe("Variable");
        v.elegir(variable);
        assertEquals(List.of(new Rango(1, 9, 1, 10)), elegidos);
    }

    @Test
    void elCursorSeleccionaElNodoMasProfundoQueLoContiene() {
        VistaArbol v = new VistaArbol(arbol(), elegidos::add);
        v.cursorEn(1, 10);
        assertEquals("Variable", etiquetaDe(v.arbol().getSelectionPath()).etiqueta());
        v.cursorEn(1, 18); // dentro de Declaracion pero fuera de sus hijos
        assertEquals("Declaracion", etiquetaDe(v.arbol().getSelectionPath()).etiqueta());
        v.cursorEn(2, 5);
        assertEquals("Mostrar", etiquetaDe(v.arbol().getSelectionPath()).etiqueta());
        assertTrue(elegidos.isEmpty(), "mover el cursor no debe volver a resaltar en el editor");
    }

    @Test
    void sinNodoBajoElCursorSeQuitaLaSeleccion() {
        VistaArbol v = new VistaArbol(arbol(), elegidos::add);
        v.cursorEn(1, 10);
        v.cursorEn(9, 1);
        assertNull(v.arbol().getSelectionPath());
    }
}
