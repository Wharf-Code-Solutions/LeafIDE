package leaf.ide.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Dónde están las clases del compilador dentro de la carpeta elegida y de qué commit de Git son. */
final class UbicacionCompilador {

    private UbicacionCompilador() {}

    /**
     * La carpeta que contiene {@code generado/}: la elegida si ya la tiene ({@code C:\Leaf\src}), o su {@code src}
     * ({@code C:\Leaf}, como está organizado el repo Leaf).
     */
    static Path clases(Path elegida) {
        Path normal = elegida.toAbsolutePath().normalize();
        if (Files.isDirectory(normal.resolve("generado"))) return normal;
        Path src = normal.resolve("src");
        if (Files.isDirectory(src.resolve("generado")) || Files.isDirectory(src)) return src;
        return normal;
    }

    /** Los 7 primeros caracteres del commit en el que está el repo Git que contiene esa carpeta, si lo hay. */
    static Optional<String> commit(Path desde) {
        Path dir = desde.toAbsolutePath().normalize();
        for (int i = 0; i < 4 && dir != null; i++, dir = dir.getParent()) {
            Path git = dir.resolve(".git");
            if (Files.isDirectory(git)) return commitDe(git);
        }
        return Optional.empty();
    }

    private static Optional<String> commitDe(Path git) {
        String head = ProveedorConmutable.leer(git.resolve("HEAD"));
        String hash = head;
        if (head.startsWith("ref:")) {
            String ref = head.substring(4).strip();
            hash = ProveedorConmutable.leer(git.resolve(ref));
            if (hash.isEmpty()) hash = buscarEnPacked(git, ref);
        }
        return hash.length() >= 7 && hash.chars().allMatch(c -> Character.digit(c, 16) >= 0)
                ? Optional.of(hash.substring(0, 7)) : Optional.empty();
    }

    private static String buscarEnPacked(Path git, String ref) {
        for (String linea : ProveedorConmutable.leer(git.resolve("packed-refs")).split("\\R")) {
            String[] partes = linea.strip().split(" ");
            if (partes.length == 2 && partes[1].equals(ref)) return partes[0];
        }
        return "";
    }
}
