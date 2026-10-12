package leaf.ide.core.consola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import leaf.ide.core.EventBus;
import leaf.ide.core.Informe;
import leaf.ide.core.Pipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InterpreteLeafTest {

    @TempDir Path carpeta;

    private ContextoDePrueba ctx;
    private InterpreteLeaf interprete;

    @BeforeEach
    void preparar() {
        ctx = new ContextoDePrueba();
        interprete = new InterpreteLeaf(ctx, carpeta);
    }

    private InterpreteLeaf.Respuesta ok(String linea) {
        InterpreteLeaf.Respuesta r = interprete.ejecutar(linea);
        assertFalse(r.error(), "no debía fallar: " + linea + " -> " + r.texto());
        return r;
    }

    private InterpreteLeaf.Respuesta falla(String linea) {
        InterpreteLeaf.Respuesta r = interprete.ejecutar(linea);
        assertTrue(r.error(), "debía fallar: " + linea + " -> " + r.texto());
        return r;
    }

    private Path archivo(String nombre, String contenido) throws IOException {
        return Files.writeString(carpeta.resolve(nombre), contenido);
    }

    // ---------- básicos ----------

    @Test
    void unaLineaVaciaNoHaceNada() {
        assertEquals("", ok("   ").texto());
    }

    @Test
    void unComandoDesconocidoSugiereAyuda() {
        InterpreteLeaf.Respuesta r = falla("xyz");
        assertTrue(r.texto().contains("Comando desconocido: xyz"), r.texto());
        assertTrue(r.texto().contains("ayuda"), r.texto());
    }

    @Test
    void ayudaListaLosComandosYElDetalleDeUno() {
        String general = ok("ayuda").texto();
        for (String c : List.of("compilar", "tokens", "ast", "simbolos", "cuadruplos", "errores", "abrir", "guardar", "nuevo", "ls", "cd",
                "tema", "fases", "version", "limpiar", "ejecutar", "perspectiva", "leafc")) {
            assertTrue(general.contains(c), "falta " + c + " en la ayuda:\n" + general);
        }
        String detalle = ok("ayuda compilar").texto();
        assertTrue(detalle.contains("--hasta"), detalle);
        assertTrue(ok("ayuda leafc").texto().contains("leafc"));
        assertTrue(falla("ayuda inexistente").texto().contains("inexistente"));
    }

    @Test
    void versionDiceElCompiladorYLaApi() {
        String v = ok("version").texto();
        assertTrue(v.contains("Leaf 1.0"), v);
        assertTrue(v.contains("API 1"), v);
    }

    @Test
    void fasesListaLasSeisYMarcaLasQueFaltan() {
        String f = ok("fases").texto();
        assertTrue(f.contains("lexico"), f);
        assertTrue(f.contains("Código objeto"), f);
        assertEquals(3, f.lines().filter(l -> l.contains("próximamente")).count(), f);
    }

    @Test
    void limpiarPideALaPantallaBorrarse() {
        assertEquals("", ok("limpiar").texto());
        ok("cls");
        assertEquals(List.of("limpiar", "limpiar"), ctx.llamadas);
    }

    // ---------- compilar ----------

    @Test
    void compilarUsaElEditorYActualizaLasVistas() {
        String salida = ok("compilar").texto();
        assertEquals(Informe.de(new Pipeline(ctx.proveedor, new EventBus()).ejecutar(1, ctx.codigoDelEditor, null)), salida);
        assertTrue(salida.contains("Programa válido"), salida);
        assertTrue(ctx.ultimoActualizar, "compilar debe actualizar las vistas como el botón");
        assertEquals(null, ctx.ultimasOpciones.hastaFaseId());
    }

    @Test
    void compilarMuestraLosErroresConElFormatoOriginal() {
        ctx.codigoDelEditor = "SEMERR";
        String salida = ok("compilar").texto();
        assertTrue(salida.contains("Se encontraron 1 error(es) en total:"), salida);
        assertTrue(salida.contains("Tipo de Error: Semántico Línea 3, Columna 5 : tipos incompatibles"), salida);
    }

    @Test
    void hastaCortaEnLaFaseIndicadaYLoDice() {
        String salida = ok("compilar --hasta=sintactico").texto();
        assertEquals("sintactico", ctx.ultimasOpciones.hastaFaseId());
        assertTrue(salida.contains("Análisis hasta la fase: Sintáctico"), salida);
    }

    @Test
    void hastaAceptaAcentosMayusculasYSinonimos() {
        ok("compilar --hasta=LÉXICO");
        assertEquals("lexico", ctx.ultimasOpciones.hastaFaseId());
        ok("compilar --hasta=optimizado");
        assertEquals("optimizacion", ctx.ultimasOpciones.hastaFaseId());
        ok("compilar --hasta=optimización");
        assertEquals("optimizacion", ctx.ultimasOpciones.hastaFaseId());
    }

    @Test
    void unaFaseDesconocidaEsUnError() {
        String texto = falla("compilar --hasta=cuadruplos").texto();
        assertTrue(texto.contains("Fase desconocida"), texto);
        assertTrue(texto.contains("semantico"), "debe listar las fases válidas: " + texto);
    }

    @Test
    void unaOpcionDesconocidaEsUnError() {
        assertTrue(falla("compilar --rapido").texto().contains("Opción desconocida: --rapido"));
    }

    @Test
    void losAtajosCorrenHastaSuFase() {
        ok("lexico");
        assertEquals("lexico", ctx.ultimasOpciones.hastaFaseId());
        ok("sintactico");
        assertEquals("sintactico", ctx.ultimasOpciones.hastaFaseId());
        ok("semantico");
        assertEquals("semantico", ctx.ultimasOpciones.hastaFaseId());
    }

    @Test
    void lasFasesFuturasSeAnuncianComoProximamente() {
        for (String c : List.of("intermedio", "optimizar", "objeto")) {
            String salida = ok(c).texto();
            assertTrue(salida.contains("aún no está implementada"), c + ":\n" + salida);
            assertTrue(salida.contains("próximamente"), c + ":\n" + salida);
        }
        ok("optimizar");
        assertEquals("optimizacion", ctx.ultimasOpciones.hastaFaseId());
    }

    @Test
    void compilarUnArchivoNoTocaElEditorNiLasVistas() throws IOException {
        archivo("prueba.leaf", "SINERR");
        String salida = ok("compilar prueba.leaf").texto();
        assertTrue(salida.contains("Tipo de Error: Sintáctico Línea 2, Columna 1 : falta algo"), salida);
        assertEquals(List.of(), ctx.llamadas, "no debía usar el editor");
    }

    @Test
    void unArchivoConEspaciosVaEntreComillas() throws IOException {
        archivo("mi programa.leaf", "LEXERR");
        assertTrue(ok("compilar \"mi programa.leaf\"").texto().contains("símbolo inválido"));
    }

    @Test
    void unArchivoInexistenteEsUnError() {
        assertTrue(falla("compilar no-existe.leaf").texto().contains("No se pudo leer"));
    }

    @Test
    void leafcExigeUnArchivoYFunciona() throws IOException {
        assertTrue(falla("leafc").texto().contains("Uso: leafc"));
        archivo("a.leaf", "SEMERR");
        assertTrue(ok("leafc a.leaf --hasta=semantico").texto().contains("tipos incompatibles"));
        assertEquals(List.of(), ctx.llamadas);
    }

    @Test
    void demasiadosArgumentosSonUnError() {
        assertTrue(falla("compilar a b").texto().contains("Demasiados argumentos"));
    }

    // ---------- tokens, ast, símbolos, errores ----------

    @Test
    void tokensImprimeLaTablaSinActualizarLasVistas() {
        String t = ok("tokens").texto();
        assertTrue(t.contains("Lexema"), t);
        assertTrue(t.contains("definir"), t);
        assertTrue(t.contains("2 filas"), t);
        assertFalse(ctx.ultimoActualizar, "tokens no debe cambiar lo que se ve en las pestañas");
        assertEquals("lexico", ctx.ultimasOpciones.hastaFaseId());
    }

    @Test
    void tokensAvisaSiLaFaseTuvoErrores() {
        ctx.codigoDelEditor = "LEXERR";
        String t = ok("tokens").texto();
        assertTrue(t.contains("Hay 1 error(es) en esta fase"), t);
    }

    @Test
    void astImprimeElVolcado() {
        String t = ok("ast").texto();
        assertTrue(t.contains("Programa\n  Declaracion"), t);
        assertEquals("sintactico", ctx.ultimasOpciones.hastaFaseId());
    }

    @Test
    void cuadruplosExplicaPorQueNoHayTablaCuandoLaFaseNoGeneraNada() {
        String t = ok("cuadruplos").texto();
        assertTrue(t.contains("No hay cuádruplos"), t);
        assertFalse(ctx.ultimoActualizar, "cuadruplos no debe cambiar lo que se ve en las pestañas");
    }

    @Test
    void simbolosImprimeLaTabla() {
        String t = ok("simbolos").texto();
        assertTrue(t.contains("Nombre"), t);
        assertTrue(t.contains("$x"), t);
        assertEquals("semantico", ctx.ultimasOpciones.hastaFaseId());
    }

    @Test
    void erroresImprimeElInformeCompleto() {
        ctx.codigoDelEditor = "SEMERR";
        String t = ok("errores").texto();
        assertTrue(t.contains("Se encontraron 1 error(es) en total:"), t);
        assertFalse(ctx.ultimoActualizar);
    }

    @Test
    void tokensAstSimbolosYErroresAdmitenUnArchivo() throws IOException {
        archivo("p.leaf", "algo");
        for (String c : List.of("tokens", "ast", "simbolos", "errores")) ok(c + " p.leaf");
        assertEquals(List.of(), ctx.llamadas);
    }

    // ---------- archivos y entorno ----------

    @Test
    void lsListaCarpetasPrimeroYArchivosConSuTamano() throws IOException {
        Files.createDirectory(carpeta.resolve("ejemplos"));
        archivo("b.leaf", "12345");
        archivo("a.txt", "");
        String t = ok("ls").texto();
        assertTrue(t.indexOf("ejemplos") < t.indexOf("a.txt"), "las carpetas van primero:\n" + t);
        assertTrue(t.indexOf("a.txt") < t.indexOf("b.leaf"), t);
        assertTrue(t.contains("<DIR>"), t);
        assertTrue(t.contains("5  b.leaf"), t);
    }

    @Test
    void lsDeUnaCarpetaVaciaOInexistente() throws IOException {
        Files.createDirectory(carpeta.resolve("vacia"));
        assertTrue(ok("ls vacia").texto().contains("(vacía)"));
        assertTrue(falla("ls no-existe").texto().contains("No existe"));
        archivo("x.txt", "");
        assertTrue(falla("ls x.txt").texto().contains("No es una carpeta"));
    }

    @Test
    void cdCambiaLaCarpetaDeLaSesion() throws IOException {
        Files.createDirectory(carpeta.resolve("sub"));
        assertEquals("", ok("cd sub").texto());
        assertEquals(carpeta.resolve("sub"), interprete.directorio());
        ok("cd ..");
        assertEquals(carpeta, interprete.directorio());
        assertTrue(ok("cd").texto().contains(carpeta.toString()));
        assertTrue(falla("cd no-existe").texto().contains("No existe la carpeta"));
        assertEquals(carpeta, interprete.directorio());
    }

    @Test
    void lasRutasRelativasSeResuelvenDesdeLaCarpetaDeLaSesion() throws IOException {
        Files.createDirectory(carpeta.resolve("sub"));
        Files.writeString(carpeta.resolve("sub").resolve("dentro.leaf"), "SEMERR");
        ok("cd sub");
        assertTrue(ok("compilar dentro.leaf").texto().contains("tipos incompatibles"));
    }

    @Test
    void abrirPasaLaRutaResueltaAlEditor() throws IOException {
        archivo("uno.leaf", "x");
        assertTrue(ok("abrir uno.leaf").texto().contains("Abierto"));
        assertEquals(List.of("abrir:" + carpeta.resolve("uno.leaf")), ctx.llamadas);
        assertTrue(falla("abrir no-existe.leaf").texto().contains("No existe el archivo"));
        assertTrue(falla("abrir").texto().contains("Uso: abrir"));
    }

    @Test
    void guardarConYSinRuta() {
        assertTrue(falla("guardar").texto().contains("Indica una ruta"));
        assertTrue(ok("guardar copia.leaf").texto().contains("Guardado"));
        assertEquals(List.of("guardar:" + carpeta.resolve("copia.leaf")), ctx.llamadas);
    }

    @Test
    void nuevoPideUnDocumentoEnBlanco() {
        assertTrue(ok("nuevo").texto().contains("Documento nuevo"));
        assertEquals(List.of("nuevo"), ctx.llamadas);
    }

    @Test
    void siElIdeSeNiegaElComandoMuestraSuMotivo() throws IOException {
        archivo("u.leaf", "x");
        ctx.fallar = new ErrorDeComando("Se canceló: hay cambios sin guardar.");
        assertTrue(falla("nuevo").texto().contains("Se canceló"));
        assertTrue(falla("abrir u.leaf").texto().contains("Se canceló"));
    }

    @Test
    void unaExcepcionInesperadaNuncaEscapaDelInterprete() throws IOException {
        archivo("u.leaf", "x");
        ctx.fallar = new IllegalStateException("se rompió");
        InterpreteLeaf.Respuesta r = falla("abrir u.leaf");
        assertTrue(r.texto().contains("Error interno del comando 'abrir'"), r.texto());
    }

    @Test
    void cambiarDirectorioDesdeFueraEsComoUnCd() throws IOException {
        Path otra = Files.createDirectory(carpeta.resolve("proyecto"));
        Files.writeString(otra.resolve("hola.txt"), "x");
        interprete.cambiarDirectorio(otra);
        assertEquals(otra.toAbsolutePath().normalize(), interprete.directorio());
        assertTrue(ok("ls").texto().contains("hola.txt"));
        interprete.cambiarDirectorio(carpeta.resolve("no-existe")); // se ignora
        assertEquals(otra.toAbsolutePath().normalize(), interprete.directorio());
    }

    @Test
    void temaClaroYOscuro() {
        ok("tema oscuro");
        ok("tema CLARO");
        assertEquals(List.of("tema:oscuro", "tema:claro"), ctx.llamadas);
        assertTrue(falla("tema azul").texto().contains("Uso: tema"));
        assertTrue(falla("tema").texto().contains("Uso: tema"));
    }

    @Test
    void ejecutarAvisaQueTodaviaNoExiste() {
        assertTrue(ok("ejecutar").texto().contains("M10"));
    }

    @Test
    void perspectivaCambiaLaDisposicionAcentosIncluidos() {
        assertTrue(ok("perspectiva").texto().contains("escritura|analisis"));
        assertEquals("Perspectiva analisis.", ok("perspectiva Análisis").texto().strip());
        assertEquals(List.of("perspectiva:analisis"), ctx.llamadas);
        assertTrue(falla("perspectiva rara").texto().contains("Uso: perspectiva"));
        assertEquals(List.of("escritura"), interprete.completar("perspectiva es"));
    }

    // ---------- Tab ----------

    @Test
    void completaNombresDeComandos() {
        assertEquals(List.of("compilar"), interprete.completar("comp"));
        assertTrue(interprete.completar("").containsAll(List.of("compilar", "tokens", "ayuda")));
        assertEquals(List.of(), interprete.completar("zzz"));
    }

    @Test
    void completaLasFasesDespuesDeHasta() {
        assertEquals(List.of("--hasta=sintactico", "--hasta=semantico"), interprete.completar("compilar --hasta=s"));
        assertTrue(interprete.completar("compilar --h").contains("--hasta="));
    }

    @Test
    void completaLosArgumentosDeTemaYAyuda() {
        assertEquals(List.of("oscuro"), interprete.completar("tema o"));
        assertEquals(List.of("claro", "oscuro"), interprete.completar("tema "));
        assertEquals(List.of("tokens"), interprete.completar("ayuda tok"));
    }
}
