package leaf.adapter.v1;

import java.util.Set;

/**
 * Única clase que decide la categoría de un token (columna "Categoría" de la tabla léxica).
 * En M4 se reemplazará por lo que defina {@code leaf-lenguaje.json}.
 */
public final class CategoriasToken {

    public static final String PALABRA_RESERVADA = "Palabra reservada";
    public static final String TIPO_DATO = "Tipo de dato";
    public static final String LITERAL = "Literal";
    public static final String IDENTIFICADOR = "Identificador";
    public static final String OPERADOR = "Operador";
    public static final String DELIMITADOR = "Delimitador";
    public static final String ERROR = "Error léxico";
    public static final String OTRO = "Otro";

    private static final Set<String> TIPOS = Set.of(
            "ENTERO_T", "DECIMAL_T", "TEXTO_T", "LOGICO_T", "ARREGLO_T", "VACIO_T");
    private static final Set<String> LITERALES = Set.of(
            "NUMERO", "DEC", "CADENA", "SI_LIT", "NO_LIT");
    private static final Set<String> IDENTIFICADORES = Set.of("VARIABLE", "ID");
    private static final Set<String> OPERADORES = Set.of(
            "MAS", "MENOS", "POR", "DIV", "POT", "RAIZ", "MOD",
            "Y", "O", "NO", "IGUAL", "DIF", "MAYOR_Q", "MENOR_Q", "MAYOR_IQ", "MENOR_IQ", "VALE", "FLECHA");
    private static final Set<String> DELIMITADORES = Set.of(
            "PUNTO", "PAREN_A", "PAREN_C", "ABRE_B", "CIERRA_B", "CORCH_A", "CORCH_C", "COMA", "NUEVA_LINEA");
    private static final Set<String> ERRORES = Set.of(
            "ERROR_LEXICO", "DOLAR_SUELTO", "CADENA_ESCAPE_INVALIDA");

    private CategoriasToken() {}

    /**
     * @param nombreTipo nombre de la constante en LeafConstants (p. ej. "VARIABLE")
     * @param imagen     imagen de la gramática (p. ej. "\"definir\""), puede ser null
     */
    public static String categoria(String nombreTipo, String imagen) {
        if (nombreTipo == null) return OTRO;
        if (TIPOS.contains(nombreTipo)) return TIPO_DATO;
        if (LITERALES.contains(nombreTipo)) return LITERAL;
        if (IDENTIFICADORES.contains(nombreTipo)) return IDENTIFICADOR;
        if (OPERADORES.contains(nombreTipo)) return OPERADOR;
        if (DELIMITADORES.contains(nombreTipo)) return DELIMITADOR;
        if (ERRORES.contains(nombreTipo)) return ERROR;
        // Respaldo para tokens nuevos de la gramática: una imagen literal con letras es palabra reservada.
        if (imagen != null && imagen.matches("\"[\\p{L} ]+\"")) return PALABRA_RESERVADA;
        return OTRO;
    }
}
