package com.example.sample.repository;

import com.example.sample.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final Map<Long, Order> orders = new HashMap<>();

    public OrderRepository() {
        orders.put(1L, new Order(1L, "keyboard", 2, "secret-token-abc"));
        orders.put(2L, new Order(2L, "mouse", 1, "secret-token-xyz"));
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orders.get(id));
    }
}
