package com.roadmap.bookstore.entity;

public class Book {

    private long id;
    private String title;
    private String author;
    private double price;
    private int stock;

    public Book() {}

    public Book(long id, String title, String author, double price, int stock) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
        this.stock = stock;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "', author='" + author
                + "', price=" + price + ", stock=" + stock + "}";
    }
}