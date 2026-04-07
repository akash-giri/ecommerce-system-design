package com.ecommerce.inventory.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StockUpdateEvent {
    String eventId;
    Long orderId;
    Long productId;
    Integer requestedQuantity;
    String status;
    String reason;
    Instant processedAt;
}
