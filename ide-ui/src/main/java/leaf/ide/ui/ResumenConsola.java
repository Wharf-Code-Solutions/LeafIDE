package leaf.ide.ui;

import leaf.api.Estado;

/** Texto del estado de una fase en el título de su pestaña. Sin Swing para poder probarlo. */
final class ResumenConsola {

    private ResumenConsola() {}

    static String etiqueta(Estado estado) {
        return switch (estado) {
            case OK -> "OK";
            case CON_ERRORES -> "con errores";
            case OMITIDA -> "omitida";
            case NO_IMPLEMENTADA -> "próximamente";
            case FALLO_INTERNO -> "fallo interno";
        };
    }
}
