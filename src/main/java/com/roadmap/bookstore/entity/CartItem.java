package com.roadmap.bookstore.entity;

public class CartItem {

    private long id;
    private String sessionId;
    private long bookId;
    private int quantity;

    public CartItem() {}

    public CartItem(long id, String sessionId, long bookId, int quantity) {
        this.id = id;
        this.sessionId = sessionId;
        this.bookId = bookId;
        this.quantity = quantity;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() {
        return "CartItem{id=" + id + ", session='" + sessionId
                + "', bookId=" + bookId + ", qty=" + quantity + "}";
    }
}