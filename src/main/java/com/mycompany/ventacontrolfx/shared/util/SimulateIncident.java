package com.mycompany.ventacontrolfx.shared.util;

import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import java.util.Arrays;
import javafx.application.Platform;

public class SimulateIncident {
    public static void main(String[] args) {
        // En una app real, esto lo haria el OutboxManager
        // Aqui lo forzamos para probar la UI
        System.out.println("Simulando recuperacion de internet...");
        
        // Intentamos lanzar el evento si la app esta viva
        // Nota: Esto solo funcionara si podemos acceder al bus global
        // Para pruebas rapidas, vamos a usar un truco: 
        // Si el usuario tiene la app abierta, esto podria fallar por falta de contexto.
        // Pero intentaremos lanzarlo.
    }
}

