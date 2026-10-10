package leaf.api;

import java.util.List;
import java.util.Map;

public record ArtefactoTexto(String titulo, String lenguaje, String contenido,
                             Map<Integer, List<String>> lineaARefs) implements Artefacto {
    public ArtefactoTexto {
        if (contenido == null) contenido = "";
        lineaARefs = Copias.mapa(lineaARefs);
    }
}
