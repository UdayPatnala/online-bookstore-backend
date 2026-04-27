package com.roadmap.bookstore.repository;

import com.roadmap.bookstore.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}
