package leaf.ide.ui;

import static org.fife.ui.rsyntaxtextarea.TokenTypes.COMMENT_EOL;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.COMMENT_MULTILINE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.DATA_TYPE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.ERROR_IDENTIFIER;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.ERROR_STRING_DOUBLE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.FUNCTION;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.IDENTIFIER;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_BOOLEAN;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_NUMBER_FLOAT;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.NULL;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.OPERATOR;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.RESERVED_WORD;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.SEPARATOR;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.VARIABLE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.WHITESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import javax.swing.text.Segment;
import leaf.api.LenguajeDef;
import leaf.api.LenguajeDef.GrupoPalabras;
import org.fife.ui.rsyntaxtextarea.Token;
import org.junit.jupiter.api.Test;

class TokenMakerLenguajeTest {

    /** Un extracto de Leaf: lo justo para probar cada tipo de token. */
    private static final LenguajeDef LEAF = new LenguajeDef("Leaf", List.of("leaf"),
            List.of(new GrupoPalabras(LenguajeDef.RESERVADA,
                            List.of("definir", "como", "con valor de", "mostrar", "funcion", "si", "sino si", "sino", "de")),
                    new GrupoPalabras(LenguajeDef.TIPO, List.of("entero", "decimal")),
                    new GrupoPalabras(LenguajeDef.LITERAL, List.of("verdadero", "falso")),
                    new GrupoPalabras(LenguajeDef.OPERADOR, List.of("Y", "mayor que", "mayor o igual que")),
                    new GrupoPalabras("categoria-inventada", List.of("raro"))),
            List.of("+", "-", "/", "-/", "->"), List.of(".", "(", ")", ">", "<", ","),
            "//", "/*", "*/", "$");

    private final TokenMakerLenguaje maker = new TokenMakerLenguaje(LEAF);

    /** Tokens "tipo:texto" de una línea (sin el token nulo del final). */
    private List<String> tokens(String linea, int tipoInicial) {
        Segment s = new Segment(linea.toCharArray(), 0, linea.length());
        List<String> salida = new ArrayList<>();
        for (Token t = maker.getTokenList(s, tipoInicial, 0); t != null && t.isPaintable(); t = t.getNextToken()) {
            salida.add(t.getType() + ":" + t.getLexeme());
        }
        return salida;
    }

    private List<String> tokens(String linea) {
        return tokens(linea, NULL);
    }

    private int ultimoTipo(String linea, int tipoInicial) {
        Segment s = new Segment(linea.toCharArray(), 0, linea.length());
        return maker.getLastTokenTypeOnLine(s, tipoInicial);
    }

    private static String t(int tipo, String texto) {
        return tipo + ":" + texto;
    }

    @Test
    void declaracionCompleta() {
        assertEquals(List.of(
                t(RESERVED_WORD, "definir"), t(WHITESPACE, " "), t(VARIABLE, "$x"), t(WHITESPACE, " "),
                t(RESERVED_WORD, "como"), t(WHITESPACE, " "), t(DATA_TYPE, "entero"), t(WHITESPACE, " "),
                t(RESERVED_WORD, "con valor de"), t(WHITESPACE, " "), t(LITERAL_NUMBER_DECIMAL_INT, "1"),
                t(SEPARATOR, ".")), tokens("definir $x como entero con valor de 1."));
    }

    @Test
    void frasesDeVariasPalabrasSonUnSoloToken() {
        assertEquals(List.of(t(RESERVED_WORD, "sino si"), t(SEPARATOR, "(")), tokens("sino si("));
        assertEquals(List.of(t(RESERVED_WORD, "sino")), tokens("sino"));
        assertEquals(List.of(t(OPERATOR, "mayor o igual que")), tokens("mayor o igual que"));
        // La frase más larga gana sobre su prefijo.
        assertEquals(List.of(t(OPERATOR, "mayor que")), tokens("mayor que"));
    }

    @Test
    void fraseSinFinDePalabraNoCuenta() {
        // "sino sip" no es "sino si": queda "sino", espacio e identificador.
        assertEquals(List.of(t(RESERVED_WORD, "sino"), t(WHITESPACE, " "), t(IDENTIFIER, "sip")), tokens("sino sip"));
    }

    @Test
    void palabrasPegadasAUnIdentificadorNoSonReservadas() {
        assertEquals(List.of(t(IDENTIFIER, "sinosi")), tokens("sinosi"));
        assertEquals(List.of(t(IDENTIFIER, "entero2")), tokens("entero2"));
        assertEquals(List.of(t(IDENTIFIER, "mayor")), tokens("mayor"));
    }

    @Test
    void mayusculasSeRespetan() {
        assertEquals(List.of(t(OPERATOR, "Y")), tokens("Y"));
        assertEquals(List.of(t(IDENTIFIER, "y")), tokens("y"));
    }

    @Test
    void literalesBooleanosYCategoriaDesconocida() {
        assertEquals(List.of(t(LITERAL_BOOLEAN, "verdadero")), tokens("verdadero"));
        assertEquals(List.of(t(RESERVED_WORD, "raro")), tokens("raro"));
    }

    @Test
    void nombreSeguidoDeParentesisEsFuncionPeroUnaReservadaNo() {
        assertEquals(List.of(t(RESERVED_WORD, "funcion"), t(WHITESPACE, " "), t(FUNCTION, "area"), t(SEPARATOR, "(")),
                tokens("funcion area("));
        assertEquals(List.of(t(FUNCTION, "area"), t(WHITESPACE, " "), t(SEPARATOR, "(")), tokens("area ("));
        assertEquals(List.of(t(RESERVED_WORD, "mostrar"), t(SEPARATOR, "(")), tokens("mostrar("));
    }

    @Test
    void numerosYPunto() {
        assertEquals(List.of(t(LITERAL_NUMBER_FLOAT, "3.14")), tokens("3.14"));
        assertEquals(List.of(t(LITERAL_NUMBER_DECIMAL_INT, "3"), t(SEPARATOR, ".")), tokens("3."));
        assertEquals(List.of(t(LITERAL_NUMBER_DECIMAL_INT, "42")), tokens("42"));
    }

    @Test
    void operadoresSimbolicosUsanElMasLargo() {
        assertEquals(List.of(t(OPERATOR, "-/")), tokens("-/"));
        assertEquals(List.of(t(OPERATOR, "->")), tokens("->"));
        assertEquals(List.of(t(OPERATOR, "-"), t(LITERAL_NUMBER_DECIMAL_INT, "1")), tokens("-1"));
        assertEquals(List.of(t(OPERATOR, "/")), tokens("/"));
    }

    @Test
    void llavesDeBloqueSonSeparadores() {
        assertEquals(List.of(t(SEPARATOR, ">"), t(SEPARATOR, "<")), tokens("><"));
    }

    @Test
    void cadenasConEscapesYSinCerrar() {
        assertEquals(List.of(t(LITERAL_STRING_DOUBLE_QUOTE, "\"Hola, mundo\"")), tokens("\"Hola, mundo\""));
        assertEquals(List.of(t(LITERAL_STRING_DOUBLE_QUOTE, "\"a\\\"b\"")), tokens("\"a\\\"b\""));
        assertEquals(List.of(t(ERROR_STRING_DOUBLE, "\"sin cerrar")), tokens("\"sin cerrar"));
        // Dentro de una cadena nada es palabra reservada ni comentario.
        assertEquals(List.of(t(LITERAL_STRING_DOUBLE_QUOTE, "\"si // /* definir\"")), tokens("\"si // /* definir\""));
    }

    @Test
    void variablesYDolarSuelto() {
        assertEquals(List.of(t(VARIABLE, "$área_1")), tokens("$área_1"));
        assertEquals(List.of(t(ERROR_IDENTIFIER, "$"), t(WHITESPACE, " "), t(IDENTIFIER, "x")), tokens("$ x"));
        assertEquals(List.of(t(ERROR_IDENTIFIER, "$"), t(LITERAL_NUMBER_DECIMAL_INT, "1")), tokens("$1"));
    }

    @Test
    void identificadoresConAcentosYEnies() {
        assertEquals(List.of(t(IDENTIFIER, "ñandú")), tokens("ñandú"));
    }

    @Test
    void comentarioDeLinea() {
        assertEquals(List.of(t(COMMENT_EOL, "// hola si definir")), tokens("// hola si definir"));
        assertEquals(List.of(t(IDENTIFIER, "x"), t(WHITESPACE, " "), t(COMMENT_EOL, "// c")), tokens("x // c"));
    }

    @Test
    void comentarioDeBloqueEnUnaLinea() {
        assertEquals(List.of(t(COMMENT_MULTILINE, "/* a */"), t(WHITESPACE, " "), t(RESERVED_WORD, "si")),
                tokens("/* a */ si"));
        assertEquals(NULL, ultimoTipo("/* a */ si", NULL));
        assertEquals(NULL, ultimoTipo("/* a */", NULL));
    }

    @Test
    void comentarioDeBloqueEnVariasLineas() {
        assertEquals(List.of(t(COMMENT_MULTILINE, "/* uno")), tokens("/* uno"));
        assertEquals(COMMENT_MULTILINE, ultimoTipo("/* uno", NULL));

        assertEquals(List.of(t(COMMENT_MULTILINE, "dos si definir")), tokens("dos si definir", COMMENT_MULTILINE));
        assertEquals(COMMENT_MULTILINE, ultimoTipo("dos si definir", COMMENT_MULTILINE));

        // Una línea vacía dentro del comentario no lo cierra.
        assertEquals(COMMENT_MULTILINE, ultimoTipo("", COMMENT_MULTILINE));

        assertEquals(List.of(t(COMMENT_MULTILINE, "tres */"), t(WHITESPACE, " "), t(RESERVED_WORD, "si")),
                tokens("tres */ si", COMMENT_MULTILINE));
        assertEquals(NULL, ultimoTipo("tres */ si", COMMENT_MULTILINE));
    }

    @Test
    void caracterDesconocidoEsUnErrorDeUnCaracter() {
        assertEquals(List.of(t(ERROR_IDENTIFIER, "@"), t(IDENTIFIER, "x")), tokens("@x"));
    }

    @Test
    void lineaVaciaNoDaTokensVisibles() {
        assertEquals(List.of(), tokens(""));
    }

    @Test
    void marcadorDeComentarioDeLineaParaElEditor() {
        assertEquals("//", maker.getLineCommentStartAndEnd(0)[0]);
    }
}
