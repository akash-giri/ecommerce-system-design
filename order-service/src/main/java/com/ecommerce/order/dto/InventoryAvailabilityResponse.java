package com.ecommerce.order.dto;

import lombok.Data;

@Data
public class InventoryAvailabilityResponse {
    private Long productId;
    private Integer availableQuantity;
    private Boolean inStock;
}
