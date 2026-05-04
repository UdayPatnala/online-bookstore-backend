# Online Bookstore Backend (Pure Java)

I built this project to learn how to create a REST API from scratch using only **Core Java**. No Spring Boot, no Maven, and no Hibernate—just pure logic and the built-in Java libraries.

## Why I built this?
Most people start with Spring Boot, but I wanted to understand:
1. How HTTP servers actually listen for requests.
2. How to handle routing and path variables manually.
3. How to manage data and thread safety in an in-memory "database".
4. How JSON parsing works under the hood.

## What I Learned
- **`com.sun.net.httpserver`**: Learned how to set up an HTTP server and create "contexts" for different routes.
- **Multithreading**: Used a `FixedThreadPool` and `synchronized` methods to ensure the app doesn't crash or corrupt data when multiple users access it at once.
- **REST Principles**: Implemented standard CRUD (Create, Read, Update, Delete) and proper HTTP status codes (200, 201, 204, 400, 404, 409).
- **Atomic Operations**: The checkout logic ensures that stock is only deducted if ALL items are available, preventing partial orders.
- **Manual JSON**: Since I didn't use Jackson, I wrote my own `JsonUtil` and manual serialization logic to understand the JSON format.

## Features
- **Book Catalog**: Full CRUD operations for managing books.
- **Shopping Cart**: Session-based cart (uses a `sessionId` string to track users).
- **Checkout**: Validates stock, calculates total, creates an order, and clears the cart.
- **In-Memory Storage**: Data persists as long as the server is running.

## Project Structure
```
src/main/java/com/roadmap/bookstore/
├── BookstoreApplication.java    # Server setup + Routing
├── service/
│   └── BookstoreService.java    # Business logic & In-memory data
├── entity/                      # Data models (Book, CartItem, Order, etc.)
├── util/
│   └── JsonUtil.java            # Simple manual JSON helper
```

## How to Run
1. Make sure you have Java 17+ installed.
2. Run the build script:
   ```powershell
   .\run.ps1
   ```
3. The API will be available at `http://localhost:8080`.

## API Endpoints

### Books
- `GET /api/books` - List all books
- `POST /api/books` - Create a new book
- `GET /api/books/{id}` - Get book details
- `PUT /api/books/{id}` - Update a book
- `DELETE /api/books/{id}` - Delete a book

### Cart & Orders
- `GET /api/cart/{sessionId}` - View current cart
- `POST /api/cart/{sessionId}/items` - Add a book to cart
- `POST /api/cart/{sessionId}/checkout` - Place an order
- `GET /api/orders/{sessionId}` - View order history

## License
MIT