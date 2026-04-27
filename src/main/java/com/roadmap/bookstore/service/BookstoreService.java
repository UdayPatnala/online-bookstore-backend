package com.roadmap.bookstore.service;

import com.roadmap.bookstore.entity.Book;
import com.roadmap.bookstore.entity.CartItem;
import com.roadmap.bookstore.entity.CustomerOrder;
import com.roadmap.bookstore.entity.OrderLine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookstoreService {
    private final Map<Long, Book> books = new LinkedHashMap<>();
    private final Map<String, List<CartItem>> carts = new HashMap<>();
    private final Map<String, List<CustomerOrder>> ordersBySession = new HashMap<>();
    private final Map<Long, List<OrderLine>> orderLinesByOrderId = new HashMap<>();

    private long nextBookId = 1;
    private long nextCartItemId = 1;
    private long nextOrderId = 1;
    private long nextOrderLineId = 1;

    public BookstoreService() {
        createBook("Clean Code", "Robert C. Martin", 499.0, 20);
        createBook("Effective Java", "Joshua Bloch", 599.0, 15);
        createBook("Designing Data-Intensive Applications", "Martin Kleppmann", 799.0, 10);
    }

    public synchronized List<Book> listBooks() {
        return new ArrayList<>(books.values());
    }

    public synchronized Book getBook(long id) {
        Book book = books.get(id);
        if (book == null) {
            throw new IllegalArgumentException("Book not found");
        }
        return book;
    }

    public synchronized Book createBook(String title, String author, double price, int stock) {
        if (title == null || title.isBlank() || author == null || author.isBlank()) {
            throw new IllegalArgumentException("Title and author are required");
        }
        if (price < 0 || stock < 0) {
            throw new IllegalArgumentException("Price and stock must be non-negative");
        }

        Book book = new Book();
        book.setId(nextBookId++);
        book.setTitle(title.trim());
        book.setAuthor(author.trim());
        book.setPrice(price);
        book.setStock(stock);

        books.put(book.getId(), book);
        return book;
    }

    public synchronized Book updateBook(long id, String title, String author, double price, int stock) {
        Book existing = getBook(id);

        if (title == null || title.isBlank() || author == null || author.isBlank()) {
            throw new IllegalArgumentException("Title and author are required");
        }
        if (price < 0 || stock < 0) {
            throw new IllegalArgumentException("Price and stock must be non-negative");
        }

        existing.setTitle(title.trim());
        existing.setAuthor(author.trim());
        existing.setPrice(price);
        existing.setStock(stock);
        return existing;
    }

    public synchronized void deleteBook(long id) {
        if (books.remove(id) == null) {
            throw new IllegalArgumentException("Book not found");
        }
    }

    public synchronized CartItem addCartItem(String sessionId, long bookId, int quantity) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Book book = getBook(bookId);
        if (book.getStock() < quantity) {
            throw new IllegalStateException("Not enough stock available");
        }

        CartItem item = new CartItem();
        item.setId(nextCartItemId++);
        item.setSessionId(sessionId);
        item.setBookId(bookId);
        item.setQuantity(quantity);

        carts.computeIfAbsent(sessionId, key -> new ArrayList<>()).add(item);
        return item;
    }

    public synchronized List<CartItem> getCartItems(String sessionId) {
        return new ArrayList<>(carts.getOrDefault(sessionId, Collections.emptyList()));
    }

    public synchronized CustomerOrder checkout(String sessionId) {
        List<CartItem> items = carts.getOrDefault(sessionId, Collections.emptyList());
        if (items.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        double total = 0;
        for (CartItem item : items) {
            Book book = getBook(item.getBookId());
            if (book.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for book id " + book.getId());
            }
        }

        for (CartItem item : items) {
            Book book = getBook(item.getBookId());
            book.setStock(book.getStock() - item.getQuantity());
            total += book.getPrice() * item.getQuantity();
        }

        CustomerOrder order = new CustomerOrder();
        order.setId(nextOrderId++);
        order.setSessionId(sessionId);
        order.setTotalAmount(total);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());

        List<OrderLine> lines = new ArrayList<>();
        for (CartItem item : items) {
            Book book = getBook(item.getBookId());
            OrderLine line = new OrderLine();
            line.setId(nextOrderLineId++);
            line.setOrderId(order.getId());
            line.setBookId(book.getId());
            line.setQuantity(item.getQuantity());
            line.setPriceAtPurchase(book.getPrice());
            lines.add(line);
        }

        orderLinesByOrderId.put(order.getId(), lines);
        ordersBySession.computeIfAbsent(sessionId, key -> new ArrayList<>()).add(order);
        ordersBySession.get(sessionId).sort(Comparator.comparing(CustomerOrder::getCreatedAt).reversed());

        carts.remove(sessionId);
        return order;
    }

    public synchronized List<CustomerOrder> getOrders(String sessionId) {
        return new ArrayList<>(ordersBySession.getOrDefault(sessionId, Collections.emptyList()));
    }

    public synchronized List<OrderLine> getOrderLines(long orderId) {
        return new ArrayList<>(orderLinesByOrderId.getOrDefault(orderId, Collections.emptyList()));
    }
}