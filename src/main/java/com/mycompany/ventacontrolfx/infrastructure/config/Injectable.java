package com.mycompany.ventacontrolfx.infrastructure.config;

/**
 * Interface for controllers that require the ServiceContainer for dependency
 * injection.
 */
public interface Injectable {
    void inject(ServiceContainer container);
}

