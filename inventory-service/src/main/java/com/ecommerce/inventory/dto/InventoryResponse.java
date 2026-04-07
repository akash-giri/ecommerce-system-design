package com.ecommerce.inventory.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InventoryResponse {
    Long productId;
    String productName;
    Integer availableQuantity;
    Integer reservedQuantity;
}
