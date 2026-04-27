package com.roadmap.bookstore.controller;

import com.roadmap.bookstore.entity.CartItem;
import com.roadmap.bookstore.entity.CustomerOrder;
import com.roadmap.bookstore.service.CartService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/cart/{sessionId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItem addItem(@PathVariable String sessionId, @RequestBody AddCartItemRequest request) {
        return cartService.addItem(sessionId, request.bookId(), request.quantity());
    }

    @GetMapping("/cart/{sessionId}")
    public List<CartItem> getCart(@PathVariable String sessionId) {
        return cartService.getCartItems(sessionId);
    }

    @PostMapping("/cart/{sessionId}/checkout")
    public CustomerOrder checkout(@PathVariable String sessionId) {
        return cartService.checkout(sessionId);
    }

    @GetMapping("/orders/{sessionId}")
    public List<CustomerOrder> getOrders(@PathVariable String sessionId) {
        return cartService.getOrders(sessionId);
    }

    public record AddCartItemRequest(@NotNull Long bookId, @NotNull @Min(1) Integer quantity) {}
}
