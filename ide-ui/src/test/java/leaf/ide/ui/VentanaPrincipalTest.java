package leaf.ide.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import leaf.api.ArtefactoArbol;
import leaf.api.ArtefactoTabla;
import leaf.api.ArtefactoTexto;
import leaf.api.ContextoCompilacion;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.EventoFase;
import leaf.api.FaseCompilacion;
import leaf.api.Fila;
import leaf.api.LenguajeDef;
import leaf.api.NodoVista;
import leaf.api.ProveedorCompilador;
import leaf.api.Rango;
import leaf.api.ResultadoFase;
import leaf.api.Severidad;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Prueba de humo de la ventana real con un compilador falso. Se omite si no hay pantalla (CI sin display). */
class VentanaPrincipalTest {

    private static FaseCompilacion fase(String id, String nombre, boolean implementada, ResultadoFase resultado) {
        return new FaseCompilacion() {
            @Override public String id() { return id; }
            @Override public String nombre() { return nombre; }
            @Override public List<String> dependeDe() { return List.of(); }
            @Override public boolean implementada() { return implementada; }
            @Override public ResultadoFase ejecutar(ContextoCompilacion ctx) { return resultado; }
        };
    }

    private static ProveedorCompilador proveedorFalso() {
        Diagnostico error = new Diagnostico("b", Severidad.ERROR, Rango.punto(2, 3), "algo salió mal");
        ResultadoFase a = new ResultadoFase("a", Estado.OK, List.of(), List.of(
                new ArtefactoTabla("Tabla", List.of("x"), List.of(new Fila("1", List.of("1"), new Rango(1, 1, 1, 3), null, null))),
                new ArtefactoArbol("Árbol", new NodoVista("Raiz", null, null, List.of(new NodoVista("Hijo", "v", null, null)))),
                new ArtefactoTexto("Texto", "texto", "hola", null)), 0);
        ResultadoFase b = new ResultadoFase("b", Estado.CON_ERRORES, List.of(error), List.of(), 0);
        List<FaseCompilacion> fases = List.of(
                fase("a", "Fase A", true, a), fase("b", "Fase B", true, b), fase("c", "Fase C", false, null));
        return new ProveedorCompilador() {
            @Override public String nombre() { return "Falso"; }
            @Override public String version() { return "9.9"; }
            @Override public int apiVersion() { return API_VERSION; }
            @Override public List<FaseCompilacion> fases() { return fases; }
            @Override public LenguajeDef lenguaje() { return new LenguajeDef("Falso", List.of("f")); }
        };
    }

    private static <T> T enEdt(Supplier<T> s) throws Exception {
        List<T> r = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> r.add(s.get()));
        return r.get(0);
    }

    /** Retardo tan largo que el análisis en vivo no ocurre solo durante la prueba. */
    private static final int SIN_EN_VIVO = 3_600_000;

    private static void esperarHasta(Supplier<Boolean> condicion, String que) throws Exception {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite && !enEdt(condicion)) Thread.sleep(25);
        assertTrue(enEdt(condicion), "No ocurrió a tiempo: " + que);
    }

    @Test
    void elAnalisisEnVivoCompilaSoloDespuesDeEscribir() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), 100));
        try {
            SwingUtilities.invokeAndWait(() -> v.setVisible(true)); // para saber qué panel está al frente
            // Sin pulsar Compilar: al escribir, el análisis llega solo y subraya el error con posición.
            SwingUtilities.invokeAndWait(() -> {
                v.mostrar(v.dockLeaf); // el usuario está mirando la consola Leaf
                v.editor.cargar("uno\ndos\ntres");
            });
            esperarHasta(() -> v.estado.getText().startsWith("✖"), "el análisis en vivo");

            assertEquals("✖ 1 diagnóstico", enEdt(() -> v.estado.getText()));
            assertEquals(1, enEdt(() -> v.editor.cantidadSubrayados()));
            assertEquals("Problemas (1)", enEdt(() -> v.dockProblemas.titulo()));
            assertEquals("Fase B · con errores", enEdt(() -> v.fases.titulo(1)));
            // El análisis en vivo no le cambia la pestaña que está mirando.
            assertEquals(true, enEdt(() -> v.terminalLeaf.isShowing()));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void conElInterruptorApagadoNoAnalizaSolo() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        // Se apaga en el mismo evento en que se crea la ventana: así el análisis que deja programado el texto inicial
        // no llega a ocurrir (con la ventana ya abierta podría adelantarse a la prueba).
        VentanaPrincipal v = enEdt(() -> {
            VentanaPrincipal nueva = new VentanaPrincipal(proveedorFalso(), 100);
            nueva.enVivo.doClick(); // lo apaga (estaba encendido)
            return nueva;
        });
        try {
            SwingUtilities.invokeAndWait(() -> v.setVisible(true)); // para saber qué panel está al frente
            SwingUtilities.invokeAndWait(() -> {
                v.estado.setText("sin tocar");
                v.editor.cargar("uno\ndos\ntres");
            });
            Thread.sleep(600); // mucho más que el retardo de 100 ms
            assertEquals("sin tocar", enEdt(() -> v.estado.getText()));
            assertEquals(0, enEdt(() -> v.editor.cantidadSubrayados()));

            // Compilar a mano sigue funcionando (y muestra la consola Leaf con el informe), y al volver a encenderlo
            // analiza de inmediato.
            SwingUtilities.invokeAndWait(() -> v.mostrar(v.dockProblemas));
            SwingUtilities.invokeAndWait(v::compilar);
            esperarHasta(() -> v.estado.getText().startsWith("✖"), "la compilación manual");
            assertEquals(1, enEdt(() -> v.editor.cantidadSubrayados()));
            assertEquals(true, enEdt(() -> v.terminalLeaf.isShowing()));

            SwingUtilities.invokeAndWait(() -> {
                v.estado.setText("otra vez");
                v.enVivo.doClick(); // lo enciende
            });
            esperarHasta(() -> v.estado.getText().startsWith("✖"), "el análisis al encender el interruptor");
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    /** Primer componente de ese tipo dentro del contenedor (búsqueda en profundidad). */
    private static <T> T buscar(java.awt.Container raiz, Class<T> tipo) {
        for (java.awt.Component c : raiz.getComponents()) {
            if (tipo.isInstance(c)) return tipo.cast(c);
            if (c instanceof java.awt.Container hijo) {
                T dentro = buscar(hijo, tipo);
                if (dentro != null) return dentro;
            }
        }
        return null;
    }

    // ---------- consolas ----------

    private static void escribirEn(VentanaPrincipal v, PanelTerminal terminal, String linea) throws Exception {
        SwingUtilities.invokeAndWait(() -> terminal.enviarLinea(linea));
    }

    @Test
    void losComandosDeLeafHacenLoMismoQueElMenu(@org.junit.jupiter.api.io.TempDir java.nio.file.Path carpeta) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), SIN_EN_VIVO));
        try {
            escribirEn(v, v.terminalLeaf, "version");
            esperarHasta(() -> v.terminalLeaf.texto().contains("Falso 9.9"), "el comando version");

            // tema: cambia el tema y el menú
            escribirEn(v, v.terminalLeaf, "tema oscuro");
            esperarHasta(() -> v.temaOscuro(), "el tema oscuro");
            escribirEn(v, v.terminalLeaf, "tema claro");
            esperarHasta(() -> !v.temaOscuro(), "el tema claro");

            // abrir: carga el archivo en el editor
            java.nio.file.Path origen = java.nio.file.Files.writeString(carpeta.resolve("uno.leaf"), "contenido uno");
            escribirEn(v, v.terminalLeaf, "abrir \"" + origen + "\"");
            esperarHasta(() -> v.editor.texto().equals("contenido uno"), "abrir");
            esperarHasta(() -> v.getTitle().contains("uno.leaf"), "el título con el archivo abierto");

            // guardar con ruta: escribe el editor en otro archivo
            java.nio.file.Path copia = carpeta.resolve("copia.leaf");
            escribirEn(v, v.terminalLeaf, "guardar \"" + copia + "\"");
            esperarHasta(() -> java.nio.file.Files.exists(copia), "guardar");
            assertEquals("contenido uno", java.nio.file.Files.readString(copia));
            esperarHasta(() -> v.getTitle().contains("copia.leaf"), "el título con el archivo guardado");

            // nuevo: documento en blanco
            escribirEn(v, v.terminalLeaf, "nuevo");
            esperarHasta(() -> v.editor.texto().isEmpty(), "nuevo");
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void unComandoQueFallaSeImprimeEnRojoYNoRompeLaConsola() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), SIN_EN_VIVO));
        try {
            escribirEn(v, v.terminalLeaf, "comando-inventado");
            esperarHasta(() -> v.terminalLeaf.texto().contains("Comando desconocido: comando-inventado"), "el mensaje de error");
            // Y la consola sigue respondiendo.
            escribirEn(v, v.terminalLeaf, "fases");
            esperarHasta(() -> v.terminalLeaf.texto().contains("Fase C"), "el comando fases");
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void laTerminalDelSistemaEjecutaComandosRealesYSigueLaCarpeta(@org.junit.jupiter.api.io.TempDir java.nio.file.Path carpeta) throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"), "solo Windows");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), SIN_EN_VIVO));
        try {
            escribirEn(v, v.terminalSistema, "echo hola-desde-el-sistema");
            esperarHasta(() -> v.terminalSistema.texto().contains("hola-desde-el-sistema\n"), "la salida de echo");
            // El proceso imprime y un instante después termina: mientras viva, lo escrito iría a su entrada.
            esperarHasta(() -> !v.terminalSistema.prompt().equals("» "), "que termine el proceso");

            java.nio.file.Files.createDirectory(carpeta.resolve("destino"));
            escribirEn(v, v.terminalSistema, "cd /d \"" + carpeta.resolve("destino") + "\"");
            esperarHasta(() -> v.terminalSistema.prompt().equals(carpeta.resolve("destino") + "> "), "el prompt con la carpeta nueva");

            // cls limpia la pantalla sin lanzar un proceso
            escribirEn(v, v.terminalSistema, "cls");
            esperarHasta(() -> v.terminalSistema.texto().isEmpty(), "cls");
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void elegirUnaFilaMarcaElEditorYMoverElCursorSeleccionaLaFila() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), SIN_EN_VIVO));
        try {
            SwingUtilities.invokeAndWait(() -> v.editor.cargar("uno\ndos\ntres"));
            SwingUtilities.invokeAndWait(v::compilar);
            esperarHasta(() -> v.estado.getText().startsWith("✖"), "la compilación");

            VistaTabla tabla = enEdt(() -> buscar(v.fases.panel(0), VistaTabla.class));
            assertTrue(tabla != null, "la fase A debe mostrar una tabla");

            // Clic en la fila: el editor marca el tramo (línea 1, columnas 1 a 3 = "uno").
            SwingUtilities.invokeAndWait(() -> tabla.elegir(0));
            assertEquals("[0, 3]", java.util.Arrays.toString(enEdt(() -> v.editor.tramoResaltado())));

            // Mover el cursor quita la marca y selecciona la fila bajo el cursor.
            SwingUtilities.invokeAndWait(() -> v.editor.irA(1, 2));
            assertEquals(null, enEdt(() -> v.editor.tramoResaltado()));
            assertEquals(0, enEdt(() -> tabla.tabla().getSelectedRow()));

            // Una posición sin token quita la selección de la fila.
            SwingUtilities.invokeAndWait(() -> v.editor.irA(3, 3));
            assertEquals(-1, enEdt(() -> tabla.tabla().getSelectedRow()));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void elFiltroDeUnaTablaSobreviveAUnNuevoAnalisis() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), SIN_EN_VIVO));
        try {
            SwingUtilities.invokeAndWait(v::compilar);
            esperarHasta(() -> v.estado.getText().startsWith("✖"), "la primera compilación");
            VistaTabla antes = enEdt(() -> buscar(v.fases.panel(0), VistaTabla.class));
            SwingUtilities.invokeAndWait(() -> antes.filtrar("zzz"));
            assertEquals(0, enEdt(() -> antes.tabla().getRowCount()));

            SwingUtilities.invokeAndWait(() -> v.estado.setText("de nuevo"));
            SwingUtilities.invokeAndWait(v::compilar);
            esperarHasta(() -> v.estado.getText().startsWith("✖"), "la segunda compilación");

            VistaTabla despues = enEdt(() -> buscar(v.fases.panel(0), VistaTabla.class));
            assertTrue(despues != antes, "las vistas se reconstruyen con cada análisis");
            assertEquals("zzz", enEdt(() -> despues.estado().filtro()));
            assertEquals(0, enEdt(() -> despues.tabla().getRowCount()));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void elModoDemostracionEsOpcionalYReproduceFasePorFase() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), SIN_EN_VIVO));
        try {
            assertEquals(false, enEdt(() -> v.demostracionActiva()), "siempre apagado al abrir");
            SwingUtilities.invokeAndWait(() -> v.editor.cargar("uno\ndos\ntres"));

            SwingUtilities.invokeAndWait(() -> v.activarDemostracion(true));
            esperarHasta(() -> v.fases.titulo(0).startsWith("▶ Fase A"), "la primera fase activa");
            // Al empezar ninguna fase tiene su resultado: esperan su turno y no se ha tocado Problemas.
            assertEquals("Problemas", enEdt(() -> v.dockProblemas.titulo()));
            assertEquals("Fase B", enEdt(() -> v.fases.titulo(1)));

            // Fase A: 3 pasos de contenido (tabla de 1 fila, ...) hasta cerrarla; "siguiente fase" salta a la B.
            SwingUtilities.invokeAndWait(v::siguienteFaseDeDemostracion);
            assertEquals("Fase A · OK", enEdt(() -> v.fases.titulo(0)), "la fase anterior queda completa");
            assertTrue(enEdt(() -> v.fases.titulo(1)).startsWith("▶ Fase B"));
            assertEquals("Problemas", enEdt(() -> v.dockProblemas.titulo()), "aún no se anuncian los errores");

            // Al terminar la última fase con pasos, se anuncian los diagnósticos.
            SwingUtilities.invokeAndWait(v::siguienteFaseDeDemostracion);
            assertEquals("Problemas (1)", enEdt(() -> v.dockProblemas.titulo()));

            // Salir del modo devuelve las fases completas, sin marca.
            SwingUtilities.invokeAndWait(() -> v.activarDemostracion(false));
            esperarHasta(() -> v.fases.titulo(1).equals("Fase B · con errores"), "las fases completas al salir");
            assertEquals("Fase A · OK", enEdt(() -> v.fases.titulo(0)));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void elInterruptorDeResaltadoNoAfectaAlSubrayadoDeErrores() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), SIN_EN_VIVO));
        try {
            assertEquals(true, enEdt(() -> v.resaltado.isSelected()));
            assertEquals(FabricaTokenMaker.ESTILO, enEdt(() -> v.editor.estilo()));

            SwingUtilities.invokeAndWait(() -> v.resaltado.doClick()); // lo apaga
            assertEquals("text/plain", enEdt(() -> v.editor.estilo()));

            SwingUtilities.invokeAndWait(v::compilar);
            esperarHasta(() -> v.estado.getText().startsWith("✖"), "la compilación manual");
            assertEquals(1, enEdt(() -> v.editor.cantidadSubrayados()));

            SwingUtilities.invokeAndWait(() -> v.resaltado.doClick()); // lo enciende
            assertEquals(FabricaTokenMaker.ESTILO, enEdt(() -> v.editor.estilo()));
            assertEquals(1, enEdt(() -> v.editor.cantidadSubrayados()));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }

    @Test
    void compilarLlenaPestanasProblemasYConsola() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "sin pantalla");
        VentanaPrincipal v = enEdt(() -> new VentanaPrincipal(proveedorFalso(), SIN_EN_VIVO));
        try {
            // Antes de compilar: tres pestañas; la no implementada ya dice "próximamente".
            assertEquals(3, v.fases.cantidad());
            assertEquals("Fase A", enEdt(() -> v.fases.titulo(0)));
            assertEquals("Fase C · próximamente", enEdt(() -> v.fases.titulo(2)));

            SwingUtilities.invokeAndWait(v::compilar);
            long limite = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < limite && !enEdt(() -> v.estado.getText()).startsWith("✖")) Thread.sleep(25);

            assertEquals("✖ 1 diagnóstico", enEdt(() -> v.estado.getText()));
            assertEquals("Fase A · OK", enEdt(() -> v.fases.titulo(0)));
            assertEquals("Fase B · con errores", enEdt(() -> v.fases.titulo(1)));
            assertEquals("Fase C · próximamente", enEdt(() -> v.fases.titulo(2)));
            assertEquals(1, enEdt(() -> v.problemas.cantidad()));
            assertEquals("Problemas (1)", enEdt(() -> v.dockProblemas.titulo()));
            // F5 es el comando 'compilar' de la consola Leaf: se ve el comando y el informe de errores.
            esperarHasta(() -> v.terminalLeaf.texto().contains("Se encontraron 1 error(es) en total:"), "el informe en la consola Leaf");
            String consola = enEdt(() -> v.terminalLeaf.texto());
            assertTrue(consola.contains("leaf> compilar\n"), consola);
            assertTrue(consola.contains("  Tipo de Error: B Línea 2, Columna 3 : algo salió mal\n"), consola);
            assertTrue(!consola.contains("iniciada"), "la consola ya no es un registro de fases: " + consola);

            // El cursor se mueve y la barra de estado lo refleja.
            SwingUtilities.invokeAndWait(() -> {
                v.editor.cargar("uno\ndos\ntres");
                v.editor.irA(2, 3);
            });
            assertEquals("Ln 2, Col 3", enEdt(() -> v.posicion.getText()));
        } finally {
            SwingUtilities.invokeAndWait(v::dispose);
        }
    }
}
