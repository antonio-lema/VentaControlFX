package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDate;

public class StaffVacation {
    private int id;
    private int userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String type; // VACATION, MEDICAL, OTHER
    private String notes;

    public StaffVacation(int userId, LocalDate start, LocalDate end, String type, String notes) {
        this.userId = userId;
        this.startDate = start;
        this.endDate = end;
        this.type = type;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getType() {
        return type;
    }

    public String getNotes() {
        return notes;
    }
}
