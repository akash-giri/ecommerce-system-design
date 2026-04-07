package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertInventoryRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    private Integer availableQuantity;
}
