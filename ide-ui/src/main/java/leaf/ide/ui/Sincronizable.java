package leaf.ide.ui;

/** Una vista que sigue al cursor del editor (por ejemplo, selecciona el token o el nodo que está bajo el cursor). */
interface Sincronizable {

    /** El cursor está en (línea, columna), ambas desde 1. No debe volver a pedir nada al editor. */
    void cursorEn(int linea, int columna);
}
