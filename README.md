# Online Bookstore Backend (Spring Boot)

A job-relevant backend starter with CRUD, cart management, and checkout logic.

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA
- H2 (can be switched to PostgreSQL)

## Features

- Book CRUD APIs
- Session-based cart
- Checkout workflow (stock update + order creation)

## Run

```bash
mvn spring-boot:run
```

## APIs

- `GET /api/books`
- `POST /api/books`
- `PUT /api/books/{id}`
- `DELETE /api/books/{id}`
- `POST /api/cart/{sessionId}/items`
- `GET /api/cart/{sessionId}`
- `POST /api/cart/{sessionId}/checkout`
- `GET /api/orders/{sessionId}`
