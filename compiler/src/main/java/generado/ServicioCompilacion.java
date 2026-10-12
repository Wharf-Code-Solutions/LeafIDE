package generado;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Punto de entrada para compilar texto en memoria (lo usará el IDE).
 * Va en el paquete "generado" porque el estado de Leaf es package-private.
 */
public final class ServicioCompilacion {

    private ServicioCompilacion() {}

    /** synchronized: el estado de Leaf es static, así que solo una compilación a la vez. */
    public static synchronized ResultadoCompilacion compilar(String codigo) {
        // --- Hallazgo 2: reiniciar el estado estático de Leaf ---
        Leaf.errores.clear();
        Leaf.totalErrores = 0;
        Leaf.pilaAberturas.clear();

        // --- Hallazgo 1: compilar texto en vez de un archivo ---
        Leaf parser = new Leaf(new StringReader(codigo));
        Token tokenInicial = parser.token;   // cabeza ficticia; .next recorre todos los tokens
        SimpleNode raiz = null;

        try {
            raiz = parser.programa();
        } catch (ParseException e) {
            Leaf.registrarErrorSintactico(Leaf.tokenParaError(e), Leaf.mensajeReporteError(e));
        } catch (TokenMgrError e) {
            Leaf.totalErrores++;
            Leaf.errores.add("Tipo de Error: Léxico : Colapso fatal del motor al leer caracteres.");
        }

        TablaSimbolos tabla = null;
        VisitanteSemantico visitante = null;
        if (raiz != null) {
            visitante = new VisitanteSemantico();
            try {
                raiz.jjtAccept(visitante, null);
            } catch (RuntimeException e) {
                // Un AST parcial (por errores sintácticos) no debe tumbar al IDE
                Leaf.totalErrores++;
                Leaf.errores.add("Tipo de Error: Interno : El análisis semántico se interrumpió (" + e + ").");
            }
            tabla = visitante.tabla;
        }

        // Igual que Leaf.main: el código intermedio solo se genera si el programa no tiene errores
        GeneradorCodigoIntermedio generador = null;
        String falla = null;
        if (raiz != null && visitante != null && Leaf.errores.isEmpty()) {
            try {
                generador = new GeneradorCodigoIntermedio(visitante);
                generador.generar(raiz);
            } catch (RuntimeException e) {
                generador = null;
                falla = String.valueOf(e.getMessage() != null ? e.getMessage() : e);
            }
        }

        // Copia ordenada por línea: el IDE no debe depender de la lista estática
        ArrayList<String> ordenados = new ArrayList<>(Leaf.errores);
        ordenados.sort(Comparator.comparingInt(Leaf::extraerLinea));
        return new ResultadoCompilacion(ordenados, raiz, tabla, tokenInicial, generador, falla);
    }
}
