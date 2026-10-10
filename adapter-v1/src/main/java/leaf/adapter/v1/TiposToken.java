package leaf.adapter.v1;

import generado.LeafConstants;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Nombre del tipo de token ({@code VARIABLE}, {@code PUNTO}, ...) a partir de su {@code kind}.
 * Se lee por reflexión de {@link LeafConstants} para tolerar cambios en la gramática sin tocar el IDE.
 */
final class TiposToken {

    /** Constantes de LeafConstants que son estados léxicos, no tipos de token (sus valores chocan con 0, 1, 2). */
    private static final Set<String> ESTADOS_LEXICOS = Set.of("DEFAULT", "IN_BLOCK_COMMENT", "IN_CADENA");

    private static final Map<Integer, String> NOMBRES = cargar();

    private TiposToken() {}

    static String nombre(int kind) {
        String n = NOMBRES.get(kind);
        return n != null ? n : "TOKEN_" + kind;
    }

    /** Imagen literal de la gramática ("\"definir\"", "<VARIABLE>") o null. */
    static String imagen(int kind) {
        return kind >= 0 && kind < LeafConstants.tokenImage.length ? LeafConstants.tokenImage[kind] : null;
    }

    private static Map<Integer, String> cargar() {
        Map<Integer, String> mapa = new HashMap<>();
        for (Field f : LeafConstants.class.getFields()) {
            if (f.getType() != int.class || !Modifier.isStatic(f.getModifiers())) continue;
            if (ESTADOS_LEXICOS.contains(f.getName())) continue;
            try {
                mapa.putIfAbsent(f.getInt(null), f.getName());
            } catch (IllegalAccessException e) {
                // Constante inaccesible: se usará TOKEN_<kind>.
            }
        }
        return mapa;
    }
}
