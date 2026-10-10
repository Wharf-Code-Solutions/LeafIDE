package leaf.ide.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lectura y escritura de programas Leaf: siempre UTF-8, sin BOM (regla 8 de AGENTS.md). */
public final class ArchivosLeaf {

    public static final String EXTENSION = ".leaf";

    private ArchivosLeaf() {}

    public static String leer(Path archivo) throws IOException {
        String texto = Files.readString(archivo, StandardCharsets.UTF_8);
        return texto.startsWith("﻿") ? texto.substring(1) : texto;
    }

    public static void escribir(Path archivo, String texto) throws IOException {
        Files.writeString(archivo, texto, StandardCharsets.UTF_8);
    }

    /** Si el nombre no tiene extensión se añade {@code .leaf}; {@code .txt} y cualquier otra se respetan. */
    public static Path conExtension(Path archivo) {
        String nombre = archivo.getFileName().toString();
        return nombre.contains(".") ? archivo : archivo.resolveSibling(nombre + EXTENSION);
    }
}
