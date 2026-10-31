package leaf.ide.core;

import java.nio.file.Path;

/**
 * Qué compilador usa el IDE: el de una carpeta del disco (el compilador base, por ejemplo {@code C:\Leaf}, con sus
 * {@code .class}) o la copia que el IDE lleva dentro. Lo implementa el proveedor que arma {@code ide-app}; la ventana
 * lo usa para el menú y la barra de estado sin saber nada del compilador.
 */
public interface SelectorCompilador {

    /** {@code true} si se usa el compilador de {@link #carpeta()}; {@code false} si se usa la copia del IDE. */
    boolean usaCarpeta();

    /** Carpeta del compilador base (la última elegida, aunque ahora se use la copia del IDE). */
    Path carpeta();

    /** A partir de la próxima compilación se usa el compilador de esa carpeta. */
    void usarCarpeta(Path carpeta);

    /** A partir de la próxima compilación se usa la copia del IDE. */
    void usarCopiaDelIde();

    /** Texto corto para la barra de estado, por ejemplo {@code dd0e608 · C:\Leaf\src} o {@code a864ec7 · copia del IDE}. */
    String descripcion();
}
