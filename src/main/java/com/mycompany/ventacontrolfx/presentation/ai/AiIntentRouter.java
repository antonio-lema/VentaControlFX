package com.mycompany.ventacontrolfx.presentation.ai;

import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import java.util.Map;

public class AiIntentRouter {
    private final ServiceContainer container;

    public AiIntentRouter(ServiceContainer container) {
        this.container = container;
    }

    public Object handleIntent(String intent, Map<String, Object> payload) {
        return null; // Implementation in flows
    }
}

