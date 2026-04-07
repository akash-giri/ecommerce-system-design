package com.ecommerce.user.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OrderSummaryResponse {
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
}
