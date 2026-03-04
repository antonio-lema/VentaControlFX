package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SuspendedCart {
    private int id;
    private String alias;
    private int userId;
    private String username;
    private Integer clientId;
    private String clientName;
    private LocalDateTime suspendedAt;
    private double total;
    private List<SuspendedCartItem> items = new ArrayList<>();

    public SuspendedCart() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public LocalDateTime getSuspendedAt() {
        return suspendedAt;
    }

    public void setSuspendedAt(LocalDateTime suspendedAt) {
        this.suspendedAt = suspendedAt;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<SuspendedCartItem> getItems() {
        return items;
    }

    public void setItems(List<SuspendedCartItem> items) {
        this.items = items;
    }
}
