package leaf.ide.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import leaf.api.EventoFase;

/** Bus simple para emitir eventos de fase de compilación. */
public final class EventBus {

    private final List<Consumer<EventoFase>> suscriptores = new ArrayList<>();

    public synchronized void suscribir(Consumer<EventoFase> suscriptor) {
        suscriptores.add(suscriptor);
    }

    public synchronized void desuscribir(Consumer<EventoFase> suscriptor) {
        suscriptores.remove(suscriptor);
    }

    public void publicar(EventoFase evento) {
        List<Consumer<EventoFase>> copia;
        synchronized (this) {
            copia = new ArrayList<>(suscriptores);
        }
        for (Consumer<EventoFase> s : copia) {
            s.accept(evento);
        }
    }
}
