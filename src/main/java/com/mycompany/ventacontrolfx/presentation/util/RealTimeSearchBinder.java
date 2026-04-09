package com.mycompany.ventacontrolfx.presentation.util;

import javafx.scene.control.TextField;
import java.util.function.Consumer;

/**
 * Binder central para aplicar el comportamiento de bÃºsqueda en tiempo real
 * a cualquier campo de texto de la aplicaciÃ³n.
 */
public class RealTimeSearchBinder {

    /**
     * Vincula un TextField con una acciÃ³n de bÃºsqueda aplicando un debounce
     * estÃ¡ndar de 300ms.
     * 
     * @param searchField    El campo de texto de bÃºsqueda.
     * @param onSearchAction La acciÃ³n a ejecutar (normalmente una llamada a un caso
     *                       de uso).
     */
    public static void bind(TextField searchField, Consumer<String> onSearchAction) {
        bind(searchField, 300, onSearchAction);
    }

    /**
     * Vincula un TextField con una acciÃ³n de bÃºsqueda con delay personalizable.
     */
    public static void bind(TextField searchField, double delayMs, Consumer<String> onSearchAction) {
        SearchDebouncer debouncer = new SearchDebouncer(delayMs, onSearchAction);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // No disparamos bÃºsqueda si el texto es nulo (por seguridad)
            String query = (newValue == null) ? "" : newValue.trim();
            debouncer.feed(query);
        });
    }
}
