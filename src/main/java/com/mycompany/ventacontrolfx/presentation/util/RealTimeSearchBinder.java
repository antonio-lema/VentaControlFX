package com.mycompany.ventacontrolfx.presentation.util;

import javafx.scene.control.TextField;
import java.util.function.Consumer;

/**
 * Binder central para aplicar el comportamiento de b\u00fasqueda en tiempo real
 * a cualquier campo de texto de la aplicaci\u00f3n.
 */
public class RealTimeSearchBinder {

    /**
     * Vincula un TextField con una acci\u00f3n de b\u00fasqueda aplicando un debounce
     * est\u00e1ndar de 300ms.
     * 
     * @param searchField    El campo de texto de b\u00fasqueda.
     * @param onSearchAction La acci\u00f3n a ejecutar (normalmente una llamada a un caso
     *                       de uso).
     */
    public static void bind(TextField searchField, Consumer<String> onSearchAction) {
        bind(searchField, 300, onSearchAction);
    }

    /**
     * Vincula un TextField con una acci\u00f3n de b\u00fasqueda con delay personalizable.
     */
    public static void bind(TextField searchField, double delayMs, Consumer<String> onSearchAction) {
        SearchDebouncer debouncer = new SearchDebouncer(delayMs, onSearchAction);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // No disparamos b\u00fasqueda si el texto es nulo (por seguridad)
            String query = (newValue == null) ? "" : newValue.trim();
            debouncer.feed(query);
        });
    }
}
