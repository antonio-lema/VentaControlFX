package com.mycompany.ventacontrolfx.presentation.ai;

import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import java.util.Map;

public class AiSkillDispatcher {
    private final ServiceContainer container;

    public AiSkillDispatcher(ServiceContainer container) {
        this.container = container;
    }

    public Object dispatch(String skillName, Map<String, Object> parameters) {
        // Dispatch logic for AI skills
        return null;
    }
}
