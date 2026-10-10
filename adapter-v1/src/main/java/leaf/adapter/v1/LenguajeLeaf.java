package leaf.adapter.v1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import leaf.api.LenguajeDef;

/** Arma el {@link LenguajeDef} desde {@code leaf-lenguaje.json}, que viaja con este adaptador (versión del compilador). */
final class LenguajeLeaf {

    static final String RECURSO = "/leaf-lenguaje.json";

    private LenguajeLeaf() {}

    static LenguajeDef cargar() {
        try (InputStream in = LenguajeLeaf.class.getResourceAsStream(RECURSO)) {
            if (in == null) throw new IllegalStateException("Falta el recurso " + RECURSO);
            try (Reader lector = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return desde(JsonParser.parseReader(lector).getAsJsonObject());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + RECURSO, e);
        }
    }

    private static LenguajeDef desde(JsonObject json) {
        List<LenguajeDef.GrupoPalabras> grupos = new ArrayList<>();
        if (json.has("palabras")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("palabras").entrySet()) {
                grupos.add(new LenguajeDef.GrupoPalabras(e.getKey(), lista(e.getValue())));
            }
        }
        JsonObject bloque = json.has("comentarioBloque") ? json.getAsJsonObject("comentarioBloque") : new JsonObject();
        return new LenguajeDef(
                texto(json, "nombre"),
                json.has("extensiones") ? lista(json.get("extensiones")) : List.of(),
                grupos,
                json.has("operadores") ? lista(json.get("operadores")) : List.of(),
                json.has("delimitadores") ? lista(json.get("delimitadores")) : List.of(),
                texto(json, "comentarioLinea"),
                texto(bloque, "inicio"),
                texto(bloque, "fin"),
                texto(json, "prefijoVariable"));
    }

    private static String texto(JsonObject objeto, String clave) {
        return objeto.has(clave) ? objeto.get(clave).getAsString() : "";
    }

    private static List<String> lista(JsonElement elemento) {
        JsonArray arreglo = elemento.getAsJsonArray();
        List<String> salida = new ArrayList<>(arreglo.size());
        for (JsonElement e : arreglo) salida.add(e.getAsString());
        return salida;
    }
}
