package com.ecommerce.order.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class StockUpdateEvent {
    private String eventId;
    private Long orderId;
    private Long productId;
    private Integer requestedQuantity;
    private String status;
    private String reason;
    private Instant processedAt;
}
