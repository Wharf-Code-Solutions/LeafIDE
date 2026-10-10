package leaf.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Copias inmutables que toleran {@code null} (la lista nula se vuelve vacía; los elementos nulos se conservan). */
final class Copias {
    private Copias() {}

    static <T> List<T> lista(List<T> origen) {
        if (origen == null || origen.isEmpty()) return List.of();
        return Collections.unmodifiableList(new ArrayList<>(origen));
    }

    static <K, V> Map<K, V> mapa(Map<K, V> origen) {
        if (origen == null || origen.isEmpty()) return Map.of();
        return Collections.unmodifiableMap(new LinkedHashMap<>(origen));
    }
}
