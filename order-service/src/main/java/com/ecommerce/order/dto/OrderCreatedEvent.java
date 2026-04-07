package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderCreatedEvent {
    String eventId;
    Long orderId;
    Long userId;
    Long productId;
    Integer quantity;
    BigDecimal totalAmount;
    Instant createdAt;
}
