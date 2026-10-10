package leaf.ide.core.consola;

import java.util.ArrayList;
import java.util.List;

/**
 * Divide una línea de comandos en argumentos. Los espacios separan, las comillas dobles agrupan (y se quitan) y las
 * barras invertidas se conservan tal cual, porque son parte de las rutas de Windows.
 */
public final class LineaDeComandos {

    private LineaDeComandos() {}

    public static List<String> dividir(String linea) {
        List<String> args = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComillas = false;
        boolean hayArgumento = false; // distingue "" (un argumento vacío) de la ausencia de argumento
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                enComillas = !enComillas;
                hayArgumento = true;
            } else if (Character.isWhitespace(c) && !enComillas) {
                if (hayArgumento) args.add(actual.toString());
                actual.setLength(0);
                hayArgumento = false;
            } else {
                actual.append(c);
                hayArgumento = true;
            }
        }
        if (hayArgumento) args.add(actual.toString());
        return args;
    }
}
