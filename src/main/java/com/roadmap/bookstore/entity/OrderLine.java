package com.roadmap.bookstore.entity;

public class OrderLine {

    private long id;
    private long orderId;
    private long bookId;
    private int quantity;
    private double priceAtPurchase; // snapshot of price when order was placed

    public OrderLine() {}

    public OrderLine(long id, long orderId, long bookId,
                     int quantity, double priceAtPurchase) {
        this.id = id;
        this.orderId = orderId;
        this.bookId = bookId;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPriceAtPurchase() { return priceAtPurchase; }
    public void setPriceAtPurchase(double priceAtPurchase) { this.priceAtPurchase = priceAtPurchase; }

    @Override
    public String toString() {
        return "OrderLine{id=" + id + ", orderId=" + orderId
                + ", bookId=" + bookId + ", qty=" + quantity
                + ", price=" + priceAtPurchase + "}";
    }
}