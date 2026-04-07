package com.ecommerce.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    private BigDecimal unitPrice;
}
