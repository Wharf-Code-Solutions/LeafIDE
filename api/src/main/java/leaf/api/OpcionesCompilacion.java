package leaf.api;

/**
 * (Aditivo, M2) Opciones de una compilación.
 *
 * @param hastaFaseId última fase a ejecutar (con sus dependencias); {@code null} = todas
 * @param ejecutarSemanticoConErroresSintacticos si {@code true}, la fase semántica corre aunque haya errores
 *                                               léxicos o sintácticos
 */
public record OpcionesCompilacion(String hastaFaseId, boolean ejecutarSemanticoConErroresSintacticos) {

    public static OpcionesCompilacion porDefecto() {
        return new OpcionesCompilacion(null, true);
    }

    public static OpcionesCompilacion hasta(String faseId) {
        return new OpcionesCompilacion(faseId, true);
    }
}
