package leaf.ide.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Carga el compilador desde los {@code .class} de una carpeta del disco (la del compilador base, por ejemplo
 * {@code C:\Leaf\src}, con su paquete {@code generado}), igual que haría {@code java generado.Leaf} en esa carpeta.
 * <ul>
 *   <li>{@code generado.*}: solo de la carpeta. Si falta un archivo (porque se borró o porque falta compilar con
 *       {@code javac}), la clase no existe y el IDE lo informa: nunca se rellena con la copia del IDE.</li>
 *   <li>Las dos clases propias del IDE ({@code ServicioCompilacion} y {@code ResultadoCompilacion}) y el adaptador
 *       ({@code leaf.adapter.v1.*}) se definen otra vez aquí, con los bytes de la copia del IDE, para que trabajen con
 *       las clases de la carpeta (comparten paquete con {@code Leaf} y usan sus miembros no públicos).</li>
 *   <li>Todo lo demás (la API, Java, Gson…) viene del cargador del IDE, así los resultados se entienden fuera.</li>
 * </ul>
 * Un cargador nuevo por cada versión de la carpeta: si cambian sus {@code .class}, se crea otro.
 */
final class CargadorCompiladorExterno extends ClassLoader {

    private static final String PAQUETE_COMPILADOR = "generado.";
    private static final String PAQUETE_ADAPTADOR = "leaf.adapter.v1.";
    /** Clases del paquete {@code generado} que no son del compilador sino del IDE (no están en el repo Leaf). */
    private static final Set<String> PROPIAS_DEL_IDE = Set.of("generado.ServicioCompilacion", "generado.ResultadoCompilacion");

    private final Path clases;

    /** @param clases carpeta que contiene {@code generado/Leaf.class} */
    CargadorCompiladorExterno(Path clases, ClassLoader delIde) {
        super("compilador-externo", delIde);
        this.clases = clases;
    }

    Path clases() {
        return clases;
    }

    @Override
    protected Class<?> loadClass(String nombre, boolean resolver) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(nombre)) {
            Class<?> c = findLoadedClass(nombre);
            if (c == null) {
                if (esDelIde(nombre)) {
                    c = definir(nombre, bytesDelIde(nombre));
                } else if (nombre.startsWith(PAQUETE_COMPILADOR)) {
                    c = definir(nombre, bytesDeLaCarpeta(nombre));
                } else {
                    return super.loadClass(nombre, resolver);
                }
            }
            if (resolver) resolveClass(c);
            return c;
        }
    }

    private static boolean esDelIde(String nombre) {
        if (nombre.startsWith(PAQUETE_ADAPTADOR)) return true;
        String exterior = nombre.contains("$") ? nombre.substring(0, nombre.indexOf('$')) : nombre;
        return PROPIAS_DEL_IDE.contains(exterior);
    }

    private Class<?> definir(String nombre, byte[] bytes) {
        return defineClass(nombre, bytes, 0, bytes.length);
    }

    private static String archivo(String nombre) {
        return nombre.replace('.', '/') + ".class";
    }

    private byte[] bytesDelIde(String nombre) throws ClassNotFoundException {
        try (InputStream in = getParent().getResourceAsStream(archivo(nombre))) {
            if (in == null) throw new ClassNotFoundException(nombre);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new ClassNotFoundException(nombre, e);
        }
    }

    private byte[] bytesDeLaCarpeta(String nombre) throws ClassNotFoundException {
        Path f = clases.resolve(archivo(nombre));
        try {
            return Files.readAllBytes(f);
        } catch (IOException e) {
            throw new ClassNotFoundException("No está " + f + " (¿se borró o falta compilar con javac?)", e);
        }
    }
}
