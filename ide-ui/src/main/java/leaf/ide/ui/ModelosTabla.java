package leaf.ide.ui;

import java.util.List;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import leaf.api.Diagnostico;
import leaf.ide.core.NombresFase;
import leaf.api.Fila;

/** Convierte datos de la API en modelos de tabla de solo lectura. */
final class ModelosTabla {

    static final List<String> COLUMNAS_PROBLEMAS = List.of("Fase", "Línea", "Col.", "Mensaje");

    private ModelosTabla() {}

    /**
     * Modelo con las entradas que se van a mostrar: una fila por cada fila de datos y, para un encabezado de grupo,
     * el texto en la primera columna y el resto vacío.
     */
    static TableModel deEntradas(List<String> columnas, List<FilasVisibles.Entrada> entradas) {
        Object[][] datos = new Object[entradas.size()][columnas.size()];
        for (int i = 0; i < datos.length; i++) {
            FilasVisibles.Entrada entrada = entradas.get(i);
            Fila fila = entrada.fila();
            for (int c = 0; c < columnas.size(); c++) {
                if (fila == null) datos[i][c] = c == 0 ? entrada.encabezado() : "";
                else datos[i][c] = c < fila.celdas().size() ? fila.celdas().get(c) : "";
            }
        }
        return soloLectura(datos, columnas);
    }

    static TableModel deProblemas(List<Diagnostico> diagnosticos) {
        Object[][] datos = new Object[diagnosticos.size()][];
        for (int i = 0; i < datos.length; i++) {
            Diagnostico d = diagnosticos.get(i);
            boolean conPosicion = !d.rango().esDesconocido();
            datos[i] = new Object[] {
                NombresFase.de(d.faseId()),
                conPosicion ? String.valueOf(d.rango().lineaIni()) : "—",
                conPosicion ? String.valueOf(d.rango().colIni()) : "—",
                d.mensaje()
            };
        }
        return soloLectura(datos, COLUMNAS_PROBLEMAS);
    }

    private static TableModel soloLectura(Object[][] datos, List<String> columnas) {
        return new DefaultTableModel(datos, columnas.toArray()) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }
}
