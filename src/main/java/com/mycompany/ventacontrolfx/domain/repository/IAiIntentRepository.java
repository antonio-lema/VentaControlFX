package com.mycompany.ventacontrolfx.domain.repository;

import java.util.Map;

public interface IAiIntentRepository {
    void logIntent(String intent, Map<String, Object> payload, Map<String, Object> result, Integer userId,
            Integer cashierId, String intentId);
}

