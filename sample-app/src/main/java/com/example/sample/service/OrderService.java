package com.example.sample.service;

import com.example.sample.domain.Order;
import com.example.sample.repository.OrderRepository;
import io.github.wooongchan.requestflow.annotation.DeepTrace;
import org.springframework.stereotype.Service;

// @DeepTrace 데모: validateId()는 this.validateId(id)로 호출되는 내부 메서드라
// 이 애노테이션이 없으면 트레이스 트리에 나타나지 않는다.
@DeepTrace
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order findOrder(Long id) {
        validateId(id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: id=" + id));
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다: " + id);
        }
    }
}
