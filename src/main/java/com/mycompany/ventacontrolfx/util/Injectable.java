package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.service.ServiceContainer;

/**
 * Interface for controllers that require the ServiceContainer for dependency
 * injection.
 */
public interface Injectable {
    void inject(ServiceContainer container);
}
