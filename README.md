# Online Bookstore — REST API (Java)

A backend REST API for an online bookstore with book CRUD, session-based shopping cart, and checkout workflow. Built with pure Java `HttpServer` — no Spring, no Maven, no external dependencies.

## Tech Stack

- **Java 17+**
- **Built-in HTTP server** (`com.sun.net.httpserver.HttpServer`)
- **In-memory data store** (concurrent-safe maps)

## Architecture

```
                    ┌─────────────────────────┐
  HTTP Request ───▶ │  BookstoreApplication    │
                    │  (routing + serialization)│
                    └────────┬────────────────┘
                             │
                             ▼
                    ┌─────────────────────────┐
                    │   BookstoreService       │
                    │   (business logic)       │
                    │                          │
                    │  Books  │ Carts │ Orders │
                    └─────────────────────────┘

  Entities: Book, CartItem, CustomerOrder, OrderLine
  Utility:  JsonUtil (manual JSON parser — no Jackson)
```

## Run

```powershell
.\run.ps1
```

Server starts at `http://localhost:8080`

## API Reference

### Health Check

```
GET /health
→ { "status": "ok" }
```

### Books

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/books` | List all books |
| GET | `/api/books/{id}` | Get book by ID |
| POST | `/api/books` | Create a book |
| PUT | `/api/books/{id}` | Update a book |
| DELETE | `/api/books/{id}` | Delete a book |

**Create / Update body:**

```json
{
  "title": "Refactoring",
  "author": "Martin Fowler",
  "price": 650,
  "stock": 8
}
```

### Shopping Cart

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cart/{sessionId}` | View cart items |
| POST | `/api/cart/{sessionId}/items` | Add item to cart |
| POST | `/api/cart/{sessionId}/checkout` | Checkout cart → create order |

**Add to cart:**

```json
{
  "bookId": 1,
  "quantity": 2
}
```

### Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/orders/{sessionId}` | List orders for a session |

## Seeded Data

The API starts with 3 books pre-loaded:

| ID | Title | Author | Price | Stock |
|----|-------|--------|-------|-------|
| 1 | Clean Code | Robert C. Martin | 499 | 20 |
| 2 | Effective Java | Joshua Bloch | 599 | 15 |
| 3 | Designing Data-Intensive Applications | Martin Kleppmann | 799 | 10 |

## Project Structure

```
├── src/main/java/com/roadmap/bookstore/
│   ├── BookstoreApplication.java   # HTTP server + routing
│   ├── entity/
│   │   ├── Book.java
│   │   ├── CartItem.java
│   │   ├── CustomerOrder.java
│   │   └── OrderLine.java
│   ├── service/
│   │   └── BookstoreService.java   # Business logic (thread-safe)
│   └── util/
│       └── JsonUtil.java           # Lightweight JSON parser
├── run.ps1
└── README.md
```

## Key Concepts

- **Pure Java HTTP server** — no framework overhead
- **Thread safety** — all service methods are `synchronized`
- **Manual JSON parsing** — handles nested objects, escaping, edge cases
- **Clean architecture** — entity / service / routing separation
- **Checkout workflow** — stock validation → stock deduction → order creation

## License

MIT