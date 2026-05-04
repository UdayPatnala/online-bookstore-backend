package com.roadmap.bookstore;

import com.roadmap.bookstore.entity.Book;
import com.roadmap.bookstore.entity.CartItem;
import com.roadmap.bookstore.entity.CustomerOrder;
import com.roadmap.bookstore.entity.OrderLine;
import com.roadmap.bookstore.service.BookstoreService;
import com.roadmap.bookstore.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * BookstoreApplication - The "Controller" and Entry Point.
 * 
 * I built this using Java's built-in HttpServer to understand how web servers
 * work under the hood without relying on Spring Boot or other big frameworks.
 * 
 * It handles:
 * 1. Routing: Mapping URLs to Java methods.
 * 2. Parsing: Converting JSON request bodies into Java objects.
 * 3. Responding: Converting Java objects back into JSON strings.
 */
public class BookstoreApplication {

    private static final Logger LOG = Logger.getLogger(BookstoreApplication.class.getName());
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // The service handles all the "business logic". Keeping it separate
    // makes the code much cleaner and easier to test later.
    private final BookstoreService service = new BookstoreService();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        new BookstoreApplication().startServer(port);
    }

    private void startServer(int port) throws IOException {
        // Create the server on the given port. The '0' is the backlog size.
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Contexts act like "routes". Anything starting with these paths 
        // will be sent to the corresponding handler method.
        server.createContext("/health", this::handleHealth);
        server.createContext("/api/books", this::handleBooks);
        server.createContext("/api/cart", this::handleCart);
        server.createContext("/api/orders", this::handleOrders);

        // Using a ThreadPool allows the server to handle multiple requests 
        // at the same time. Without this, it would only handle one by one.
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        LOG.info(">>> Bookstore API is live at http://localhost:" + port);
        LOG.info(">>> Press Ctrl+C to stop.");
    }

    // --- Request Handlers ---

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    /**
     * Handles all /api/books/... requests.
     * I used manual path splitting to distinguish between /api/books (list/create)
     * and /api/books/{id} (get/update/delete).
     */
    private void handleBooks(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String[] parts = splitPath(exchange.getRequestURI().getPath());

            // Case: /api/books
            if (parts.length == 2) {
                if ("GET".equals(method)) {
                    sendJson(exchange, 200, booksToJson(service.listBooks()));
                    return;
                }
                if ("POST".equals(method)) {
                    Map<String, String> body = JsonUtil.parseObject(readBody(exchange));
                    Book created = service.createBook(
                            JsonUtil.requireString(body, "title"),
                            JsonUtil.requireString(body, "author"),
                            JsonUtil.requireDouble(body, "price"),
                            JsonUtil.requireInt(body, "stock"));
                    sendJson(exchange, 201, bookToJson(created));
                    return;
                }
            }

            // Case: /api/books/{id}
            if (parts.length == 3) {
                long bookId = parseId(parts[2]);

                if ("GET".equals(method)) {
                    sendJson(exchange, 200, bookToJson(service.getBook(bookId)));
                    return;
                }
                if ("PUT".equals(method)) {
                    Map<String, String> body = JsonUtil.parseObject(readBody(exchange));
                    Book updated = service.updateBook(bookId,
                            JsonUtil.requireString(body, "title"),
                            JsonUtil.requireString(body, "author"),
                            JsonUtil.requireDouble(body, "price"),
                            JsonUtil.requireInt(body, "stock"));
                    sendJson(exchange, 200, bookToJson(updated));
                    return;
                }
                if ("DELETE".equals(method)) {
                    service.deleteBook(bookId);
                    sendJson(exchange, 204, ""); // 204 No Content
                    return;
                }
            }

            sendJson(exchange, 404, "{\"error\":\"Endpoint not found\"}");

        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, errorJson(e.getMessage()));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, errorJson(e.getMessage()));
        } catch (Exception e) {
            LOG.severe("Server error: " + e.getMessage());
            sendJson(exchange, 500, errorJson("Internal server error"));
        }
    }

    private void handleCart(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String[] parts = splitPath(exchange.getRequestURI().getPath());

            // GET /api/cart/{sessionId}
            if (parts.length == 3 && "GET".equals(method)) {
                sendJson(exchange, 200, cartItemsToJson(service.getCartItems(parts[2])));
                return;
            }

            // POST /api/cart/{sessionId}/items
            if (parts.length == 4 && "items".equals(parts[3]) && "POST".equals(method)) {
                Map<String, String> body = JsonUtil.parseObject(readBody(exchange));
                CartItem item = service.addCartItem(parts[2],
                        JsonUtil.requireLong(body, "bookId"),
                        JsonUtil.requireInt(body, "quantity"));
                sendJson(exchange, 201, cartItemToJson(item));
                return;
            }

            // POST /api/cart/{sessionId}/checkout
            if (parts.length == 4 && "checkout".equals(parts[3]) && "POST".equals(method)) {
                CustomerOrder order = service.checkout(parts[2]);
                List<OrderLine> lines = service.getOrderLines(order.getId());
                sendJson(exchange, 200, orderToJson(order, lines));
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"Endpoint not found\"}");

        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, errorJson(e.getMessage()));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, errorJson(e.getMessage()));
        } catch (Exception e) {
            LOG.severe("Server error: " + e.getMessage());
            sendJson(exchange, 500, errorJson("Internal server error"));
        }
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String[] parts = splitPath(exchange.getRequestURI().getPath());

            // GET /api/orders/{sessionId}
            if (parts.length == 3 && "GET".equals(method)) {
                sendJson(exchange, 200, ordersToJson(service.getOrders(parts[2])));
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"Endpoint not found\"}");

        } catch (Exception e) {
            LOG.severe("Server error: " + e.getMessage());
            sendJson(exchange, 500, errorJson("Internal server error"));
        }
    }

    // --- Helper Methods ---

    private static String[] splitPath(String path) {
        String safe = (path == null) ? "" : path.trim();
        return Arrays.stream(safe.split("/"))
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    private static long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID format");
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        // readAllBytes() is a simple way to get the whole JSON body as a string.
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        // Enable CORS so frontend apps (like React or even a browser) can call this API.
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        // Simple logging for every request
        LOG.info(String.format("[%d] %s %s", status, exchange.getRequestMethod(), exchange.getRequestURI().getPath()));

        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    // --- JSON "Manual" Serialization ---
    // Since I'm not using Jackson or Gson, I'm building the JSON strings manually.
    // It's a bit tedious but it shows exactly how JSON is structured!

    private static String bookToJson(Book b) {
        return "{" +
                "\"id\":" + b.getId() + "," +
                "\"title\":" + JsonUtil.quote(b.getTitle()) + "," +
                "\"author\":" + JsonUtil.quote(b.getAuthor()) + "," +
                "\"price\":" + b.getPrice() + "," +
                "\"stock\":" + b.getStock() +
                "}";
    }

    private static String booksToJson(List<Book> books) {
        return listToJson(books, BookstoreApplication::bookToJson);
    }

    private static String cartItemToJson(CartItem c) {
        return "{" +
                "\"id\":" + c.getId() + "," +
                "\"sessionId\":" + JsonUtil.quote(c.getSessionId()) + "," +
                "\"bookId\":" + c.getBookId() + "," +
                "\"quantity\":" + c.getQuantity() +
                "}";
    }

    private static String cartItemsToJson(List<CartItem> items) {
        return listToJson(items, BookstoreApplication::cartItemToJson);
    }

    private static String orderSummaryToJson(CustomerOrder o) {
        return "{" +
                "\"id\":" + o.getId() + "," +
                "\"sessionId\":" + JsonUtil.quote(o.getSessionId()) + "," +
                "\"totalAmount\":" + o.getTotalAmount() + "," +
                "\"status\":" + JsonUtil.quote(o.getStatus()) + "," +
                "\"createdAt\":" + JsonUtil.quote(ISO_DT.format(o.getCreatedAt())) +
                "}";
    }

    private static String ordersToJson(List<CustomerOrder> orders) {
        return listToJson(orders, BookstoreApplication::orderSummaryToJson);
    }

    private static String orderLineToJson(OrderLine l) {
        return "{" +
                "\"id\":" + l.getId() + "," +
                "\"orderId\":" + l.getOrderId() + "," +
                "\"bookId\":" + l.getBookId() + "," +
                "\"quantity\":" + l.getQuantity() + "," +
                "\"priceAtPurchase\":" + l.getPriceAtPurchase() +
                "}";
    }

    private static String orderToJson(CustomerOrder o, List<OrderLine> lines) {
        return "{" +
                "\"id\":" + o.getId() + "," +
                "\"sessionId\":" + JsonUtil.quote(o.getSessionId()) + "," +
                "\"totalAmount\":" + o.getTotalAmount() + "," +
                "\"status\":" + JsonUtil.quote(o.getStatus()) + "," +
                "\"createdAt\":" + JsonUtil.quote(ISO_DT.format(o.getCreatedAt())) + "," +
                "\"lines\":" + listToJson(lines, BookstoreApplication::orderLineToJson) +
                "}";
    }

    /**
     * Generic helper to turn a List of objects into a JSON array string.
     */
    private static <T> String listToJson(List<T> items,
                                         java.util.function.Function<T, String> toJson) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(toJson.apply(items.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String errorJson(String message) {
        return "{\"error\":" + JsonUtil.quote(message) + "}";
    }
}
