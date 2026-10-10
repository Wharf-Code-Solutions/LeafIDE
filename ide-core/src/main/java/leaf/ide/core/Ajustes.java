package leaf.ide.core;

/**
 * Preferencias del usuario que sobreviven al cerrar el IDE.
 *
 * @param temaOscuro   tema oscuro encendido
 * @param analisisEnVivo análisis automático mientras se escribe
 * @param resaltado    resaltado de sintaxis encendido
 * @param perspectiva  nombre de la última perspectiva elegida (ver {@code Perspectiva} en la UI)
 */
public record Ajustes(boolean temaOscuro, boolean analisisEnVivo, boolean resaltado, String perspectiva) {

    public static final String PERSPECTIVA_INICIAL = "ANALISIS";

    public static Ajustes porDefecto() {
        return new Ajustes(false, true, true, PERSPECTIVA_INICIAL);
    }

    public Ajustes conTemaOscuro(boolean valor) {
        return new Ajustes(valor, analisisEnVivo, resaltado, perspectiva);
    }

    public Ajustes conAnalisisEnVivo(boolean valor) {
        return new Ajustes(temaOscuro, valor, resaltado, perspectiva);
    }

    public Ajustes conResaltado(boolean valor) {
        return new Ajustes(temaOscuro, analisisEnVivo, valor, perspectiva);
    }

    public Ajustes conPerspectiva(String valor) {
        return new Ajustes(temaOscuro, analisisEnVivo, resaltado, valor);
    }
}
