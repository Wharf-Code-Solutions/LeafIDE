package leaf.ide.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Qué muestra el explorador de archivos: las carpetas (primero) y los programas Leaf ({@code .leaf} y {@code .txt}),
 * cada grupo ordenado sin distinguir mayúsculas. Ocultas las carpetas que empiezan con punto ({@code .git}) y las de
 * compilación ({@code target}, {@code out}). Sin Swing, para poder probarlo.
 */
public final class ListadoCarpeta {

    private static final Set<String> CARPETAS_OCULTAS = Set.of("target", "out");

    private ListadoCarpeta() {}

    public static boolean esArchivoLeaf(Path archivo) {
        String nombre = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        return nombre.endsWith(".leaf") || nombre.endsWith(".txt");
    }

    /** Contenido visible de una carpeta; vacío si no existe, no se puede leer o no es una carpeta. */
    public static List<Path> hijos(Path carpeta) {
        if (!Files.isDirectory(carpeta)) return List.of();
        List<Path> carpetas = new ArrayList<>();
        List<Path> archivos = new ArrayList<>();
        try (Stream<Path> flujo = Files.list(carpeta)) {
            flujo.forEach(p -> {
                if (Files.isDirectory(p)) {
                    if (!oculta(p)) carpetas.add(p);
                } else if (esArchivoLeaf(p)) {
                    archivos.add(p);
                }
            });
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        Comparator<Path> porNombre = Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT));
        carpetas.sort(porNombre);
        archivos.sort(porNombre);
        carpetas.addAll(archivos);
        return carpetas;
    }

    /** ¿Hay algo que mostrar dentro de esa carpeta? (decide si el árbol dibuja la flecha de expandir) */
    public static boolean tieneContenido(Path carpeta) {
        return !hijos(carpeta).isEmpty();
    }

    private static boolean oculta(Path carpeta) {
        String nombre = carpeta.getFileName() == null ? "" : carpeta.getFileName().toString();
        return nombre.startsWith(".") || CARPETAS_OCULTAS.contains(nombre.toLowerCase(Locale.ROOT));
    }
}
