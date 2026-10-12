package leaf.ide.core.consola;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;
import leaf.api.Artefacto;
import leaf.api.ArtefactoTabla;
import leaf.api.ArtefactoTexto;
import leaf.api.Estado;
import leaf.api.FaseCompilacion;
import leaf.api.Fila;
import leaf.api.OpcionesCompilacion;
import leaf.api.ResultadoFase;
import leaf.ide.core.ArchivosLeaf;
import leaf.ide.core.EventBus;
import leaf.ide.core.Informe;
import leaf.ide.core.NombresFase;
import leaf.ide.core.Pipeline;

/**
 * Intérprete de los comandos de Leaf (la pestaña "Leaf" de la consola). No conoce Swing: todo lo que depende de la
 * ventana lo pide a un {@link ContextoComandos}. {@link #ejecutar} nunca lanza excepciones; un comando que falla da una
 * {@link Respuesta} con {@code error = true}.
 */
public final class InterpreteLeaf {

    /** Lo que se imprime tras un comando; {@code error} es para mostrarlo distinto (por ejemplo, en rojo). */
    public record Respuesta(String texto, boolean error) {}

    private record Comando(String nombre, List<String> alias, String uso, String resumen, String detalle,
                           Function<List<String>, String> accion) {}

    private record Argumentos(List<String> posicionales, Map<String, String> opciones) {}

    private static final Map<String, String> SINONIMOS_DE_FASE = Map.of(
            "optimizar", "optimizacion", "optimizado", "optimizacion");

    private final ContextoComandos ctx;
    private final Pipeline pipeline;
    private final List<Comando> comandos = new ArrayList<>();
    private final Map<String, Comando> porNombre = new LinkedHashMap<>();
    private volatile Path directorio;

    public InterpreteLeaf(ContextoComandos ctx, Path directorioInicial) {
        this.ctx = ctx;
        this.pipeline = new Pipeline(ctx.proveedor(), new EventBus());
        this.directorio = directorioInicial.toAbsolutePath().normalize();
        registrarComandos();
    }

    /** La carpeta actual de la sesión (la que cambia {@code cd}). */
    public Path directorio() {
        return directorio;
    }

    /** Mueve la sesión a otra carpeta desde fuera (como si se hubiera escrito {@code cd}); ignora lo que no sea una carpeta. */
    public void cambiarDirectorio(Path nuevo) {
        if (nuevo != null && Files.isDirectory(nuevo)) directorio = nuevo.toAbsolutePath().normalize();
    }

    /** Nombres de todos los comandos y alias, para el Tab. */
    public List<String> nombresDeComandos() {
        return new ArrayList<>(new TreeSet<>(porNombre.keySet()));
    }

    // ---------- ejecutar ----------

    public Respuesta ejecutar(String linea) {
        List<String> partes = LineaDeComandos.dividir(linea == null ? "" : linea);
        if (partes.isEmpty()) return new Respuesta("", false);
        Comando comando = porNombre.get(partes.get(0).toLowerCase(Locale.ROOT));
        if (comando == null) {
            return new Respuesta(conSalto("Comando desconocido: " + partes.get(0) + ". Escribe 'ayuda' para ver la lista."), true);
        }
        try {
            return new Respuesta(conSalto(comando.accion().apply(partes.subList(1, partes.size()))), false);
        } catch (ErrorDeComando e) {
            return new Respuesta(conSalto(e.getMessage()), true);
        } catch (RuntimeException e) {
            return new Respuesta(conSalto("Error interno del comando '" + comando.nombre() + "': " + e), true);
        }
    }

    // ---------- Tab ----------

    /** Candidatos para completar la última palabra de la línea (vacío si no hay ninguno). */
    public List<String> completar(String linea) {
        List<String> partes = LineaDeComandos.dividir(linea == null ? "" : linea);
        boolean terminaEnEspacio = linea != null && !linea.isEmpty() && Character.isWhitespace(linea.charAt(linea.length() - 1));
        if (partes.isEmpty() || (partes.size() == 1 && !terminaEnEspacio)) {
            String prefijo = partes.isEmpty() ? "" : partes.get(0).toLowerCase(Locale.ROOT);
            return nombresDeComandos().stream().filter(n -> n.startsWith(prefijo)).toList();
        }
        Comando comando = porNombre.get(partes.get(0).toLowerCase(Locale.ROOT));
        if (comando == null) return List.of();
        String ultimo = terminaEnEspacio ? "" : partes.get(partes.size() - 1);
        List<String> candidatos = new ArrayList<>();
        if (ultimo.startsWith("--hasta=")) {
            for (FaseCompilacion f : ctx.proveedor().fases()) candidatos.add("--hasta=" + f.id());
        } else if (ultimo.startsWith("--") || (ultimo.isEmpty() && aceptaHasta(comando))) {
            if (aceptaHasta(comando)) candidatos.add("--hasta=");
        } else if (comando.nombre().equals("tema")) {
            candidatos.addAll(List.of("claro", "oscuro"));
        } else if (comando.nombre().equals("perspectiva")) {
            candidatos.addAll(ctx.perspectivas());
        } else if (comando.nombre().equals("ayuda")) {
            candidatos.addAll(nombresDeComandos());
        }
        return candidatos.stream().filter(c -> c.startsWith(ultimo)).toList();
    }

    private static boolean aceptaHasta(Comando c) {
        return c.nombre().equals("compilar") || c.nombre().equals("leafc");
    }

    // ---------- registro ----------

    private void registrarComandos() {
        agregar("ayuda", List.of(), "ayuda [comando]", "Muestra esta lista, o el detalle de un comando.",
                "Sin argumentos lista todos los comandos; con el nombre de uno muestra cómo se usa.", this::ayuda);
        agregar("version", List.of(), "version", "Muestra la versión del compilador y de la API.", "", a -> version());
        agregar("limpiar", List.of("cls"), "limpiar", "Borra la pantalla de esta consola.", "", a -> {
            ctx.limpiarPantalla();
            return "";
        });
        agregar("fases", List.of(), "fases", "Lista las fases de compilación y cuáles están implementadas.", "", a -> fases());

        String usoCompilar = "compilar [archivo] [--hasta=fase]";
        agregar("compilar", List.of(), usoCompilar, "Compila el editor (o un archivo) y muestra los errores.",
                "Sin archivo compila lo que hay en el editor y actualiza las pestañas, igual que el botón Compilar.\n"
                        + "Con un archivo lo compila sin tocar el editor.\n"
                        + "--hasta=fase detiene la compilación en esa fase: lexico, sintactico, semantico, intermedio,\n"
                        + "optimizacion u objeto (también se aceptan optimizar y optimizado).",
                a -> compilar(a, null, false, usoCompilar));
        String usoLeafc = "leafc <archivo> [--hasta=fase]";
        agregar("leafc", List.of(), usoLeafc, "Compila un archivo del disco, como java generado.Leaf.",
                "Igual que 'compilar <archivo>': el archivo es obligatorio y no se toca el editor.",
                a -> compilar(a, null, true, usoLeafc));
        atajo("lexico", "lexico", "léxico");
        atajo("sintactico", "sintactico", "sintáctico");
        atajo("semantico", "semantico", "semántico");
        atajo("intermedio", "intermedio", "del código intermedio");
        atajo("optimizar", "optimizacion", "de optimización");
        atajo("objeto", "objeto", "del código objeto");

        agregar("tokens", List.of(), "tokens [archivo]", "Imprime la tabla de tokens (análisis léxico).", "",
                a -> artefacto(a, "lexico", "tokens [archivo]", f -> tabla(f, "No hay tabla de tokens.")));
        agregar("ast", List.of(), "ast [archivo]", "Imprime el árbol sintáctico (volcado del AST).", "",
                a -> artefacto(a, "sintactico", "ast [archivo]", f -> texto(f, "No hay árbol sintáctico.")));
        agregar("simbolos", List.of(), "simbolos [archivo]", "Imprime la tabla de símbolos (análisis semántico).", "",
                a -> artefacto(a, "semantico", "simbolos [archivo]", f -> tabla(f, "No hay tabla de símbolos.")));
        agregar("cuadruplos", List.of(), "cuadruplos [archivo]", "Imprime la tabla de cuádruplos (código intermedio).", "",
                a -> artefacto(a, "intermedio", "cuadruplos [archivo]",
                        f -> tabla(f, "No hay cuádruplos: el programa tiene errores (usa 'errores') o el compilador aún no genera código intermedio.")));
        agregar("errores", List.of(), "errores [archivo]", "Compila todo y muestra solo el informe de errores.", "", this::errores);
        agregar("ejecutar", List.of(), "ejecutar", "Ejecuta el programa (todavía no disponible).", "",
                a -> "Todavía no se puede ejecutar el programa: el compilador aún no genera código (llega con el hito M10).\n"
                        + "Usa 'compilar' para analizarlo.");

        agregar("abrir", List.of(), "abrir <ruta>", "Abre un archivo en el editor.", "", this::abrir);
        agregar("guardar", List.of(), "guardar [ruta]", "Guarda el editor (en su archivo actual, o en la ruta indicada).", "", this::guardar);
        agregar("nuevo", List.of(), "nuevo", "Abre un documento en blanco.", "", a -> {
            ctx.nuevo();
            return "Documento nuevo.";
        });
        agregar("ls", List.of(), "ls [ruta]", "Lista una carpeta (la actual si no se indica).", "", this::ls);
        agregar("cd", List.of(), "cd [ruta]", "Cambia la carpeta de esta consola, o muestra la actual.", "", this::cd);

        agregar("tema", List.of(), "tema <claro|oscuro>", "Cambia el tema de la ventana.", "", this::tema);
        agregar("perspectiva", List.of(), "perspectiva [nombre]", "Cambia la distribución de paneles; sin argumento, lista las perspectivas.", "", this::perspectiva);
    }

    private void agregar(String nombre, List<String> alias, String uso, String resumen, String detalle,
                         Function<List<String>, String> accion) {
        Comando c = new Comando(nombre, alias, uso, resumen, detalle, accion);
        comandos.add(c);
        porNombre.put(nombre, c);
        for (String a : alias) porNombre.put(a, c);
    }

    private void atajo(String nombre, String faseId, String descripcion) {
        String uso = nombre + " [archivo]";
        agregar(nombre, List.of(), uso, "Compila hasta la fase " + descripcion + ".",
                "Equivale a 'compilar --hasta=" + faseId + "'.", a -> compilar(a, faseId, false, uso));
    }

    // ---------- ayuda, versión, fases ----------

    private String ayuda(List<String> args) {
        if (args.size() > 1) throw new ErrorDeComando("Uso: ayuda [comando]");
        if (args.isEmpty()) {
            StringBuilder sb = new StringBuilder("Comandos disponibles (escribe 'ayuda <comando>' para el detalle):\n\n");
            for (Comando c : comandos) sb.append(String.format("  %-12s %s%n", c.nombre(), c.resumen()));
            return sb.toString();
        }
        Comando c = porNombre.get(args.get(0).toLowerCase(Locale.ROOT));
        if (c == null) throw new ErrorDeComando("No existe el comando '" + args.get(0) + "'. Escribe 'ayuda' para ver la lista.");
        StringBuilder sb = new StringBuilder("Uso: ").append(c.uso()).append("\n").append(c.resumen()).append('\n');
        if (!c.detalle().isEmpty()) sb.append('\n').append(c.detalle()).append('\n');
        if (!c.alias().isEmpty()) sb.append("\nTambién: ").append(String.join(", ", c.alias())).append('\n');
        return sb.toString();
    }

    private String version() {
        return "LeafIDE · " + ctx.proveedor().nombre() + " " + ctx.proveedor().version() + " · API "
                + ctx.proveedor().apiVersion() + " · Java " + System.getProperty("java.version");
    }

    private String fases() {
        List<Fila> filas = new ArrayList<>();
        for (FaseCompilacion f : ctx.proveedor().fases()) {
            filas.add(new Fila(f.id(), List.of(f.id(), f.nombre(), f.implementada() ? "implementada" : "próximamente"), null, null, null));
        }
        return FormatoTexto.tabla(new ArtefactoTabla("Fases", List.of("Fase", "Nombre", "Estado"), filas));
    }

    // ---------- compilar y consultar ----------

    private String compilar(List<String> args, String hastaFijo, boolean exigirArchivo, String uso) {
        Argumentos a = analizar(args, hastaFijo == null ? Set.of("hasta") : Set.of());
        if (a.posicionales().size() > 1) throw new ErrorDeComando("Demasiados argumentos. Uso: " + uso);
        if (exigirArchivo && a.posicionales().isEmpty()) throw new ErrorDeComando("Uso: " + uso);
        String hasta = hastaFijo != null ? hastaFijo
                : a.opciones().containsKey("hasta") ? idDeFase(a.opciones().get("hasta")) : null;
        OpcionesCompilacion opciones = hasta == null ? OpcionesCompilacion.porDefecto() : OpcionesCompilacion.hasta(hasta);

        List<ResultadoFase> resultados = resultados(a.posicionales(), opciones, true);
        StringBuilder sb = new StringBuilder(Informe.de(resultados));
        if (hasta != null) {
            boolean sinImplementar = resultados.stream()
                    .anyMatch(r -> r.faseId().equals(hasta) && r.estado() == Estado.NO_IMPLEMENTADA);
            sb.append(sinImplementar
                    ? "La fase " + NombresFase.de(hasta) + " aún no está implementada (próximamente); se analizó lo disponible.\n"
                    : "Análisis hasta la fase: " + NombresFase.de(hasta) + ".\n");
        }
        return sb.toString();
    }

    private String errores(List<String> args) {
        Argumentos a = analizar(args, Set.of());
        if (a.posicionales().size() > 1) throw new ErrorDeComando("Demasiados argumentos. Uso: errores [archivo]");
        return Informe.de(resultados(a.posicionales(), OpcionesCompilacion.porDefecto(), false));
    }

    private String artefacto(List<String> args, String faseId, String uso, Function<ResultadoFase, String> formatear) {
        Argumentos a = analizar(args, Set.of());
        if (a.posicionales().size() > 1) throw new ErrorDeComando("Demasiados argumentos. Uso: " + uso);
        List<ResultadoFase> resultados = resultados(a.posicionales(), OpcionesCompilacion.hasta(faseId), false);
        ResultadoFase fase = resultados.stream().filter(r -> r.faseId().equals(faseId)).findFirst()
                .orElseThrow(() -> new ErrorDeComando("El compilador no tiene la fase '" + faseId + "'."));
        StringBuilder sb = new StringBuilder(formatear.apply(fase));
        if (!fase.diagnosticos().isEmpty()) {
            sb.append("\nHay ").append(fase.diagnosticos().size())
                    .append(" error(es) en esta fase; usa 'errores' para verlos.\n");
        }
        return sb.toString();
    }

    private static String tabla(ResultadoFase fase, String sinDatos) {
        return primero(fase, ArtefactoTabla.class).map(FormatoTexto::tabla).orElse(sinDatos);
    }

    private static String texto(ResultadoFase fase, String sinDatos) {
        return primero(fase, ArtefactoTexto.class).map(ArtefactoTexto::contenido).orElse(sinDatos);
    }

    private static <T extends Artefacto> Optional<T> primero(ResultadoFase fase, Class<T> tipo) {
        return fase.artefactos().stream().filter(tipo::isInstance).map(tipo::cast).findFirst();
    }

    /** Resultados de compilar un archivo (sin tocar la ventana) o, sin archivo, el editor. */
    private List<ResultadoFase> resultados(List<String> posicionales, OpcionesCompilacion opciones, boolean actualizarVistas) {
        if (posicionales.isEmpty()) return ctx.compilarEditor(opciones, actualizarVistas);
        Path ruta = resolver(posicionales.get(0));
        String codigo;
        try {
            codigo = ArchivosLeaf.leer(ruta);
        } catch (IOException e) {
            throw new ErrorDeComando("No se pudo leer el archivo: " + ruta);
        }
        return pipeline.ejecutar(0, codigo, opciones);
    }

    private String idDeFase(String texto) {
        String limpio = sinAcentos(texto);
        limpio = SINONIMOS_DE_FASE.getOrDefault(limpio, limpio);
        List<String> ids = new ArrayList<>();
        for (FaseCompilacion f : ctx.proveedor().fases()) {
            if (f.id().equals(limpio)) return f.id();
            ids.add(f.id());
        }
        throw new ErrorDeComando("Fase desconocida: '" + texto + "'. Fases válidas: " + String.join(", ", ids));
    }

    // ---------- archivos y entorno ----------

    private String abrir(List<String> args) {
        if (args.size() != 1) throw new ErrorDeComando("Uso: abrir <ruta>");
        Path ruta = resolver(args.get(0));
        if (!Files.isRegularFile(ruta)) throw new ErrorDeComando("No existe el archivo: " + ruta);
        ctx.abrir(ruta);
        return "Abierto: " + ruta;
    }

    private String guardar(List<String> args) {
        if (args.size() > 1) throw new ErrorDeComando("Uso: guardar [ruta]");
        Path guardado = ctx.guardar(args.isEmpty() ? null : resolver(args.get(0)));
        return "Guardado: " + guardado;
    }

    private String ls(List<String> args) {
        if (args.size() > 1) throw new ErrorDeComando("Uso: ls [ruta]");
        Path destino = args.isEmpty() ? directorio : resolver(args.get(0));
        if (!Files.exists(destino)) throw new ErrorDeComando("No existe: " + destino);
        if (!Files.isDirectory(destino)) throw new ErrorDeComando("No es una carpeta: " + destino);
        List<Path> entradas;
        try (Stream<Path> s = Files.list(destino)) {
            entradas = s.sorted(Comparator.<Path, Boolean>comparing(p -> !Files.isDirectory(p))
                    .thenComparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))).toList();
        } catch (IOException e) {
            throw new ErrorDeComando("No se pudo leer la carpeta: " + destino);
        }
        StringBuilder sb = new StringBuilder("Contenido de " + destino + ":\n");
        if (entradas.isEmpty()) return sb.append("  (vacía)\n").toString();
        for (Path p : entradas) {
            String tamano = Files.isDirectory(p) ? "<DIR>" : tamanoDe(p);
            sb.append(String.format("%10s  %s%n", tamano, p.getFileName()));
        }
        return sb.toString();
    }

    private static String tamanoDe(Path p) {
        try {
            return String.valueOf(Files.size(p));
        } catch (IOException e) {
            return "?";
        }
    }

    private String cd(List<String> args) {
        if (args.size() > 1) throw new ErrorDeComando("Uso: cd [ruta]");
        if (args.isEmpty()) return directorio.toString();
        Path destino = resolver(args.get(0));
        if (!Files.isDirectory(destino)) throw new ErrorDeComando("No existe la carpeta: " + destino);
        directorio = destino;
        return "";
    }

    private String tema(List<String> args) {
        String eleccion = args.size() == 1 ? sinAcentos(args.get(0)) : "";
        if (!eleccion.equals("claro") && !eleccion.equals("oscuro")) throw new ErrorDeComando("Uso: tema <claro|oscuro>");
        ctx.cambiarTema(eleccion.equals("oscuro"));
        return "Tema " + eleccion + ".";
    }

    private String perspectiva(List<String> args) {
        List<String> disponibles = ctx.perspectivas();
        String uso = "Uso: perspectiva <" + String.join("|", disponibles) + ">";
        if (args.isEmpty()) return uso;
        String eleccion = args.size() == 1 ? sinAcentos(args.get(0)) : "";
        if (!disponibles.contains(eleccion)) throw new ErrorDeComando(uso);
        ctx.cambiarPerspectiva(eleccion);
        return "Perspectiva " + eleccion + ".";
    }

    // ---------- utilidades ----------

    private Path resolver(String texto) {
        try {
            return directorio.resolve(texto).normalize();
        } catch (InvalidPathException e) {
            throw new ErrorDeComando("Ruta no válida: " + texto);
        }
    }

    private static Argumentos analizar(List<String> args, Set<String> permitidas) {
        List<String> posicionales = new ArrayList<>();
        Map<String, String> opciones = new LinkedHashMap<>();
        for (String a : args) {
            if (a.startsWith("--")) {
                int igual = a.indexOf('=');
                String clave = (igual < 0 ? a.substring(2) : a.substring(2, igual)).toLowerCase(Locale.ROOT);
                if (!permitidas.contains(clave)) throw new ErrorDeComando("Opción desconocida: " + a);
                opciones.put(clave, igual < 0 ? "" : a.substring(igual + 1));
            } else {
                posicionales.add(a);
            }
        }
        return new Argumentos(posicionales, opciones);
    }

    private static String sinAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String conSalto(String texto) {
        return texto.isEmpty() || texto.endsWith("\n") ? texto : texto + "\n";
    }
}
