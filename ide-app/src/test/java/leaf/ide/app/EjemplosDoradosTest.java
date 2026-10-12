package leaf.ide.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import leaf.api.ArtefactoArbol;
import leaf.api.ArtefactoTabla;
import leaf.api.Diagnostico;
import leaf.api.Estado;
import leaf.api.ResultadoFase;
import leaf.ide.core.EventBus;
import leaf.ide.core.Pipeline;
import leaf.ide.core.RegistroProveedores;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Pruebas doradas: compila cada ejemplo con el pipeline real y compara contra ejemplos/esperado/.
 * Con -Dgolden.update=true se reescriben los dorados (solo tras un cambio intencional del compilador).
 */
class EjemplosDoradosTest {

    private static final String[] CARPETAS = {"validos", "con-errores", "origen"};
    private static final boolean ACTUALIZAR = Boolean.getBoolean("golden.update");

    @TestFactory
    List<DynamicTest> ejemplosContraDorados() throws IOException {
        Path raiz = raizDelProyecto();
        Pipeline pipeline = new Pipeline(RegistroProveedores.cargar().orElseThrow(), new EventBus());
        List<DynamicTest> pruebas = new ArrayList<>();
        long id = 0;
        for (String carpeta : CARPETAS) {
            List<Path> archivos;
            try (Stream<Path> s = Files.list(raiz.resolve("ejemplos").resolve(carpeta))) {
                archivos = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
            }
            for (Path archivo : archivos) {
                long idCompilacion = ++id;
                pruebas.add(DynamicTest.dynamicTest(carpeta + "/" + archivo.getFileName(), () -> {
                    List<ResultadoFase> fases = pipeline.ejecutar(idCompilacion, leer(archivo), null);
                    verificarRegla(carpeta, fases);
                    compararConDorado(raiz.resolve("ejemplos/esperado").resolve(carpeta).resolve(archivo.getFileName()),
                            resumen(fases));
                }));
            }
        }
        assertEquals(26, pruebas.size(), "Se esperaban 26 ejemplos");
        return pruebas;
    }

    private static void verificarRegla(String carpeta, List<ResultadoFase> fases) {
        int diagnosticos = fases.stream().mapToInt(f -> f.diagnosticos().size()).sum();
        if (carpeta.equals("validos")) assertEquals(0, diagnosticos, "un ejemplo válido no debe dar diagnósticos");
        if (carpeta.equals("con-errores")) assertTrue(diagnosticos >= 1, "un ejemplo con errores debe dar al menos uno");
        // El código intermedio solo existe si el programa no tiene ningún error (como en Leaf.main).
        ResultadoFase intermedio = fases.stream().filter(f -> f.faseId().equals("intermedio")).findFirst().orElseThrow();
        if (diagnosticos == 0) {
            assertEquals(Estado.OK, intermedio.estado(), "sin errores debe haber código intermedio");
            assertTrue(!intermedio.artefactos().isEmpty(), "el código intermedio debe traer sus artefactos");
        } else {
            assertEquals(Estado.OMITIDA, intermedio.estado(), "con errores no se genera código intermedio");
        }
        for (String futura : List.of("optimizacion", "objeto")) {
            assertEquals(Estado.NO_IMPLEMENTADA, fases.stream().filter(f -> f.faseId().equals(futura)).findFirst()
                    .orElseThrow().estado(), "fase futura " + futura);
        }
    }

    /** Resumen estable: estado por fase, cifras de los artefactos y cada diagnóstico (fase, línea, columna, mensaje). */
    private static String resumen(List<ResultadoFase> fases) {
        StringBuilder sb = new StringBuilder();
        for (ResultadoFase f : fases) {
            sb.append(f.faseId()).append(": ").append(f.estado()).append('\n');
            for (var a : f.artefactos()) {
                if (a instanceof ArtefactoTabla t) {
                    sb.append("  tabla \"").append(t.titulo()).append("\": ").append(t.filas().size()).append(" filas\n");
                } else if (a instanceof ArtefactoArbol t) {
                    sb.append("  arbol \"").append(t.titulo()).append("\": raíz ").append(t.raiz().etiqueta()).append('\n');
                }
            }
        }
        sb.append("diagnosticos:\n");
        for (ResultadoFase f : fases) {
            for (Diagnostico d : f.diagnosticos()) {
                sb.append("  ").append(d.faseId()).append(" L").append(d.rango().lineaIni()).append(":C")
                        .append(d.rango().colIni()).append(" - ").append(d.mensaje()).append('\n');
            }
        }
        return sb.toString();
    }

    private static void compararConDorado(Path dorado, String actual) throws IOException {
        if (ACTUALIZAR) {
            Files.createDirectories(dorado.getParent());
            Files.writeString(dorado, actual, StandardCharsets.UTF_8);
            return;
        }
        assertTrue(Files.exists(dorado), "Falta el dorado " + dorado + " (genéralo con -Dgolden.update=true)");
        assertEquals(leer(dorado).replace("\r\n", "\n"), actual, "El resultado cambió respecto a " + dorado);
    }

    /** UTF-8 y sin BOM (regla 8 de AGENTS.md). */
    private static String leer(Path p) throws IOException {
        String s = Files.readString(p, StandardCharsets.UTF_8);
        return s.startsWith("﻿") ? s.substring(1) : s;
    }

    /** Sube desde el directorio de trabajo (ide-app/ bajo Maven) hasta encontrar ejemplos/. */
    private static Path raizDelProyecto() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isDirectory(p.resolve("ejemplos"))) p = p.getParent();
        assertTrue(p != null, "No se encontró la carpeta ejemplos/");
        return p;
    }
}
