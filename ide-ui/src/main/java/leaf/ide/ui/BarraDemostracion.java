package leaf.ide.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import leaf.ide.core.demo.Demostracion;

/**
 * Controles del modo demostración: reiniciar, un paso, siguiente fase, avance automático con velocidad, y la línea de
 * narración. No sabe nada de las vistas: cada vez que cambia el paso avisa a quien la creó.
 */
final class BarraDemostracion extends JPanel {

    private static final int VELOCIDAD_INICIAL = 5;

    private final Consumer<Demostracion> alCambiar;
    private final JButton reiniciar = new JButton("⏮ Reiniciar");
    private final JButton paso = new JButton("▶ Paso");
    private final JButton fase = new JButton("⏭ Siguiente fase");
    private final JButton auto = new JButton("▶▶ Automático");
    private final JSlider velocidad = new JSlider(1, 10, VELOCIDAD_INICIAL);
    private final JLabel narracion = new JLabel(" ");
    private final Timer temporizador;
    private Demostracion demostracion;

    /** @param alCambiar se llama cuando cambia el paso actual (y al fijar una demostración nueva) */
    BarraDemostracion(Consumer<Demostracion> alCambiar) {
        super(new BorderLayout());
        this.alCambiar = alCambiar;
        temporizador = new Timer(retardo(), e -> {
            if (!avanzar()) detenerAuto();
        });
        temporizador.setRepeats(true);

        reiniciar.setToolTipText("Vuelve al principio de la demostración");
        paso.setToolTipText("Muestra un elemento más (F6)");
        fase.setToolTipText("Completa la fase actual y pasa a la siguiente (F7)");
        auto.setToolTipText("Avanza solo, con la velocidad del deslizador (F8)");
        velocidad.setToolTipText("Velocidad del avance automático");
        velocidad.setPreferredSize(new java.awt.Dimension(110, velocidad.getPreferredSize().height));
        reiniciar.addActionListener(e -> reiniciar());
        paso.addActionListener(e -> paso());
        fase.addActionListener(e -> siguienteFase());
        auto.addActionListener(e -> alternarAuto());
        velocidad.addChangeListener(e -> temporizador.setDelay(retardo()));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        botones.add(reiniciar);
        botones.add(paso);
        botones.add(fase);
        botones.add(auto);
        botones.add(new JLabel("Velocidad"));
        botones.add(velocidad);
        narracion.setFont(narracion.getFont().deriveFont(Font.BOLD));
        narracion.setBorder(BorderFactory.createEmptyBorder(2, 10, 5, 10));
        add(botones, BorderLayout.NORTH);
        add(narracion, BorderLayout.CENTER);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, javax.swing.UIManager.getColor("Component.borderColor")));
        actualizar();
    }

    /** Retardo entre pasos del modo automático: de 1000 ms (velocidad 1) a 100 ms (velocidad 10). */
    private int retardo() {
        return 1100 - velocidad.getValue() * 100;
    }

    /** Empieza una demostración nueva en su primer paso. */
    void fijar(Demostracion nueva) {
        detenerAuto();
        demostracion = nueva;
        cambio();
    }

    /** Texto de aviso (por ejemplo, mientras se compila) sin demostración cargada. */
    void aviso(String texto) {
        detenerAuto();
        demostracion = null;
        narracion.setText(texto);
        actualizar();
    }

    boolean paso() {
        return avanzar();
    }

    boolean siguienteFase() {
        if (demostracion == null || !demostracion.siguienteFase()) return false;
        cambio();
        return true;
    }

    void reiniciar() {
        if (demostracion == null) return;
        detenerAuto();
        demostracion.reiniciar();
        cambio();
    }

    void alternarAuto() {
        if (temporizador.isRunning()) {
            detenerAuto();
        } else if (demostracion != null && !demostracion.alFinal()) {
            auto.setText("⏸ Pausa");
            temporizador.start();
        }
    }

    boolean enAutomatico() {
        return temporizador.isRunning();
    }

    /** Apaga el avance automático (al salir del modo o al cambiar de demostración). */
    void detenerAuto() {
        temporizador.stop();
        auto.setText("▶▶ Automático");
    }

    /** Para las pruebas. */
    String textoDeNarracion() {
        return narracion.getText();
    }

    private boolean avanzar() {
        if (demostracion == null || !demostracion.avanzar()) return false;
        cambio();
        return true;
    }

    private void cambio() {
        Demostracion.Paso p = demostracion == null ? null : demostracion.actual();
        narracion.setText(p == null ? "No hay fases implementadas que mostrar." : p.texto());
        if (demostracion != null && demostracion.alFinal()) detenerAuto();
        actualizar();
        if (demostracion != null) alCambiar.accept(demostracion);
    }

    private void actualizar() {
        boolean hay = demostracion != null && demostracion.cantidad() > 0;
        boolean quedaPorVer = hay && !demostracion.alFinal();
        reiniciar.setEnabled(hay && !demostracion.alPrincipio());
        paso.setEnabled(quedaPorVer);
        fase.setEnabled(quedaPorVer);
        auto.setEnabled(quedaPorVer || temporizador.isRunning());
    }
}
