package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BusinessDay {
    private int dayOfWeek; // 1 (Mon) - 7 (Sun)
    private List<TimeRange> shifts = new ArrayList<>();
    private boolean closed;

    public BusinessDay() {
    }

    public BusinessDay(int dayOfWeek, boolean closed) {
        this.dayOfWeek = dayOfWeek;
        this.closed = closed;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public List<TimeRange> getShifts() {
        return shifts;
    }

    public void setShifts(List<TimeRange> shifts) {
        this.shifts = shifts;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void addShift(LocalTime open, LocalTime close) {
        this.shifts.add(new TimeRange(open, close));
    }

    public static class TimeRange {
        private LocalTime open;
        private LocalTime close;
        private List<Integer> assignedUserIds = new ArrayList<>();

        public TimeRange(LocalTime open, LocalTime close) {
            this.open = open;
            this.close = close;
        }

        public TimeRange(LocalTime open, LocalTime close, List<Integer> users) {
            this.open = open;
            this.close = close;
            if (users != null)
                this.assignedUserIds = new ArrayList<>(users);
        }

        public LocalTime getOpen() {
            return open;
        }

        public void setOpen(LocalTime open) {
            this.open = open;
        }

        public LocalTime getClose() {
            return close;
        }

        public void setClose(LocalTime close) {
            this.close = close;
        }

        public List<Integer> getAssignedUserIds() {
            return assignedUserIds;
        }

        public void setAssignedUserIds(List<Integer> ids) {
            this.assignedUserIds = ids != null ? ids : new ArrayList<>();
        }
    }
}
