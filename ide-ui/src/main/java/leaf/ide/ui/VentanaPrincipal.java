package leaf.ide.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import io.github.andrewauclair.moderndocking.DockingRegion;
import io.github.andrewauclair.moderndocking.app.AppState;
import io.github.andrewauclair.moderndocking.app.Docking;
import io.github.andrewauclair.moderndocking.app.DockableMenuItem;
import io.github.andrewauclair.moderndocking.app.DockingState;
import io.github.andrewauclair.moderndocking.app.LayoutPersistence;
import io.github.andrewauclair.moderndocking.app.RootDockingPanel;
import io.github.andrewauclair.moderndocking.ext.ui.DockingUI;
import io.github.andrewauclair.moderndocking.exception.DockingLayoutException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JToggleButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import leaf.api.Diagnostico;
import leaf.api.LenguajeDef;
import leaf.api.OpcionesCompilacion;
import leaf.api.ProveedorCompilador;
import leaf.api.ResultadoFase;
import leaf.ide.core.AlmacenAjustes;
import leaf.ide.core.Ajustes;
import leaf.ide.core.ArchivosLeaf;
import leaf.ide.core.EjecutorCompilacion;
import leaf.ide.core.EventBus;
import leaf.ide.core.Pipeline;
import leaf.ide.core.consola.ContextoComandos;
import leaf.ide.core.consola.ErrorDeComando;
import leaf.ide.core.consola.InterpreteLeaf;
import leaf.ide.core.consola.TerminalSistema;
import leaf.ide.core.demo.Demostracion;
import leaf.api.Rango;

/** Ventana única: editor, botón Compilar, una pestaña por fase, Problemas y las consolas Leaf y Sistema. */
final class VentanaPrincipal extends JFrame {

    private static final String TEXTO_INICIAL = """
            // Programa de ejemplo: pulsa Compilar (F5)
            definir $x como entero con valor de 1.
            mostrar("Hola Leaf", $x).
            """;
    private static final int ATAJO = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    private static final Pattern POSICION_EN_INFORME = Pattern.compile("L[ií]nea (\\d+)(?:, Columna (\\d+))?");

    /** Pausa tras la última tecla antes de analizar en vivo (el plan pide entre 400 y 600 ms). */
    private static final int RETARDO_EN_VIVO_MS = 500;
    private static final String PROMPT_LEAF = "leaf> ";
    private static final String ULTIMA_DISPOSICION = "ultima.xml";
    private static final String PROMPT_PROCESO = "» ";

    private final ProveedorCompilador proveedor;
    private final EventBus bus = new EventBus();
    private final Pipeline pipeline;
    private final EjecutorCompilacion ejecutor;
    private final ExecutorService hiloDeComandos;
    private final InterpreteLeaf interprete;
    private final TerminalSistema sistema;

    final PanelEditor editor;
    final GrupoFases fases;
    final PanelProblemas problemas = new PanelProblemas();
    final PanelTerminal terminalLeaf = new PanelTerminal();
    final PanelTerminal terminalSistema = new PanelTerminal();
    PanelAcoplable dockEditor;
    PanelAcoplable dockProblemas;
    PanelAcoplable dockLeaf;
    PanelAcoplable dockSistema;
    private Disposiciones disposiciones;
    private final AlmacenAjustes almacen; // null: no se guarda nada (pruebas)
    private Perspectiva perspectiva = Perspectiva.ANALISIS;
    private boolean cerrada;

    // Modo demostración: opcional, siempre apagado al abrir el IDE.
    private final BarraDemostracion barraDemo = new BarraDemostracion(this::mostrarPasoDeDemostracion);
    private final JToggleButton botonDemo = new JToggleButton("Demostración");
    private final JCheckBoxMenuItem menuDemo = new JCheckBoxMenuItem("Modo demostración");
    private boolean demoActiva;

    final JLabel posicion = new JLabel("Ln 1, Col 1");
    private final JLabel archivoEtiqueta = new JLabel();
    final JLabel estado = new JLabel("Listo");
    final JCheckBox enVivo = new JCheckBox("Análisis en vivo", true);
    final JCheckBox resaltado = new JCheckBox("Resaltado", true);
    private final JRadioButtonMenuItem temaClaro = new JRadioButtonMenuItem("Tema claro", true);
    private final JRadioButtonMenuItem temaOscuro = new JRadioButtonMenuItem("Tema oscuro");
    private final Timer temporizador;
    private final Map<Perspectiva, JRadioButtonMenuItem> botonesPerspectiva = new EnumMap<>(Perspectiva.class);

    private int cursorLinea = 1;
    private int cursorColumna = 1;
    private Path archivoActual;
    private boolean oscuro;

    VentanaPrincipal(ProveedorCompilador proveedor) {
        this(proveedor, RETARDO_EN_VIVO_MS, Map.of(), null);
    }

    VentanaPrincipal(ProveedorCompilador proveedor, int retardoEnVivoMs) {
        this(proveedor, retardoEnVivoMs, Map.of(), null);
    }

    /**
     * @param retardoEnVivoMs pausa tras la última tecla antes de analizar solo (las pruebas usan otro valor)
     * @param entornoDeProcesos variables de entorno extra para los programas que se lancen desde la terminal Sistema
     * @param carpetaAjustes carpeta donde se guardan los ajustes y la disposición de los paneles; {@code null}: no se guarda nada
     */
    VentanaPrincipal(ProveedorCompilador proveedor, int retardoEnVivoMs, Map<String, String> entornoDeProcesos,
            Path carpetaAjustes) {
        super("LeafIDE");
        this.proveedor = proveedor;
        this.almacen = carpetaAjustes == null ? null : new AlmacenAjustes(carpetaAjustes);
        iniciarDocking();
        LenguajeDef lenguaje = proveedor.lenguaje() != null ? proveedor.lenguaje() : new LenguajeDef("", List.of());
        this.editor = new PanelEditor(lenguaje);
        this.fases = new GrupoFases(proveedor.fases(), editor::resaltarRango);
        this.pipeline = new Pipeline(proveedor, bus);
        this.ejecutor = new EjecutorCompilacion(pipeline);
        this.temporizador = new Timer(retardoEnVivoMs, e -> analizarEnVivo());
        temporizador.setRepeats(false);

        Path carpetaInicial = Path.of("").toAbsolutePath();
        this.interprete = new InterpreteLeaf(new ContextoVentana(), carpetaInicial);
        this.hiloDeComandos = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "HiloDeComandosLeaf");
            t.setDaemon(true);
            return t;
        });
        this.sistema = new TerminalSistema(carpetaInicial, entornoDeProcesos, new EscuchaDelSistema(), TerminalSistema.oemDelSistema());

        enVivo.setToolTipText("Analiza el código solo, unos instantes después de dejar de escribir");
        enVivo.addActionListener(e -> {
            if (enVivo.isSelected()) analizarEnVivo();
            else temporizador.stop();
        });
        editor.alCambiarTexto(() -> {
            if (demoActiva || enVivo.isSelected()) temporizador.restart(); // en demostración: recompila la demostración
        });
        resaltado.setToolTipText("Colorea las palabras del lenguaje; apagado, el código se ve como texto plano");
        resaltado.addActionListener(e -> editor.activarResaltado(resaltado.isSelected()));
        problemas.alElegir(d -> editor.irA(d.rango().lineaIni(), d.rango().colIni()));
        editor.alCambiarModificado(this::actualizarTitulo);
        editor.alMoverCursor((linea, col) -> {
            posicion.setText("Ln " + linea + ", Col " + col);
            cursorLinea = linea;
            cursorColumna = col;
            fases.cursorEn(linea, col); // las vistas seleccionan el token o el nodo que está bajo el cursor
        });

        conectarTerminales();
        dockEditor = new PanelAcoplable("editor", "Editor", editor, false);
        dockProblemas = new PanelAcoplable("problemas", "Problemas", problemas, true);
        dockLeaf = new PanelAcoplable("consola.leaf", "Leaf", terminalLeaf, true);
        dockSistema = new PanelAcoplable("consola.sistema", "Sistema", terminalSistema, true);
        disposiciones = new Disposiciones(dockEditor, List.of(dockProblemas, dockLeaf, dockSistema), fases);

        setLayout(new BorderLayout());
        barraDemo.setVisible(false);
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(barraHerramientas(), BorderLayout.NORTH);
        norte.add(barraDemo, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);
        add(new RootDockingPanel(this), BorderLayout.CENTER);
        add(barraEstado(), BorderLayout.SOUTH);
        setJMenuBar(menu());

        editor.aplicarTema(false);
        editor.cargar(TEXTO_INICIAL);
        actualizarTitulo();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { salir(); }
        });
        setPreferredSize(new Dimension(1280, 800));
        pack();
        setLocationRelativeTo(null);
        aplicarAjustes();
    }

    // ---------- construcción ----------

    private void conectarTerminales() {
        terminalLeaf.fijarPrompt(PROMPT_LEAF);
        terminalLeaf.alCompletar(interprete::completar);
        terminalLeaf.alDobleClicEnLinea(this::irALaLineaDelInforme);
        terminalLeaf.alEnviar(linea -> hiloDeComandos.execute(() -> {
            InterpreteLeaf.Respuesta r = interprete.ejecutar(linea);
            SwingUtilities.invokeLater(() ->
                    terminalLeaf.imprimir(r.texto(), r.error() ? PanelTerminal.Estilo.ERROR : PanelTerminal.Estilo.NORMAL));
        }));
        terminalLeaf.imprimir("LeafIDE · " + proveedor.nombre() + " " + proveedor.version() + " · API " + proveedor.apiVersion()
                + "\nEscribe 'ayuda' para ver los comandos. Doble clic en un error lleva al código.\n", PanelTerminal.Estilo.NORMAL);

        refrescarPromptDelSistema();
        terminalSistema.alInterrumpir(sistema::detener);
        terminalSistema.alEnviar(linea -> {
            if (!sistema.enEjecucion() && linea.strip().equalsIgnoreCase("cls")) {
                terminalSistema.limpiar();
            } else {
                sistema.enviar(linea);
            }
            refrescarPromptDelSistema();
        });
        terminalSistema.imprimir("Terminal del sistema (cmd). Ctrl+C detiene el programa en marcha.\n", PanelTerminal.Estilo.NORMAL);
    }

    private void refrescarPromptDelSistema() {
        terminalSistema.fijarPrompt(sistema.enEjecucion() ? PROMPT_PROCESO : sistema.directorio() + "> ");
    }

    /** Doble clic en una línea del informe de errores ("… Línea 3, Columna 5 : …"): lleva el cursor ahí. */
    private void irALaLineaDelInforme(String linea) {
        Matcher m = POSICION_EN_INFORME.matcher(linea);
        if (!m.find()) return;
        editor.irA(Integer.parseInt(m.group(1)), m.group(2) == null ? 1 : Integer.parseInt(m.group(2)));
    }

    private JToolBar barraHerramientas() {
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        JButton compilar = new JButton("▶ Compilar");
        compilar.setToolTipText("Compilar (F5): ejecuta el comando 'compilar' de la consola Leaf");
        compilar.addActionListener(e -> compilar());
        barra.add(compilar);
        barra.addSeparator();
        barra.add(enVivo);
        barra.add(resaltado);
        barra.addSeparator();
        botonDemo.setToolTipText("Reproduce la compilación paso a paso, fase por fase (opcional)");
        botonDemo.addActionListener(e -> activarDemostracion(botonDemo.isSelected()));
        barra.add(botonDemo);
        return barra;
    }

    private JPanel barraEstado() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        JPanel izquierda = new JPanel(new BorderLayout(16, 0));
        izquierda.setOpaque(false);
        izquierda.add(posicion, BorderLayout.WEST);
        izquierda.add(archivoEtiqueta, BorderLayout.CENTER);
        p.add(izquierda, BorderLayout.WEST);
        p.add(estado, BorderLayout.CENTER);
        p.add(new JLabel("UTF-8 · " + proveedor.nombre() + " " + proveedor.version() + " · API " + proveedor.apiVersion()),
                BorderLayout.EAST);
        return p;
    }

    private JMenuBar menu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.add(item("Nuevo", KeyStroke.getKeyStroke(KeyEvent.VK_N, ATAJO), this::nuevo));
        archivo.add(item("Abrir…", KeyStroke.getKeyStroke(KeyEvent.VK_O, ATAJO), this::abrir));
        archivo.add(item("Guardar", KeyStroke.getKeyStroke(KeyEvent.VK_S, ATAJO), this::guardar));
        archivo.add(item("Guardar como…",
                KeyStroke.getKeyStroke(KeyEvent.VK_S, ATAJO | InputEvent.SHIFT_DOWN_MASK), this::guardarComo));
        archivo.addSeparator();
        archivo.add(item("Salir", null, this::salir));

        JMenu compilar = new JMenu("Compilar");
        compilar.add(item("Compilar", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this::compilar));
        compilar.addSeparator();
        menuDemo.addActionListener(e -> activarDemostracion(menuDemo.isSelected()));
        compilar.add(menuDemo);
        compilar.add(item("Demostración: paso", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
                () -> { if (demoActiva) barraDemo.paso(); }));
        compilar.add(item("Demostración: siguiente fase", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0),
                () -> { if (demoActiva) barraDemo.siguienteFase(); }));
        compilar.add(item("Demostración: automático o pausa", KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0),
                () -> { if (demoActiva) barraDemo.alternarAuto(); }));

        JMenu ver = new JMenu("Ver");
        ButtonGroup grupo = new ButtonGroup();
        temaClaro.addActionListener(e -> cambiarTema(false));
        temaOscuro.addActionListener(e -> cambiarTema(true));
        grupo.add(temaClaro);
        grupo.add(temaOscuro);
        ver.add(temaClaro);
        ver.add(temaOscuro);
        ver.addSeparator();
        ver.add(menuPerspectivas());
        ver.add(item("Restaurar diseño", null, () -> aplicarPerspectiva(perspectiva)));
        ver.add(item("Guardar disposición actual como…", null, this::guardarDisposicion));
        ver.add(menuDisposicionesGuardadas());
        ver.add(menuPaneles());
        ver.addSeparator();
        ver.add(item("Limpiar consola", null, this::limpiarConsolaActual));

        barra.add(archivo);
        barra.add(compilar);
        barra.add(ver);
        return barra;
    }

    private JMenu menuPerspectivas() {
        JMenu m = new JMenu("Perspectiva");
        ButtonGroup grupo = new ButtonGroup();
        for (Perspectiva p : Perspectiva.values()) {
            JRadioButtonMenuItem i = new JRadioButtonMenuItem(p.nombre(), p == perspectiva);
            i.setToolTipText(p.descripcion());
            i.addActionListener(e -> aplicarPerspectiva(p));
            grupo.add(i);
            m.add(i);
            botonesPerspectiva.put(p, i);
        }
        return m;
    }

    private JMenu menuPaneles() {
        JMenu m = new JMenu("Paneles");
        for (PanelAcoplable p : disposiciones.todos()) m.add(new DockableMenuItem(p.getPersistentID(), p.titulo()));
        return m;
    }

    /** Se rellena al abrirse con los archivos de {@code layouts/}. */
    private JMenu menuDisposicionesGuardadas() {
        JMenu m = new JMenu("Disposiciones guardadas");
        m.addMenuListener(new MenuListener() {
            @Override public void menuSelected(MenuEvent e) {
                m.removeAll();
                List<String> nombres = nombresDeDisposiciones();
                if (nombres.isEmpty()) {
                    JMenuItem vacio = new JMenuItem("(ninguna todavía)");
                    vacio.setEnabled(false);
                    m.add(vacio);
                }
                for (String n : nombres) m.add(item(n, null, () -> cargarDisposicion(n)));
            }
            @Override public void menuDeselected(MenuEvent e) { }
            @Override public void menuCanceled(MenuEvent e) { }
        });
        return m;
    }

    // ---------- perspectivas y disposición ----------

    /** Lo primero del constructor: los paneles se registran en el docking al crearse. */
    private void iniciarDocking() {
        try {
            Docking.uninitialize(); // por si una ventana anterior (pruebas) no se cerró
        } catch (RuntimeException e) {
            // no había nada que desinicializar
        }
        Docking.initialize(this);
        DockingUI.initialize();
        AppState.setAutoPersist(false);
        if (almacen != null) AppState.setPersistFile(carpetaDisposiciones().resolve(ULTIMA_DISPOSICION).toFile());
    }

    /** Aplica los ajustes guardados y coloca los paneles: la última disposición, o la perspectiva inicial. */
    private void aplicarAjustes() {
        Ajustes a = almacen == null ? Ajustes.porDefecto() : almacen.leer();
        enVivo.setSelected(a.analisisEnVivo());
        resaltado.setSelected(a.resaltado());
        editor.activarResaltado(a.resaltado());
        if (a.temaOscuro()) cambiarTema(true);
        perspectiva = Perspectiva.de(a.perspectiva(), Perspectiva.ANALISIS);
        botonesPerspectiva.get(perspectiva).setSelected(true);

        boolean restaurada = false;
        if (almacen != null) {
            try {
                restaurada = AppState.restore();
            } catch (DockingLayoutException | RuntimeException e) {
                restaurada = false; // archivo dañado o de otra versión: se parte de la perspectiva
            }
        }
        if (!restaurada || !dockEditor.acoplado()) disposiciones.aplicar(perspectiva);
        AppState.setAutoPersist(almacen != null);
    }

    void aplicarPerspectiva(Perspectiva p) {
        perspectiva = p;
        botonesPerspectiva.get(p).setSelected(true);
        disposiciones.aplicar(p);
        estado.setText("Perspectiva: " + p.nombre());
    }

    /** Muestra el panel (lo acopla abajo si estaba cerrado) y lo pone al frente de su grupo de pestañas. */
    void mostrar(PanelAcoplable p) {
        if (!p.acoplado()) Docking.dock(p, this, DockingRegion.SOUTH, 0.3);
        Docking.bringToFront(p);
    }

    private Path carpetaDisposiciones() {
        return almacen.carpeta().resolve("layouts");
    }

    private List<String> nombresDeDisposiciones() {
        if (almacen == null || !Files.isDirectory(carpetaDisposiciones())) return List.of();
        try (var flujo = Files.list(carpetaDisposiciones())) {
            return flujo.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".xml") && !n.equals(ULTIMA_DISPOSICION))
                    .map(n -> n.substring(0, n.length() - 4))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private void guardarDisposicion() {
        if (almacen == null) {
            error("Esta ventana no guarda ajustes.");
            return;
        }
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la disposición:", "Guardar disposición",
                JOptionPane.PLAIN_MESSAGE);
        if (nombre == null) return;
        nombre = nombre.strip().replaceAll("[^\\p{L}\\p{N} _-]", "_");
        if (nombre.isEmpty() || nombre.equalsIgnoreCase("ultima")) {
            error("Elige otro nombre.");
            return;
        }
        try {
            Files.createDirectories(carpetaDisposiciones());
            LayoutPersistence.saveLayoutToFile(carpetaDisposiciones().resolve(nombre + ".xml").toFile(),
                    DockingState.getApplicationLayout());
            estado.setText("Disposición guardada: " + nombre);
        } catch (IOException | DockingLayoutException e) {
            error("No se pudo guardar la disposición:\n" + e.getMessage());
        }
    }

    private void cargarDisposicion(String nombre) {
        File archivo = carpetaDisposiciones().resolve(nombre + ".xml").toFile();
        try {
            DockingState.restoreApplicationLayout(LayoutPersistence.loadApplicationLayoutFromFile(archivo));
            estado.setText("Disposición: " + nombre);
        } catch (DockingLayoutException | RuntimeException e) {
            error("No se pudo cargar la disposición «" + nombre + "»:\n" + e.getMessage());
        }
    }

    private static JMenuItem item(String texto, KeyStroke atajo, Runnable accion) {
        JMenuItem i = new JMenuItem(texto);
        if (atajo != null) i.setAccelerator(atajo);
        i.addActionListener(e -> accion.run());
        return i;
    }

    private void limpiarConsolaActual() {
        var foco = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean enSistema = foco != null && SwingUtilities.isDescendingFrom(foco, terminalSistema);
        (enSistema ? terminalSistema : terminalLeaf).limpiar();
    }

    // ---------- compilar ----------

    /**
     * Compilación pedida por el usuario (botón o F5): es el comando {@code compilar} de la consola Leaf, así que se ve
     * en ella igual que si lo hubiera escrito. El informe de errores aparece ahí; Problemas sigue al día.
     */
    void compilar() {
        temporizador.stop(); // lo que estaba por analizarse ya se pide ahora
        if (demoActiva) { // en modo demostración, compilar es empezar la demostración de nuevo
            iniciarDemostracion();
            return;
        }
        mostrar(dockLeaf);
        SwingUtilities.invokeLater(terminalLeaf::enfocar);
        terminalLeaf.enviarLinea("compilar");
    }

    /** Análisis en vivo: no escribe en la consola ni cambia lo que el usuario está mirando. */
    private void analizarEnVivo() {
        temporizador.stop();
        if (demoActiva) { // el texto cambió: la demostración que se veía ya no corresponde
            iniciarDemostracion();
            return;
        }
        ejecutor.solicitar(editor.texto(), null, resultados -> SwingUtilities.invokeLater(() -> mostrarResultados(resultados)));
    }

    /** Solo se llama con el resultado más reciente: el ejecutor descarta los viejos. */
    private void mostrarResultados(List<ResultadoFase> resultados) {
        if (demoActiva) return; // la demostración decide qué se ve
        fases.mostrar(resultados);
        fases.cursorEn(cursorLinea, cursorColumna); // las vistas nuevas parten de lo que hay bajo el cursor
        mostrarDiagnosticos(resultados);
    }

    /** Problemas, subrayado del editor y estado, a partir de los diagnósticos de todas las fases. */
    private void mostrarDiagnosticos(List<ResultadoFase> resultados) {
        List<Diagnostico> diagnosticos = new ArrayList<>();
        for (ResultadoFase r : resultados) diagnosticos.addAll(r.diagnosticos());
        problemas.mostrar(diagnosticos);
        editor.mostrarDiagnosticos(diagnosticos);
        dockProblemas.titulo(diagnosticos.isEmpty() ? "Problemas" : "Problemas (" + diagnosticos.size() + ")");
        estado.setText(diagnosticos.isEmpty() ? "Compilación sin errores"
                : "✖ " + diagnosticos.size() + (diagnosticos.size() == 1 ? " diagnóstico" : " diagnósticos"));
    }

    // ---------- modo demostración ----------

    /** Entra o sale del modo demostración. Solo ocurre cuando el usuario lo pide (botón o menú Compilar). */
    void activarDemostracion(boolean activar) {
        if (activar == demoActiva) return;
        demoActiva = activar;
        botonDemo.setSelected(activar);
        menuDemo.setSelected(activar);
        barraDemo.setVisible(activar);
        if (activar) {
            temporizador.stop();
            boolean hayFasesAlaVista = false;
            for (int i = 0; i < fases.cantidad(); i++) hayFasesAlaVista |= fases.acoplable(i).acoplado();
            if (!hayFasesAlaVista) aplicarPerspectiva(Perspectiva.ANALISIS); // en Escritura no se vería nada
            iniciarDemostracion();
        } else {
            barraDemo.aviso(" ");
            fases.marcarActiva(null, false);
            editor.resaltarRango(Rango.DESCONOCIDO);
            analizarSinDemostracion();
        }
        revalidate();
    }

    /** Para las pruebas y los atajos: completa la fase actual y pasa a la siguiente. */
    void siguienteFaseDeDemostracion() {
        barraDemo.siguienteFase();
    }

    boolean demostracionActiva() {
        return demoActiva;
    }

    /** Compila el código del editor y reproduce el resultado desde el principio. */
    private void iniciarDemostracion() {
        barraDemo.aviso("Compilando…");
        fases.marcarActiva(null, false);
        ejecutor.solicitar(editor.texto(), null, resultados -> SwingUtilities.invokeLater(() -> {
            if (demoActiva) barraDemo.fijar(new Demostracion(resultados));
        }));
    }

    /** Al salir de la demostración las fases vuelven a mostrar el resultado completo. */
    private void analizarSinDemostracion() {
        ejecutor.solicitar(editor.texto(), null, resultados -> SwingUtilities.invokeLater(() -> mostrarResultados(resultados)));
    }

    private void mostrarPasoDeDemostracion(Demostracion d) {
        Demostracion.Paso p = d.actual();
        fases.mostrarDemostracion(d.vista());
        fases.marcarActiva(p == null ? null : p.faseId(), p != null && !p.cierre());
        if (p != null) {
            editor.resaltarRango(p.rango());
            PanelAcoplable panel = fases.acoplablePorId(p.faseId());
            if (panel != null) mostrar(panel);
        }
        if (d.alFinal()) mostrarDiagnosticos(d.resultados()); // los errores se anuncian al terminar de contar la historia
    }

    // ---------- archivos ----------

    private void nuevo() {
        nuevoDocumento();
    }

    /** @return {@code false} si el usuario canceló al preguntarle por los cambios sin guardar */
    private boolean nuevoDocumento() {
        if (!confirmarDescarte()) return false;
        archivoActual = null;
        editor.cargar("");
        actualizarTitulo();
        return true;
    }

    private void abrir() {
        if (!confirmarDescarte()) return;
        JFileChooser selector = selectorArchivos();
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path ruta = selector.getSelectedFile().toPath();
        try {
            cargarArchivo(ruta);
        } catch (IOException e) {
            error("No se pudo abrir el archivo:\n" + ruta + "\n\n" + e.getMessage());
        }
    }

    private void cargarArchivo(Path ruta) throws IOException {
        editor.cargar(ArchivosLeaf.leer(ruta));
        archivoActual = ruta;
        actualizarTitulo();
    }

    private boolean guardar() {
        return archivoActual == null ? guardarComo() : escribir(archivoActual);
    }

    private boolean guardarComo() {
        JFileChooser selector = selectorArchivos();
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
        Path ruta = ArchivosLeaf.conExtension(selector.getSelectedFile().toPath());
        if (Files.exists(ruta) && JOptionPane.showConfirmDialog(this,
                "El archivo ya existe:\n" + ruta + "\n\n¿Reemplazarlo?", "Guardar como",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return false;
        }
        return escribir(ruta);
    }

    private boolean escribir(Path ruta) {
        try {
            escribirSinAvisos(ruta);
            return true;
        } catch (IOException e) {
            error("No se pudo guardar el archivo:\n" + ruta + "\n\n" + e.getMessage());
            return false;
        }
    }

    private void escribirSinAvisos(Path ruta) throws IOException {
        ArchivosLeaf.escribir(ruta, editor.texto());
        archivoActual = ruta;
        editor.marcarGuardado();
        actualizarTitulo();
    }

    private JFileChooser selectorArchivos() {
        JFileChooser selector = new JFileChooser(archivoActual != null ? archivoActual.getParent().toFile() : null);
        selector.setFileFilter(new FileNameExtensionFilter("Programas Leaf (*.leaf, *.txt)", "leaf", "txt"));
        return selector;
    }

    /** @return {@code true} si se puede continuar (no había cambios, o se guardó, o se descartaron a propósito). */
    private boolean confirmarDescarte() {
        if (!editor.modificado()) return true;
        int r = JOptionPane.showConfirmDialog(this, "Hay cambios sin guardar. ¿Guardarlos?", "LeafIDE",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) return guardar();
        return r == JOptionPane.NO_OPTION;
    }

    private void salir() {
        if (!confirmarDescarte()) return;
        dispose();
    }

    /** Además de cerrar la ventana, detiene el análisis en vivo, los comandos y los procesos de la terminal. */
    @Override
    public void dispose() {
        if (cerrada) return;
        cerrada = true;
        guardarAjustes();
        temporizador.stop();
        barraDemo.detenerAuto();
        ejecutor.apagar();
        hiloDeComandos.shutdownNow();
        sistema.cerrar();
        AppState.setAutoPersist(false);
        try {
            Docking.uninitialize(); // suelta las ventanas separadas y el estado global; antes de cerrar la principal
        } catch (RuntimeException e) {
            // ya estaba suelto
        }
        super.dispose();
    }

    /** Antes de cerrar nada: la disposición (con sus ventanas separadas) y las preferencias. */
    private void guardarAjustes() {
        if (almacen == null) return;
        try {
            Files.createDirectories(carpetaDisposiciones());
            LayoutPersistence.saveLayoutToFile(carpetaDisposiciones().resolve(ULTIMA_DISPOSICION).toFile(),
                    DockingState.getApplicationLayout());
            almacen.guardar(new Ajustes(oscuro, enVivo.isSelected(), resaltado.isSelected(), perspectiva.name()));
        } catch (IOException | DockingLayoutException | RuntimeException e) {
            // no poder guardar las preferencias no debe impedir cerrar
        }
    }

    // ---------- varios ----------

    /** Para las pruebas y los comandos: ¿está puesto el tema oscuro? */
    boolean temaOscuro() {
        return oscuro;
    }

    private void cambiarTema(boolean oscuro) {
        this.oscuro = oscuro;
        (oscuro ? temaOscuro : temaClaro).setSelected(true);
        try {
            UIManager.setLookAndFeel(oscuro ? new FlatDarkLaf() : new FlatLightLaf());
            FlatLaf.updateUI();
        } catch (Exception e) {
            error("No se pudo cambiar el tema: " + e.getMessage());
            return;
        }
        editor.aplicarTema(oscuro);
    }

    private void actualizarTitulo() {
        String nombre = archivoActual == null ? "Sin título" : archivoActual.getFileName().toString();
        String marca = editor.modificado() ? " *" : "";
        setTitle("LeafIDE — " + nombre + marca);
        archivoEtiqueta.setText(nombre + marca);
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "LeafIDE", JOptionPane.ERROR_MESSAGE);
    }

    /** Ejecuta en el hilo de la ventana y espera; un {@link ErrorDeComando} de la acción llega a quien la pidió. */
    @SuppressWarnings("unchecked")
    private static <T> T enLaVentana(Supplier<T> accion) {
        if (SwingUtilities.isEventDispatchThread()) return accion.get();
        Object[] caja = new Object[2];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    caja[0] = accion.get();
                } catch (RuntimeException e) {
                    caja[1] = e;
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorDeComando("Se interrumpió el comando.");
        } catch (InvocationTargetException e) {
            throw new ErrorDeComando("Falló la ventana: " + e.getCause());
        }
        if (caja[1] instanceof RuntimeException e) throw e;
        return (T) caja[0];
    }

    // ---------- lo que la ventana ofrece a los comandos de Leaf ----------

    private final class ContextoVentana implements ContextoComandos {

        @Override
        public ProveedorCompilador proveedor() {
            return proveedor;
        }

        @Override
        public List<ResultadoFase> compilarEditor(OpcionesCompilacion opciones, boolean actualizarVistas) {
            String texto = enLaVentana(editor::texto);
            if (actualizarVistas) SwingUtilities.invokeLater(() -> estado.setText("Compilando…"));
            List<ResultadoFase> resultados = pipeline.ejecutar(0, texto, opciones);
            if (actualizarVistas) SwingUtilities.invokeLater(() -> mostrarResultados(resultados));
            return resultados;
        }

        @Override
        public void nuevo() {
            if (!enLaVentana(VentanaPrincipal.this::nuevoDocumento)) {
                throw new ErrorDeComando("Cancelado: hay cambios sin guardar.");
            }
        }

        @Override
        public void abrir(Path ruta) {
            enLaVentana(() -> {
                if (!confirmarDescarte()) throw new ErrorDeComando("Cancelado: hay cambios sin guardar.");
                try {
                    cargarArchivo(ruta);
                } catch (IOException e) {
                    throw new ErrorDeComando("No se pudo abrir el archivo: " + ruta);
                }
                return null;
            });
        }

        @Override
        public Path guardar(Path ruta) {
            return enLaVentana(() -> {
                Path destino = ruta != null ? ArchivosLeaf.conExtension(ruta) : archivoActual;
                if (destino == null) throw new ErrorDeComando("Indica una ruta: guardar <ruta>");
                try {
                    escribirSinAvisos(destino);
                } catch (IOException e) {
                    throw new ErrorDeComando("No se pudo guardar el archivo: " + destino);
                }
                return destino;
            });
        }

        @Override
        public void cambiarTema(boolean oscuro) {
            enLaVentana(() -> {
                VentanaPrincipal.this.cambiarTema(oscuro);
                return null;
            });
        }

        @Override
        public List<String> perspectivas() {
            return List.of(Perspectiva.values()).stream().map(p -> p.name().toLowerCase(java.util.Locale.ROOT)).toList();
        }

        @Override
        public void cambiarPerspectiva(String id) {
            enLaVentana(() -> {
                aplicarPerspectiva(Perspectiva.de(id.toUpperCase(java.util.Locale.ROOT), perspectiva));
                return null;
            });
        }

        @Override
        public void limpiarPantalla() {
            SwingUtilities.invokeLater(terminalLeaf::limpiar);
        }
    }

    // ---------- lo que imprime la terminal del sistema ----------

    private final class EscuchaDelSistema implements TerminalSistema.Escucha {

        @Override
        public void salida(String texto) {
            SwingUtilities.invokeLater(() -> terminalSistema.imprimir(texto, PanelTerminal.Estilo.NORMAL));
        }

        @Override
        public void terminado(int codigo) {
            SwingUtilities.invokeLater(() -> {
                if (codigo != 0) {
                    terminalSistema.imprimir("[el proceso terminó con código " + codigo + "]\n", PanelTerminal.Estilo.ERROR);
                }
                refrescarPromptDelSistema();
            });
        }
    }
}
