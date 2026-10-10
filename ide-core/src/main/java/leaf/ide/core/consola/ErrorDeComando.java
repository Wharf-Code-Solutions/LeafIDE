package leaf.ide.core.consola;

/** Un comando que no se pudo cumplir; su mensaje es lo que ve el usuario (no es un fallo del programa). */
public final class ErrorDeComando extends RuntimeException {

    public ErrorDeComando(String mensaje) {
        super(mensaje);
    }
}
