# Online Bookstore Backend (Java, No Maven)

A backend starter built with pure Java (`HttpServer`) and in-memory storage.

## Tech Stack

- Java 17+
- Built-in HTTP server (`com.sun.net.httpserver.HttpServer`)
- No Maven, no external libraries

## Features

- Book CRUD APIs
- Session-based cart
- Checkout workflow (stock update + order creation)

## Run

```powershell
.\run.ps1
```

Server starts at `http://localhost:8080`

## APIs

- `GET /health`
- `GET /api/books`
- `GET /api/books/{id}`
- `POST /api/books`
- `PUT /api/books/{id}`
- `DELETE /api/books/{id}`
- `POST /api/cart/{sessionId}/items`
- `GET /api/cart/{sessionId}`
- `POST /api/cart/{sessionId}/checkout`
- `GET /api/orders/{sessionId}`

## Example Payloads

Create book:

```json
{
  "title": "Refactoring",
  "author": "Martin Fowler",
  "price": 650,
  "stock": 8
}
```

Add cart item:

```json
{
  "bookId": 1,
  "quantity": 2
}
```