package leaf.ide.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.LenguajeDef;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;
import leaf.ide.core.SelectorCompilador;

/**
 * Proveedor que el IDE ve como uno solo, pero que en cada compilación usa el compilador elegido: el de una carpeta del
 * disco (por defecto {@code C:\Leaf}, el compilador base) o la copia que el IDE lleva dentro. Las fases son siempre
 * las mismas (las del adaptador), así la ventana no cambia al cambiar de compilador.
 * <p>
 * Con el de la carpeta, cada compilación revisa si cambiaron sus {@code .class}; si cambiaron (o se borró alguno), se
 * vuelve a cargar. Si no se puede cargar, la primera fase termina en «fallo interno» con el motivo y las demás se
 * omiten: el IDE nunca esconde el problema usando su copia por su cuenta.
 */
final class ProveedorConmutable implements ProveedorCompilador, SelectorCompilador {

    private static final String CLASE_PROVEEDOR = "leaf.adapter.v1.ProveedorLeafV1";

    private final ProveedorCompilador delIde;
    private final List<FaseCompilacion> fases;
    private volatile boolean usaCarpeta;
    private volatile Path carpeta;
    private Externo externo; // el último compilador cargado de la carpeta (o el motivo por el que no se pudo)

    /** Lo que se cargó de la carpeta: el proveedor, o el error; y la «huella» de sus .class para notar cambios. */
    private record Externo(String huella, Path clases, ProveedorCompilador proveedor, String error) {}

    ProveedorConmutable(ProveedorCompilador delIde, boolean usaCarpeta, Path carpeta) {
        this.delIde = delIde;
        this.usaCarpeta = usaCarpeta;
        this.carpeta = carpeta;
        List<FaseCompilacion> envueltas = new ArrayList<>();
        for (FaseCompilacion f : delIde.fases()) envueltas.add(new FaseConmutable(f));
        this.fases = List.copyOf(envueltas);
    }

    // ---------- ProveedorCompilador ----------

    @Override public String nombre() { return delIde.nombre(); }
    @Override public int apiVersion() { return delIde.apiVersion(); }
    @Override public List<FaseCompilacion> fases() { return fases; }
    @Override public LenguajeDef lenguaje() { return delIde.lenguaje(); }

    /** Versión del compilador en uso, para la barra de estado y el comando {@code version}. */
    @Override public String version() { return descripcion(); }

    // ---------- SelectorCompilador ----------

    @Override public boolean usaCarpeta() { return usaCarpeta; }
    @Override public Path carpeta() { return carpeta; }

    @Override
    public synchronized void usarCarpeta(Path nueva) {
        carpeta = nueva;
        usaCarpeta = true;
        externo = null;
    }

    @Override
    public synchronized void usarCopiaDelIde() {
        usaCarpeta = false;
    }

    @Override
    public String descripcion() {
        if (!usaCarpeta) return delIde.version() + " · copia del IDE";
        Path clases = UbicacionCompilador.clases(carpeta);
        return UbicacionCompilador.commit(clases).map(c -> c + " · ").orElse("") + clases;
    }

    // ---------- elegir el compilador de cada compilación ----------

    /** El proveedor que debe compilar ahora, o un texto con el motivo por el que el de la carpeta no se puede usar. */
    private synchronized Object actual() {
        if (!usaCarpeta) return delIde;
        Path clases = UbicacionCompilador.clases(carpeta);
        String huella = huella(clases);
        if (externo == null || !externo.huella().equals(huella) || !externo.clases().equals(clases)) {
            externo = cargar(clases, huella);
        }
        return externo.proveedor() != null ? externo.proveedor() : externo.error();
    }

    private Externo cargar(Path clases, String huella) {
        if (!Files.isDirectory(clases.resolve("generado"))) {
            return new Externo(huella, clases, null, "No se encuentra el compilador: no existe la carpeta "
                    + clases.resolve("generado") + ". Elige su carpeta en Compilar → Compilador, o usa la copia del IDE.");
        }
        CargadorCompiladorExterno cargador = new CargadorCompiladorExterno(clases, getClass().getClassLoader());
        try {
            // Las clases principales tienen que estar; las demás se notan al usarlas.
            Class.forName("generado.Leaf", false, cargador);
            ProveedorCompilador p = (ProveedorCompilador) Class.forName(CLASE_PROVEEDOR, true, cargador)
                    .getDeclaredConstructor().newInstance();
            return new Externo(huella, clases, p, null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return new Externo(huella, clases, null, "No se pudo cargar el compilador de " + clases + ": " + motivo(e));
        }
    }

    /** Nombre, tamaño y fecha de cada .class de {@code generado}: si algo cambia, cambia la huella. */
    private static String huella(Path clases) {
        Path dir = clases.resolve("generado");
        if (!Files.isDirectory(dir)) return "sin-carpeta";
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".class")).sorted().forEach(p -> {
                try {
                    sb.append(p.getFileName()).append(':').append(Files.size(p)).append(':')
                            .append(Files.getLastModifiedTime(p).toMillis()).append(';');
                } catch (IOException e) {
                    sb.append(p.getFileName()).append(":?;");
                }
            });
        } catch (IOException e) {
            return "ilegible";
        }
        return sb.toString();
    }

    private static String motivo(Throwable e) {
        Throwable raiz = e;
        while (raiz.getCause() != null && raiz.getCause() != raiz) raiz = raiz.getCause();
        String m = raiz.getMessage();
        return m == null || m.isBlank() ? raiz.getClass().getSimpleName() : m;
    }

    /** Para las pruebas: el cargador del compilador de la carpeta que se está usando ({@code null} si ninguno). */
    synchronized ClassLoader cargadorEnUso() {
        Object a = actual();
        return a instanceof ProveedorCompilador p && p != delIde ? p.getClass().getClassLoader() : null;
    }

    // ---------- fases ----------

    /** Una fase del adaptador que, al ejecutarse, delega en la misma fase del compilador elegido. */
    private final class FaseConmutable implements FaseCompilacion {

        private final FaseCompilacion modelo;

        FaseConmutable(FaseCompilacion modelo) {
            this.modelo = modelo;
        }

        @Override public String id() { return modelo.id(); }
        @Override public String nombre() { return modelo.nombre(); }
        @Override public List<String> dependeDe() { return modelo.dependeDe(); }
        @Override public boolean implementada() { return modelo.implementada(); }
        @Override public boolean toleraErroresPrevios(OpcionesCompilacion o) { return modelo.toleraErroresPrevios(o); }

        @Override
        public ResultadoFase ejecutar(ContextoCompilacion ctx) {
            Object a = actual();
            if (a instanceof String error) return falla(error);
            ProveedorCompilador p = (ProveedorCompilador) a;
            FaseCompilacion fase = p.fases().stream().filter(f -> f.id().equals(id())).findFirst().orElse(null);
            if (fase == null) return falla("El compilador no tiene la fase «" + nombre() + "».");
            try {
                return fase.ejecutar(ctx);
            } catch (LinkageError e) {
                return falla("El compilador de " + UbicacionCompilador.clases(carpeta)
                        + " no es compatible con el IDE o le falta una clase: " + motivo(e));
            }
        }

        private ResultadoFase falla(String mensaje) {
            Diagnostico d = new Diagnostico(id(), Severidad.ERROR, Rango.DESCONOCIDO, mensaje);
            return new ResultadoFase(id(), Estado.FALLO_INTERNO, List.of(d), List.of(), 0);
        }
    }

    /** Lee un archivo de texto o devuelve vacío (para el commit de la carpeta). */
    static String leer(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            return "";
        }
    }
}
