package leaf.ide.core.consola;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Terminal del sistema: cada línea se ejecuta como un comando del sistema ({@code cmd /c}) en la carpeta actual, y
 * mientras un proceso siga en marcha, lo que se escriba va a su entrada. No es una terminal completa (no hay seudo
 * consola): los comandos y programas de línea de comandos funcionan; los de pantalla completa, no.
 *
 * <p>{@code cd} lo atiende esta clase para mantener la carpeta entre comandos. Los programas de Java imprimen en UTF-8
 * (se les fuerza con {@code JAVA_TOOL_OPTIONS}) y el resto se decodifica con {@link DecodificadorSalida}.
 */
public final class TerminalSistema {

    /** Quien recibe lo que imprime el proceso. Se llama desde hilos de lectura: la UI debe pasarlo a su hilo. */
    public interface Escucha {
        void salida(String texto);

        /** El proceso terminó (con su código de salida); no se llama para los comandos que atiende la terminal misma. */
        void terminado(int codigoDeSalida);
    }

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final String OPCIONES_UTF8 = "-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8";
    private static final Pattern UNIDAD = Pattern.compile("^[A-Za-z]:$");

    private final Map<String, String> entornoExtra;
    private final Escucha escucha;
    private final Charset oem;
    private final Charset paraLaEntrada;
    private volatile Path directorio;
    private volatile Process proceso;

    /**
     * @param directorioInicial carpeta donde empieza la terminal
     * @param entornoExtra      variables de entorno que se añaden o reemplazan en cada proceso (por ejemplo CLASSPATH)
     * @param oem               página de códigos de los comandos internos de cmd (ver {@link #oemDelSistema()})
     */
    public TerminalSistema(Path directorioInicial, Map<String, String> entornoExtra, Escucha escucha, Charset oem) {
        this.directorio = directorioInicial.toAbsolutePath().normalize();
        this.entornoExtra = Map.copyOf(entornoExtra);
        this.escucha = escucha;
        this.oem = oem;
        String nativa = System.getProperty("native.encoding");
        this.paraLaEntrada = nativa != null ? Charset.forName(nativa) : Charset.defaultCharset();
    }

    public Path directorio() {
        return directorio;
    }

    /**
     * Mueve la terminal a otra carpeta desde fuera (por ejemplo, al abrir otra carpeta de trabajo).
     * @return {@code false} si no se movió: hay un programa en marcha o la ruta no es una carpeta
     */
    public boolean cambiarDirectorio(Path nuevo) {
        if (enEjecucion() || nuevo == null || !Files.isDirectory(nuevo)) return false;
        directorio = nuevo.toAbsolutePath().normalize();
        return true;
    }

    public boolean enEjecucion() {
        Process p = proceso;
        return p != null && p.isAlive();
    }

    /** Ejecuta la línea como comando, o la entrega a la entrada del proceso que esté en marcha. */
    public void enviar(String linea) {
        Process activo = proceso;
        if (activo != null && activo.isAlive()) {
            escribirEntrada(activo, linea);
            return;
        }
        if (linea == null || linea.isBlank()) return;

        List<String> args = LineaDeComandos.dividir(linea);
        String primero = args.get(0).toLowerCase(Locale.ROOT);
        if (primero.equals("cd") || primero.equals("chdir")) {
            cambiarDirectorio(args);
        } else if (args.size() == 1 && UNIDAD.matcher(args.get(0)).matches()) {
            cambiarDeUnidad(args.get(0));
        } else {
            iniciar(linea);
        }
    }

    /** Corta el proceso en marcha (y los que lanzó), como Ctrl+C. */
    public void detener() {
        Process p = proceso;
        if (p == null) return;
        p.descendants().forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
    }

    public void cerrar() {
        detener();
    }

    /** Página de códigos que usan los comandos internos de cmd (la de la consola, p. ej. 850), según {@code chcp}. */
    public static Charset oemDelSistema() {
        if (!WINDOWS) return Charset.defaultCharset();
        try {
            Process p = new ProcessBuilder("cmd.exe", "/c", "chcp").redirectErrorStream(true).start();
            String texto = new String(p.getInputStream().readAllBytes(), Charset.defaultCharset());
            p.waitFor();
            Matcher m = Pattern.compile("(\\d+)\\s*$").matcher(texto.strip());
            if (m.find()) {
                for (String prefijo : new String[] {"cp", "IBM", "windows-"}) {
                    try {
                        return Charset.forName(prefijo + m.group(1));
                    } catch (RuntimeException e) {
                        // probar con el siguiente nombre
                    }
                }
            }
        } catch (IOException e) {
            // se usa la predeterminada
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Charset.defaultCharset();
    }

    // ---------- comandos que atiende la terminal ----------

    private void cambiarDirectorio(List<String> args) {
        List<String> rutas = new ArrayList<>(args.subList(1, args.size()));
        rutas.removeIf(a -> a.equalsIgnoreCase("/d"));
        if (rutas.isEmpty()) {
            escucha.salida(directorio + System.lineSeparator());
            return;
        }
        try {
            Path destino = directorio.resolve(String.join(" ", rutas)).normalize();
            if (Files.isDirectory(destino)) {
                directorio = destino;
                return;
            }
        } catch (InvalidPathException e) {
            // cae en el mensaje de ruta no encontrada
        }
        escucha.salida("El sistema no puede encontrar la ruta especificada." + System.lineSeparator());
    }

    private void cambiarDeUnidad(String unidad) {
        Path raiz = Path.of(unidad.toUpperCase(Locale.ROOT) + "\\");
        if (Files.isDirectory(raiz)) directorio = raiz;
        else escucha.salida("El sistema no puede encontrar la unidad especificada." + System.lineSeparator());
    }

    // ---------- procesos ----------

    private void iniciar(String linea) {
        List<String> orden = WINDOWS ? List.of("cmd.exe", "/c", linea) : List.of("sh", "-c", linea);
        ProcessBuilder pb = new ProcessBuilder(orden).directory(directorio.toFile()).redirectErrorStream(true);
        Map<String, String> entorno = pb.environment();
        String previas = entorno.getOrDefault("JAVA_TOOL_OPTIONS", "");
        entorno.put("JAVA_TOOL_OPTIONS", previas.isBlank() ? OPCIONES_UTF8 : previas + " " + OPCIONES_UTF8);
        entorno.putAll(new HashMap<>(entornoExtra));

        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            escucha.salida("No se pudo ejecutar el comando: " + e.getMessage() + System.lineSeparator());
            escucha.terminado(-1);
            return;
        }
        proceso = p;
        Thread lector = new Thread(() -> leerHastaElFinal(p), "LectorDeSalida");
        lector.setDaemon(true);
        lector.start();
    }

    private void leerHastaElFinal(Process p) {
        DecodificadorSalida decodificador = new DecodificadorSalida(oem);
        byte[] buffer = new byte[4096];
        try (InputStream in = p.getInputStream()) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                String texto = decodificador.alimentar(buffer, n);
                if (!texto.isEmpty()) escucha.salida(texto);
            }
        } catch (IOException e) {
            // el proceso se cerró: se entrega lo que quedaba
        }
        String resto = decodificador.terminar();
        if (!resto.isEmpty()) escucha.salida(resto);
        int codigo;
        try {
            codigo = p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            codigo = -1;
        }
        if (proceso == p) proceso = null;
        escucha.terminado(codigo);
    }

    private void escribirEntrada(Process p, String linea) {
        try {
            OutputStream out = p.getOutputStream();
            out.write((linea + System.lineSeparator()).getBytes(paraLaEntrada));
            out.flush();
        } catch (IOException e) {
            // el proceso ya terminó
        }
    }
}
