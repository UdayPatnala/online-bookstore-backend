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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class BookstoreApplication {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BookstoreService service = new BookstoreService();

    public static void main(String[] args) throws IOException {
        BookstoreApplication app = new BookstoreApplication();
        app.start(8080);
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/api/books", this::handleBooks);
        server.createContext("/api/cart", this::handleCart);
        server.createContext("/api/orders", this::handleOrders);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Bookstore API started on http://localhost:" + port);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void handleBooks(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String[] parts = splitPath(exchange.getRequestURI().getPath());

            if (parts.length == 2) {
                if ("GET".equals(method)) {
                    List<Book> books = service.listBooks();
                    sendJson(exchange, 200, booksToJson(books));
                    return;
                }

                if ("POST".equals(method)) {
                    Map<String, String> payload = JsonUtil.parseObject(readBody(exchange));
                    Book created = service.createBook(
                            JsonUtil.requireString(payload, "title"),
                            JsonUtil.requireString(payload, "author"),
                            JsonUtil.requireDouble(payload, "price"),
                            JsonUtil.requireInt(payload, "stock")
                    );
                    sendJson(exchange, 201, bookToJson(created));
                    return;
                }
            }

            if (parts.length == 3) {
                long bookId = parseId(parts[2], "book id");

                if ("GET".equals(method)) {
                    Book book = service.getBook(bookId);
                    sendJson(exchange, 200, bookToJson(book));
                    return;
                }

                if ("PUT".equals(method)) {
                    Map<String, String> payload = JsonUtil.parseObject(readBody(exchange));
                    Book updated = service.updateBook(
                            bookId,
                            JsonUtil.requireString(payload, "title"),
                            JsonUtil.requireString(payload, "author"),
                            JsonUtil.requireDouble(payload, "price"),
                            JsonUtil.requireInt(payload, "stock")
                    );
                    sendJson(exchange, 200, bookToJson(updated));
                    return;
                }

                if ("DELETE".equals(method)) {
                    service.deleteBook(bookId);
                    sendJson(exchange, 204, "");
                    return;
                }
            }

            sendJson(exchange, 404, "{\"error\":\"Endpoint not found\"}");
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, errorJson(exception.getMessage()));
        } catch (IllegalStateException exception) {
            sendJson(exchange, 409, errorJson(exception.getMessage()));
        } catch (Exception exception) {
            sendJson(exchange, 500, errorJson("Internal server error"));
        }
    }

    private void handleCart(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String[] parts = splitPath(exchange.getRequestURI().getPath());

            if (parts.length == 3 && "GET".equals(method)) {
                String sessionId = parts[2];
                List<CartItem> items = service.getCartItems(sessionId);
                sendJson(exchange, 200, cartItemsToJson(items));
                return;
            }

            if (parts.length == 4 && "items".equals(parts[3]) && "POST".equals(method)) {
                String sessionId = parts[2];
                Map<String, String> payload = JsonUtil.parseObject(readBody(exchange));

                CartItem item = service.addCartItem(
                        sessionId,
                        JsonUtil.requireLong(payload, "bookId"),
                        JsonUtil.requireInt(payload, "quantity")
                );

                sendJson(exchange, 201, cartItemToJson(item));
                return;
            }

            if (parts.length == 4 && "checkout".equals(parts[3]) && "POST".equals(method)) {
                String sessionId = parts[2];
                CustomerOrder order = service.checkout(sessionId);
                List<OrderLine> lines = service.getOrderLines(order.getId());
                sendJson(exchange, 200, orderToJson(order, lines));
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"Endpoint not found\"}");
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, errorJson(exception.getMessage()));
        } catch (IllegalStateException exception) {
            sendJson(exchange, 409, errorJson(exception.getMessage()));
        } catch (Exception exception) {
            sendJson(exchange, 500, errorJson("Internal server error"));
        }
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String[] parts = splitPath(exchange.getRequestURI().getPath());

            if (parts.length == 3 && "GET".equals(method)) {
                String sessionId = parts[2];
                List<CustomerOrder> orders = service.getOrders(sessionId);
                sendJson(exchange, 200, ordersToJson(orders));
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"Endpoint not found\"}");
        } catch (Exception exception) {
            sendJson(exchange, 500, errorJson("Internal server error"));
        }
    }

    private static String[] splitPath(String path) {
        String safePath = path == null ? "" : path.trim();
        return Arrays.stream(safePath.split("/"))
                .filter(segment -> !segment.isBlank())
                .toArray(String[]::new);
    }

    private static long parseId(String raw, String fieldName) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid " + fieldName);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream stream = exchange.getRequestBody();
        byte[] body = stream.readAllBytes();
        return new String(body, StandardCharsets.UTF_8).trim();
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (statusCode == 204) {
            exchange.sendResponseHeaders(statusCode, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }

    private static String booksToJson(List<Book> books) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < books.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(bookToJson(books.get(i)));
        }
        return builder.append(']').toString();
    }

    private static String bookToJson(Book book) {
        return "{" +
                "\"id\":" + book.getId() + "," +
                "\"title\":" + JsonUtil.quote(book.getTitle()) + "," +
                "\"author\":" + JsonUtil.quote(book.getAuthor()) + "," +
                "\"price\":" + book.getPrice() + "," +
                "\"stock\":" + book.getStock() +
                "}";
    }

    private static String cartItemsToJson(List<CartItem> items) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(cartItemToJson(items.get(i)));
        }
        return builder.append(']').toString();
    }

    private static String cartItemToJson(CartItem item) {
        return "{" +
                "\"id\":" + item.getId() + "," +
                "\"sessionId\":" + JsonUtil.quote(item.getSessionId()) + "," +
                "\"bookId\":" + item.getBookId() + "," +
                "\"quantity\":" + item.getQuantity() +
                "}";
    }

    private static String ordersToJson(List<CustomerOrder> orders) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(orderSummaryToJson(orders.get(i)));
        }
        return builder.append(']').toString();
    }

    private static String orderSummaryToJson(CustomerOrder order) {
        return "{" +
                "\"id\":" + order.getId() + "," +
                "\"sessionId\":" + JsonUtil.quote(order.getSessionId()) + "," +
                "\"totalAmount\":" + order.getTotalAmount() + "," +
                "\"status\":" + JsonUtil.quote(order.getStatus()) + "," +
                "\"createdAt\":" + JsonUtil.quote(DATE_TIME_FORMATTER.format(order.getCreatedAt())) +
                "}";
    }

    private static String orderToJson(CustomerOrder order, List<OrderLine> lines) {
        StringBuilder linesJson = new StringBuilder("[");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                linesJson.append(',');
            }
            OrderLine line = lines.get(i);
            linesJson.append("{")
                    .append("\"id\":").append(line.getId()).append(',')
                    .append("\"orderId\":").append(line.getOrderId()).append(',')
                    .append("\"bookId\":").append(line.getBookId()).append(',')
                    .append("\"quantity\":").append(line.getQuantity()).append(',')
                    .append("\"priceAtPurchase\":").append(line.getPriceAtPurchase())
                    .append("}");
        }
        linesJson.append(']');

        return "{" +
                "\"id\":" + order.getId() + "," +
                "\"sessionId\":" + JsonUtil.quote(order.getSessionId()) + "," +
                "\"totalAmount\":" + order.getTotalAmount() + "," +
                "\"status\":" + JsonUtil.quote(order.getStatus()) + "," +
                "\"createdAt\":" + JsonUtil.quote(DATE_TIME_FORMATTER.format(order.getCreatedAt())) + "," +
                "\"lines\":" + linesJson +
                "}";
    }

    private static String errorJson(String message) {
        return "{\"error\":" + JsonUtil.quote(message) + "}";
    }
}
