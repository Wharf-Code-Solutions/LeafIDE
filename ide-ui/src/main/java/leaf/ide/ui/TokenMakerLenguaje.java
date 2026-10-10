package leaf.ide.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.text.Segment;
import leaf.api.LenguajeDef;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerBase;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

/**
 * Colorea una línea de código según un {@link LenguajeDef}: nada de Leaf está escrito aquí, todo sale de la
 * definición (palabras por categoría, símbolos, comentarios y prefijo de variable).
 *
 * <p>Reglas: las frases de varias palabras ({@code "con valor de"}) son un solo token y la más larga gana; una palabra
 * pegada a letras o dígitos es un identificador; un nombre seguido de {@code (} es una función; una cadena sin
 * cerrar y un prefijo de variable solo son errores.
 */
final class TokenMakerLenguaje extends TokenMakerBase {

    private record Simbolo(String texto, int tipo) {}

    private final LenguajeDef lenguaje;
    private final Map<String, Integer> palabras = new HashMap<>();
    private final List<Simbolo> frases = new ArrayList<>();
    private final List<Simbolo> simbolos = new ArrayList<>();

    TokenMakerLenguaje(LenguajeDef lenguaje) {
        this.lenguaje = lenguaje;
        for (LenguajeDef.GrupoPalabras g : lenguaje.grupos()) {
            int tipo = tipoDeCategoria(g.categoria());
            for (String palabra : g.palabras()) {
                if (palabra.indexOf(' ') >= 0) frases.add(new Simbolo(palabra, tipo));
                else palabras.put(palabra, tipo);
            }
        }
        for (String s : lenguaje.operadores()) simbolos.add(new Simbolo(s, TokenTypes.OPERATOR));
        for (String s : lenguaje.delimitadores()) simbolos.add(new Simbolo(s, TokenTypes.SEPARATOR));
        Comparator<Simbolo> masLargoPrimero = Comparator.comparingInt((Simbolo s) -> s.texto().length()).reversed();
        frases.sort(masLargoPrimero);
        simbolos.sort(masLargoPrimero);
    }

    private static int tipoDeCategoria(String categoria) {
        return switch (categoria) {
            case LenguajeDef.TIPO -> TokenTypes.DATA_TYPE;
            case LenguajeDef.LITERAL -> TokenTypes.LITERAL_BOOLEAN;
            case LenguajeDef.OPERADOR -> TokenTypes.OPERATOR;
            default -> TokenTypes.RESERVED_WORD; // reservada y categorías desconocidas
        };
    }

    @Override
    public Token getTokenList(Segment texto, int tipoInicial, int inicioDocumento) {
        resetTokenList();
        char[] a = texto.array;
        int inicio = texto.offset;
        int fin = inicio + texto.count; // exclusivo
        int base = inicioDocumento - inicio; // offset del documento = base + índice del arreglo

        int i = inicio;
        if (tipoInicial == TokenTypes.COMMENT_MULTILINE) {
            i = comentarioDeBloque(a, inicio, inicio, fin, base);
            if (i < 0) return firstToken; // sigue abierto: el último token es el comentario
        }
        while (i < fin) {
            char c = a[i];
            if (c == ' ' || c == '\t') {
                int j = i + 1;
                while (j < fin && (a[j] == ' ' || a[j] == '\t')) j++;
                agregar(a, i, j, TokenTypes.WHITESPACE, base);
                i = j;
            } else if (empiezaCon(a, i, fin, lenguaje.comentarioLinea())) {
                agregar(a, i, fin, TokenTypes.COMMENT_EOL, base);
                i = fin;
            } else if (empiezaCon(a, i, fin, lenguaje.comentarioBloqueInicio())) {
                i = comentarioDeBloque(a, i, i + lenguaje.comentarioBloqueInicio().length(), fin, base);
                if (i < 0) return firstToken;
            } else if (c == '"') {
                i = cadena(a, i, fin, base);
            } else if (!lenguaje.prefijoVariable().isEmpty() && empiezaCon(a, i, fin, lenguaje.prefijoVariable())) {
                i = variable(a, i, fin, base);
            } else if (Character.isDigit(c)) {
                i = numero(a, i, fin, base);
            } else if (esInicioDeNombre(c)) {
                i = palabraONombre(a, i, fin, base);
            } else {
                i = simbolo(a, i, fin, base);
            }
        }
        addNullToken();
        return firstToken;
    }

    // ---------- piezas ----------

    /** Agrega el comentario de bloque desde {@code ini}; busca el cierre desde {@code buscarDesde}. -1 si sigue abierto. */
    private int comentarioDeBloque(char[] a, int ini, int buscarDesde, int fin, int base) {
        String cierre = lenguaje.comentarioBloqueFin();
        for (int k = buscarDesde; !cierre.isEmpty() && k + cierre.length() <= fin; k++) {
            if (empiezaCon(a, k, fin, cierre)) {
                agregar(a, ini, k + cierre.length(), TokenTypes.COMMENT_MULTILINE, base);
                return k + cierre.length();
            }
        }
        agregar(a, ini, fin, TokenTypes.COMMENT_MULTILINE, base); // también con largo 0 (línea vacía dentro del comentario)
        return -1;
    }

    private int cadena(char[] a, int ini, int fin, int base) {
        int j = ini + 1;
        while (j < fin && a[j] != '"') j += a[j] == '\\' ? 2 : 1;
        if (j >= fin) {
            agregar(a, ini, fin, TokenTypes.ERROR_STRING_DOUBLE, base);
            return fin;
        }
        agregar(a, ini, j + 1, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, base);
        return j + 1;
    }

    private int variable(char[] a, int ini, int fin, int base) {
        int j = ini + lenguaje.prefijoVariable().length();
        if (j >= fin || !esInicioDeNombre(a[j])) {
            agregar(a, ini, j, TokenTypes.ERROR_IDENTIFIER, base);
            return j;
        }
        while (j < fin && esParteDeNombre(a[j])) j++;
        agregar(a, ini, j, TokenTypes.VARIABLE, base);
        return j;
    }

    private int numero(char[] a, int ini, int fin, int base) {
        int j = ini;
        while (j < fin && Character.isDigit(a[j])) j++;
        int tipo = TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
        if (j + 1 < fin && a[j] == '.' && Character.isDigit(a[j + 1])) {
            j++;
            while (j < fin && Character.isDigit(a[j])) j++;
            tipo = TokenTypes.LITERAL_NUMBER_FLOAT;
        }
        agregar(a, ini, j, tipo, base);
        return j;
    }

    private int palabraONombre(char[] a, int ini, int fin, int base) {
        for (Simbolo frase : frases) {
            int largo = frase.texto().length();
            if (empiezaCon(a, ini, fin, frase.texto()) && (ini + largo >= fin || !esParteDeNombre(a[ini + largo]))) {
                agregar(a, ini, ini + largo, frase.tipo(), base);
                return ini + largo;
            }
        }
        int j = ini;
        while (j < fin && esParteDeNombre(a[j])) j++;
        Integer tipo = palabras.get(new String(a, ini, j - ini));
        if (tipo == null) {
            int k = j;
            while (k < fin && (a[k] == ' ' || a[k] == '\t')) k++;
            tipo = k < fin && a[k] == '(' ? TokenTypes.FUNCTION : TokenTypes.IDENTIFIER;
        }
        agregar(a, ini, j, tipo, base);
        return j;
    }

    private int simbolo(char[] a, int ini, int fin, int base) {
        for (Simbolo s : simbolos) {
            if (empiezaCon(a, ini, fin, s.texto())) {
                agregar(a, ini, ini + s.texto().length(), s.tipo(), base);
                return ini + s.texto().length();
            }
        }
        agregar(a, ini, ini + 1, TokenTypes.ERROR_IDENTIFIER, base);
        return ini + 1;
    }

    // ---------- utilidades ----------

    /** Agrega el tramo [ini, finExclusivo) (la API de RSyntaxTextArea usa fin inclusivo). */
    private void agregar(char[] a, int ini, int finExclusivo, int tipo, int base) {
        addToken(a, ini, finExclusivo - 1, tipo, base + ini);
    }

    private static boolean empiezaCon(char[] a, int pos, int fin, String prefijo) {
        if (prefijo.isEmpty() || pos + prefijo.length() > fin) return false;
        for (int k = 0; k < prefijo.length(); k++) {
            if (a[pos + k] != prefijo.charAt(k)) return false;
        }
        return true;
    }

    private static boolean esInicioDeNombre(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean esParteDeNombre(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    // ---------- comportamiento del editor ----------

    @Override
    public String[] getLineCommentStartAndEnd(int indiceDeLenguaje) {
        String marca = lenguaje.comentarioLinea();
        return marca.isEmpty() ? null : new String[] {marca, null};
    }

    @Override
    public boolean isIdentifierChar(int indiceDeLenguaje, char c) {
        return esParteDeNombre(c) || lenguaje.prefijoVariable().indexOf(c) >= 0;
    }

    @Override
    public boolean getMarkOccurrencesOfTokenType(int tipo) {
        return tipo == TokenTypes.IDENTIFIER || tipo == TokenTypes.VARIABLE || tipo == TokenTypes.FUNCTION;
    }
}
