package com.roadmap.bookstore.entity;

import java.time.LocalDateTime;

public class CustomerOrder {

    private long id;
    private String sessionId;
    private double totalAmount;
    private String status;
    private LocalDateTime createdAt;

    public CustomerOrder() {}

    public CustomerOrder(long id, String sessionId, double totalAmount,
                         String status, LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", session='" + sessionId
                + "', total=" + totalAmount + ", status='" + status
                + "', at=" + createdAt + "}";
    }
}