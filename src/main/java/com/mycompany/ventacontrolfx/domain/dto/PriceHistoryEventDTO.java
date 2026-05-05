package com.mycompany.ventacontrolfx.domain.dto;

import java.time.LocalDateTime;

public class PriceHistoryEventDTO {
    public enum EventType {
        BULK_UPDATE, MANUAL_CHANGE
    }

    private final EventType type;
    private final LocalDateTime timestamp;
    private final String title;
    private final String description;
    private final String reason;
    private final String details; // e.g., "Afecado a 20 productos" or "Anterior: 10.00 \u20ac"
    private final String targetName;
    private final Integer logId;

    public PriceHistoryEventDTO(EventType type, LocalDateTime timestamp, String title, String description,
            String reason, String details, String targetName, Integer logId) {
        this.type = type;
        this.timestamp = timestamp;
        this.title = title;
        this.description = description;
        this.reason = reason;
        this.details = details;
        this.targetName = targetName;
        this.logId = logId;
    }

    public EventType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public String getTargetName() {
        return targetName;
    }

    public Integer getLogId() {
        return logId;
    }
}
