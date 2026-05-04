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

/**
 * BookstoreService - The "Engine" of the application.
 * 
 * Since I'm not using a database, I store everything in memory using Maps.
 * I used 'synchronized' on all methods to make sure the app is "thread-safe"
 * (so two people can't buy the same last book at the exact same millisecond).
 */
public class BookstoreService {

    // Storage: using LinkedHashMap for books to keep them in the order they were added.
    private final Map<Long, Book> books = new LinkedHashMap<>();
    private final Map<String, List<CartItem>> carts = new HashMap<>();
    private final Map<String, List<CustomerOrder>> ordersBySession = new HashMap<>();
    private final Map<Long, List<OrderLine>> orderLines = new HashMap<>();

    // ID Generators
    private long nextBookId = 1;
    private long nextCartItemId = 1;
    private long nextOrderId = 1;
    private long nextOrderLineId = 1;

    public BookstoreService() {
        // Initial "Seed" data so the app isn't empty when it starts.
        seedBook("Clean Code", "Robert C. Martin", 499.0, 20);
        seedBook("Effective Java", "Joshua Bloch", 599.0, 15);
        seedBook("Designing Data-Intensive Applications", "Martin Kleppmann", 799.0, 10);
    }

    // --- Book Management ---

    public synchronized List<Book> listBooks() {
        return new ArrayList<>(books.values());
    }

    public synchronized Book getBook(long id) {
        Book book = books.get(id);
        if (book == null) throw new IllegalArgumentException("Book ID " + id + " not found");
        return book;
    }

    public synchronized Book createBook(String title, String author, double price, int stock) {
        validateBookFields(title, author, price, stock);
        Book book = new Book(nextBookId++, title.trim(), author.trim(), price, stock);
        books.put(book.getId(), book);
        return book;
    }

    public synchronized Book updateBook(long id, String title, String author, double price, int stock) {
        Book existing = getBook(id);
        validateBookFields(title, author, price, stock);
        
        existing.setTitle(title.trim());
        existing.setAuthor(author.trim());
        existing.setPrice(price);
        existing.setStock(stock);
        return existing;
    }

    public synchronized void deleteBook(long id) {
        if (books.remove(id) == null) {
            throw new IllegalArgumentException("Cannot delete: Book ID " + id + " not found");
        }
    }

    // --- Shopping Cart ---

    public synchronized CartItem addCartItem(String sessionId, long bookId, int quantity) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID is required for the cart");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Book book = getBook(bookId);
        if (book.getStock() < quantity) {
            throw new IllegalStateException("Sorry, only " + book.getStock() + " copies left in stock");
        }

        CartItem item = new CartItem(nextCartItemId++, sessionId, bookId, quantity);
        carts.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(item);
        return item;
    }

    public synchronized List<CartItem> getCartItems(String sessionId) {
        return new ArrayList<>(carts.getOrDefault(sessionId, Collections.emptyList()));
    }

    // --- Checkout Logic ---
    // This is the most complex part. It has to be an "atomic" operation.

    public synchronized CustomerOrder checkout(String sessionId) {
        List<CartItem> items = carts.getOrDefault(sessionId, Collections.emptyList());
        if (items.isEmpty()) {
            throw new IllegalStateException("Your cart is empty!");
        }

        // Step 1: Validate stock for ALL items before we start deducting anything.
        // If one item fails, the whole checkout fails (all-or-nothing).
        for (CartItem item : items) {
            Book book = getBook(item.getBookId());
            if (book.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for: " + book.getTitle());
            }
        }

        // Step 2: Deduct stock and calculate the grand total.
        double total = 0;
        for (CartItem item : items) {
            Book book = getBook(item.getBookId());
            book.setStock(book.getStock() - item.getQuantity());
            total += book.getPrice() * item.getQuantity();
        }

        // Step 3: Create the Order record.
        CustomerOrder order = new CustomerOrder(
                nextOrderId++, sessionId, total, "PLACED", LocalDateTime.now());

        // Step 4: Create Order Lines (snapshots of what was bought and at what price).
        List<OrderLine> lines = new ArrayList<>();
        for (CartItem item : items) {
            Book book = getBook(item.getBookId());
            lines.add(new OrderLine(
                    nextOrderLineId++, order.getId(), book.getId(),
                    item.getQuantity(), book.getPrice()));
        }

        // Save everything and clear the cart.
        orderLines.put(order.getId(), lines);
        ordersBySession.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(order);
        
        // Sort orders by newest first.
        ordersBySession.get(sessionId)
                .sort(Comparator.comparing(CustomerOrder::getCreatedAt).reversed());

        carts.remove(sessionId);
        return order;
    }

    // --- Queries ---

    public synchronized List<CustomerOrder> getOrders(String sessionId) {
        return new ArrayList<>(ordersBySession.getOrDefault(sessionId, Collections.emptyList()));
    }

    public synchronized List<OrderLine> getOrderLines(long orderId) {
        return new ArrayList<>(orderLines.getOrDefault(orderId, Collections.emptyList()));
    }

    // --- Internal Helpers ---

    private void validateBookFields(String title, String author, double price, int stock) {
        if (title == null || title.isBlank() || author == null || author.isBlank()) {
            throw new IllegalArgumentException("Book title and author cannot be empty");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
    }

    private void seedBook(String title, String author, double price, int stock) {
        Book book = new Book(nextBookId++, title, author, price, stock);
        books.put(book.getId(), book);
    }
}