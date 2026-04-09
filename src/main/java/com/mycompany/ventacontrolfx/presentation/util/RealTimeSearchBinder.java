package com.mycompany.ventacontrolfx.presentation.util;

import javafx.scene.control.TextField;
import java.util.function.Consumer;

/**
 * Binder central para aplicar el comportamiento de b\u00c3\u00basqueda en tiempo real
 * a cualquier campo de texto de la aplicaci\u00c3\u00b3n.
 */
public class RealTimeSearchBinder {

    /**
     * Vincula un TextField con una acci\u00c3\u00b3n de b\u00c3\u00basqueda aplicando un debounce
     * est\u00c3\u00a1ndar de 300ms.
     * 
     * @param searchField    El campo de texto de b\u00c3\u00basqueda.
     * @param onSearchAction La acci\u00c3\u00b3n a ejecutar (normalmente una llamada a un caso
     *                       de uso).
     */
    public static void bind(TextField searchField, Consumer<String> onSearchAction) {
        bind(searchField, 300, onSearchAction);
    }

    /**
     * Vincula un TextField con una acci\u00c3\u00b3n de b\u00c3\u00basqueda con delay personalizable.
     */
    public static void bind(TextField searchField, double delayMs, Consumer<String> onSearchAction) {
        SearchDebouncer debouncer = new SearchDebouncer(delayMs, onSearchAction);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // No disparamos b\u00c3\u00basqueda si el texto es nulo (por seguridad)
            String query = (newValue == null) ? "" : newValue.trim();
            debouncer.feed(query);
        });
    }
}
