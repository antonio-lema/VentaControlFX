package com.mycompany.ventacontrolfx.presentation.util;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.util.function.Consumer;

/**
 * Utilidad para retrasar la ejecuci\u00c3\u00b3n de una acci\u00c3\u00b3n hasta que el usuario
 * deje de escribir por un tiempo determinado (Debounce).
 * Evita la saturaci\u00c3\u00b3n del hilo de UI y de la base de datos.
 */
public class SearchDebouncer {
    private final PauseTransition delay;
    private Consumer<String> action;

    public SearchDebouncer(double delayMs, Consumer<String> action) {
        this.delay = new PauseTransition(Duration.millis(delayMs));
        this.action = action;
    }

    /**
     * Alimenta el debouncer con el nuevo texto.
     * Reinicia el temporizador.
     */
    public void feed(String text) {
        delay.setOnFinished(event -> javafx.application.Platform.runLater(() -> action.accept(text)));
        delay.playFromStart();
    }

    public void setAction(Consumer<String> action) {
        this.action = action;
    }
}
