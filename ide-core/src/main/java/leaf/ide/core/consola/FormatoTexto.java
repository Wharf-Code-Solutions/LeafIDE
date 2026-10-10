package leaf.ide.core.consola;

import java.util.ArrayList;
import java.util.List;
import leaf.api.ArtefactoTabla;
import leaf.api.Fila;

/** Texto alineado en columnas de un {@link ArtefactoTabla}, para imprimirlo en la consola. */
public final class FormatoTexto {

    private static final int ANCHO_MAXIMO = 60;
    private static final String SEPARACION = "  ";

    private FormatoTexto() {}

    public static String tabla(ArtefactoTabla tabla) {
        int columnas = tabla.columnas().size();
        int[] ancho = new int[columnas];
        for (int c = 0; c < columnas; c++) ancho[c] = Math.min(ANCHO_MAXIMO, tabla.columnas().get(c).length());
        List<List<String>> filas = new ArrayList<>();
        for (Fila f : tabla.filas()) {
            List<String> celdas = new ArrayList<>(columnas);
            for (int c = 0; c < columnas; c++) {
                String celda = recortar(c < f.celdas().size() && f.celdas().get(c) != null ? f.celdas().get(c) : "");
                celdas.add(celda);
                ancho[c] = Math.max(ancho[c], celda.length());
            }
            filas.add(celdas);
        }

        StringBuilder sb = new StringBuilder();
        linea(sb, tabla.columnas().stream().map(FormatoTexto::recortar).toList(), ancho);
        List<String> guiones = new ArrayList<>();
        for (int c = 0; c < columnas; c++) guiones.add("-".repeat(ancho[c]));
        linea(sb, guiones, ancho);
        for (List<String> celdas : filas) linea(sb, celdas, ancho);
        sb.append('\n');
        int n = filas.size();
        sb.append(n == 0 ? "(sin filas)" : n + (n == 1 ? " fila" : " filas")).append('\n');
        return sb.toString();
    }

    private static void linea(StringBuilder sb, List<String> celdas, int[] ancho) {
        StringBuilder l = new StringBuilder();
        for (int c = 0; c < ancho.length; c++) {
            if (c > 0) l.append(SEPARACION);
            String celda = celdas.get(c);
            l.append(celda).append(" ".repeat(Math.max(0, ancho[c] - celda.length())));
        }
        int fin = l.length();
        while (fin > 0 && l.charAt(fin - 1) == ' ') fin--; // sin espacios sobrantes al final de cada línea
        sb.append(l, 0, fin).append('\n');
    }

    private static String recortar(String celda) {
        String limpia = celda.replace('\n', ' ').replace('\t', ' ');
        return limpia.length() <= ANCHO_MAXIMO ? limpia : limpia.substring(0, ANCHO_MAXIMO - 1) + "…";
    }
}
