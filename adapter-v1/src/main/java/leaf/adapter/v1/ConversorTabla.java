package leaf.adapter.v1;

import generado.Simbolo;
import generado.TablaSimbolos;
import java.util.ArrayList;
import java.util.List;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;
import leaf.api.Rango;

/**
 * Convierte la {@link TablaSimbolos} en las tablas de la fase semántica.
 * Los símbolos se obtienen de los eventos INSERTAR del historial (la tabla descarta los ámbitos al salir de ellos).
 * Se lee el estado FINAL de cada {@link Simbolo}: {@code tipoRetorno} y {@code tiposParametros} se asignan después de
 * insertar, y el evento guarda la referencia al mismo objeto.
 */
public final class ConversorTabla {

    public static final List<String> COLUMNAS_SIMBOLOS = List.of(
            "Nombre", "Tipo", "Categoría", "Ámbito", "Offset", "Dimensiones", "Línea", "Firma");
    public static final List<String> COLUMNAS_TRAZA = List.of("#", "Evento", "Ámbito", "Símbolo", "Detalle");

    private ConversorTabla() {}

    public static ArtefactoTabla tablaSimbolos(TablaSimbolos tabla) {
        List<Fila> filas = new ArrayList<>();
        if (tabla != null) {
            int n = 0;
            for (TablaSimbolos.EventoTabla ev : tabla.historial) {
                if (ev.tipo() != TablaSimbolos.TipoEvento.INSERTAR || ev.simbolo() == null) continue;
                Simbolo s = ev.simbolo();
                n++;
                Rango origen = s.linea > 0 ? new Rango(s.linea, 0, s.linea, 0) : Rango.DESCONOCIDO;
                filas.add(new Fila("s" + n,
                        List.of(texto(s.nombre), texto(s.tipo), texto(s.categoria), texto(s.ambito),
                                String.valueOf(s.offset), String.valueOf(s.dimensiones), String.valueOf(s.linea),
                                firma(s)),
                        origen, List.of(), texto(s.ambito)));
            }
        }
        return new ArtefactoTabla("Tabla de símbolos", COLUMNAS_SIMBOLOS, filas);
    }

    public static ArtefactoTabla tablaTraza(TablaSimbolos tabla) {
        List<Fila> filas = new ArrayList<>();
        if (tabla != null) {
            int n = 0;
            for (TablaSimbolos.EventoTabla ev : tabla.historial) {
                n++;
                String simbolo = ev.simbolo() == null ? "" : texto(ev.simbolo().nombre);
                filas.add(new Fila(String.valueOf(n),
                        List.of(String.valueOf(n), String.valueOf(ev.tipo()), texto(ev.ambito()), simbolo,
                                texto(ev.mensaje())),
                        null, List.of(), null));
            }
        }
        return new ArtefactoTabla("Traza de ámbitos", COLUMNAS_TRAZA, filas);
    }

    /** "(entero, decimal) -> texto" para funciones; vacío para lo demás. */
    static String firma(Simbolo s) {
        if (!"funcion".equals(s.categoria)) return "";
        List<String> params = s.tiposParametros == null ? List.of() : s.tiposParametros;
        String retorno = s.tipoRetorno != null ? s.tipoRetorno : texto(s.tipo);
        return "(" + String.join(", ", params) + ") -> " + retorno;
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
