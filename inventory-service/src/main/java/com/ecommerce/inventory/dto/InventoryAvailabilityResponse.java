package com.ecommerce.inventory.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InventoryAvailabilityResponse {
    Long productId;
    Integer availableQuantity;
    Boolean inStock;
}
