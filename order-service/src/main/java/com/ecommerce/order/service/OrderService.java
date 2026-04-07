package com.ecommerce.order.service;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.InventoryAvailabilityResponse;
import com.ecommerce.order.dto.OrderCreatedEvent;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.StockUpdateEvent;
import com.ecommerce.order.dto.UserSummaryResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.exception.InsufficientStockException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.exception.UserNotFoundException;
import com.ecommerce.order.feign.InventoryServiceClient;
import com.ecommerce.order.feign.UserServiceClient;
import com.ecommerce.order.kafka.OrderEventProducer;
import com.ecommerce.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        validateUserExists(request.getUserId());
        ensureProductHasStock(request.getProductId(), request.getQuantity());

        BigDecimal totalAmount = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        Order order = orderRepository.save(Order.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        orderEventProducer.publishOrderCreated(OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .createdAt(Instant.now())
                .build());

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toResponse(findOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void handleStockUpdate(StockUpdateEvent event) {
        Order order = findOrder(event.getOrderId());
        if ("RESERVED".equalsIgnoreCase(event.getStatus())) {
            order.setStatus(OrderStatus.STOCK_CONFIRMED);
        } else if ("REJECTED".equalsIgnoreCase(event.getStatus())) {
            order.setStatus(OrderStatus.STOCK_REJECTED);
        }
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Order {} updated to {}", order.getId(), order.getStatus());
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "userLookupFallback")
    @Retry(name = "userService")
    protected void validateUserExists(Long userId) {
        UserSummaryResponse user = userServiceClient.getUser(userId);
        if (user == null || user.getId() == null) {
            throw new UserNotFoundException(userId);
        }
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventoryService")
    protected void ensureProductHasStock(Long productId, Integer quantity) {
        InventoryAvailabilityResponse response = inventoryServiceClient.getAvailability(productId);
        boolean unavailable = response == null
                || response.getInStock() == null
                || !response.getInStock()
                || response.getAvailableQuantity() < quantity;
        if (unavailable) {
            throw new InsufficientStockException(productId);
        }
    }

    @SuppressWarnings("unused")
    protected void userLookupFallback(Long userId, Throwable throwable) {
        throw new UserNotFoundException(userId);
    }

    @SuppressWarnings("unused")
    protected void inventoryFallback(Long productId, Integer quantity, Throwable throwable) {
        throw new InsufficientStockException(productId);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .build();
    }
}
