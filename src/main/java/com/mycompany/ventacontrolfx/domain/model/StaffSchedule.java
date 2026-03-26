package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalTime;

public class StaffSchedule {
    private int userId;
    private int dayOfWeek; // 1-7
    private LocalTime startTime;
    private LocalTime endTime;

    public StaffSchedule(int userId, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getUserId() {
        return userId;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
