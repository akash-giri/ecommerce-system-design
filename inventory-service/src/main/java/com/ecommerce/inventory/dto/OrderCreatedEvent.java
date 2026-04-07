package com.ecommerce.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
public class OrderCreatedEvent {
    private String eventId;
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Instant createdAt;
}
