package leaf.ide.ui;

/** Disposiciones predefinidas de los paneles (las "pantallas" elegibles). */
enum Perspectiva {
    ESCRITURA("Escritura", "Editor grande con Problemas y consolas"),
    ANALISIS("Análisis", "Editor a la izquierda y las fases de análisis en pestañas"),
    SINTESIS("Síntesis", "Editor y las fases de generación lado a lado"),
    EVALUACION("Evaluación (3 pantallas)", "Pantalla 1: editor, consolas y análisis · 2: intermedio y optimización · 3: objeto");

    private final String nombre;
    private final String descripcion;

    Perspectiva(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    String nombre() {
        return nombre;
    }

    String descripcion() {
        return descripcion;
    }

    static Perspectiva de(String nombreEnum, Perspectiva porDefecto) {
        for (Perspectiva p : values()) if (p.name().equals(nombreEnum)) return p;
        return porDefecto;
    }
}
