package leaf.ide.core;

import java.util.Map;

/**
 * Nombre de una fase tal como se muestra al usuario (con acento). Los ids de {@code CONTRATOS.md} van sin acento
 * porque son identificadores; aquí se traducen. Un id desconocido se muestra con inicial mayúscula.
 */
public final class NombresFase {

    private static final Map<String, String> CONOCIDAS = Map.of(
            "lexico", "Léxico",
            "sintactico", "Sintáctico",
            "semantico", "Semántico",
            "intermedio", "Intermedio",
            "optimizacion", "Optimización",
            "objeto", "Objeto",
            "interno", "Interno");

    private NombresFase() {}

    public static String de(String faseId) {
        String conocida = CONOCIDAS.get(faseId);
        if (conocida != null) return conocida;
        return faseId.isEmpty() ? faseId : Character.toUpperCase(faseId.charAt(0)) + faseId.substring(1);
    }
}
