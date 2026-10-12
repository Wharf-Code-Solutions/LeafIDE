package leaf.ide.core.consola;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Reconoce, en lo que se escribe en la terminal Sistema, el comando del manual
 * {@code java generado.Leaf <archivo> [--guardar] [--tres-dir]} para que el IDE analice ese mismo archivo. Solo mira el
 * texto de la línea: un {@code .bat}, un alias o una tubería que luego llamen a Java no se detectan.
 */
public final class ComandoJavaLeaf {

    private static final String CLASE = "generado.Leaf";
    /** Opciones de {@code java} que llevan su valor en el argumento siguiente. */
    private static final Set<String> CON_VALOR = Set.of("-cp", "-classpath", "--class-path", "-p", "--module-path",
            "--add-modules", "--add-opens", "--add-exports", "--add-reads", "--upgrade-module-path");

    private ComandoJavaLeaf() {}

    /**
     * @param linea     lo que el usuario escribió
     * @param directorio carpeta actual de la terminal (contra ella se resuelve una ruta relativa)
     * @return el archivo que recibe el compilador, o vacío si la línea no es ese comando
     */
    public static Optional<Path> reconocer(String linea, Path directorio) {
        List<String> partes = LineaDeComandos.dividir(linea == null ? "" : linea);
        if (partes.isEmpty() || !esJava(partes.get(0))) return Optional.empty();

        int i = 1;
        while (i < partes.size() && partes.get(i).startsWith("-")) {
            String opcion = partes.get(i);
            if (opcion.equals("-jar")) return Optional.empty();
            i += CON_VALOR.contains(opcion) ? 2 : 1;
        }
        if (i >= partes.size() || !partes.get(i).equals(CLASE)) return Optional.empty();

        for (int j = i + 1; j < partes.size(); j++) {
            String arg = partes.get(j);
            if (arg.startsWith("--")) continue; // --guardar, --tres-dir…
            try {
                return Optional.of(directorio.resolve(arg).normalize());
            } catch (InvalidPathException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static boolean esJava(String programa) {
        String nombre = programa.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return nombre.equals("java") || nombre.equals("java.exe");
    }
}
