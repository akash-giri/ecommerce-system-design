package com.ecommerce.order.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderResponse {
    Long orderId;
    Long userId;
    Long productId;
    Integer quantity;
    BigDecimal totalAmount;
    String status;
}
