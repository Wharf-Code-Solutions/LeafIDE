package leaf.api;

import java.util.List;

public record ArtefactoTabla(String titulo, List<String> columnas, List<Fila> filas) implements Artefacto {
    public ArtefactoTabla {
        columnas = Copias.lista(columnas);
        filas = Copias.lista(filas);
    }
}
