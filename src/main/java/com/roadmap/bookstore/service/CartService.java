package com.roadmap.bookstore.service;

import com.roadmap.bookstore.entity.Book;
import com.roadmap.bookstore.entity.CartItem;
import com.roadmap.bookstore.entity.CustomerOrder;
import com.roadmap.bookstore.entity.OrderLine;
import com.roadmap.bookstore.repository.BookRepository;
import com.roadmap.bookstore.repository.CartItemRepository;
import com.roadmap.bookstore.repository.CustomerOrderRepository;
import com.roadmap.bookstore.repository.OrderLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartService {
    private final BookRepository bookRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderLineRepository orderLineRepository;

    public CartService(
            BookRepository bookRepository,
            CartItemRepository cartItemRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderLineRepository orderLineRepository
    ) {
        this.bookRepository = bookRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderLineRepository = orderLineRepository;
    }

    public CartItem addItem(String sessionId, Long bookId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (book.getStock() < quantity) {
            throw new IllegalStateException("Not enough stock available");
        }

        CartItem item = new CartItem();
        item.setSessionId(sessionId);
        item.setBookId(bookId);
        item.setQuantity(quantity);

        return cartItemRepository.save(item);
    }

    public List<CartItem> getCartItems(String sessionId) {
        return cartItemRepository.findBySessionId(sessionId);
    }

    @Transactional
    public CustomerOrder checkout(String sessionId) {
        List<CartItem> items = cartItemRepository.findBySessionId(sessionId);
        if (items.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        double total = 0;

        for (CartItem item : items) {
            Book book = bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Book not found for cart item"));

            if (book.getStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for book id " + book.getId());
            }

            book.setStock(book.getStock() - item.getQuantity());
            bookRepository.save(book);
            total += book.getPrice() * item.getQuantity();
        }

        CustomerOrder order = new CustomerOrder();
        order.setSessionId(sessionId);
        order.setTotalAmount(total);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());
        order = customerOrderRepository.save(order);

        for (CartItem item : items) {
            Book book = bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Book not found for cart item"));

            OrderLine line = new OrderLine();
            line.setOrderId(order.getId());
            line.setBookId(book.getId());
            line.setQuantity(item.getQuantity());
            line.setPriceAtPurchase(book.getPrice());
            orderLineRepository.save(line);
        }

        cartItemRepository.deleteBySessionId(sessionId);
        return order;
    }

    public List<CustomerOrder> getOrders(String sessionId) {
        return customerOrderRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }
}
