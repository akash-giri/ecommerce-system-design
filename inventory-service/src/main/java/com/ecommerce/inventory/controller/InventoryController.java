package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.InventoryAvailabilityResponse;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.UpsertInventoryRequest;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse createOrUpdate(@Valid @RequestBody UpsertInventoryRequest request) {
        return inventoryService.createOrUpdateInventory(request);
    }

    @GetMapping("/{productId}")
    public InventoryResponse getInventory(@PathVariable("productId") Long productId) {
        return inventoryService.getInventory(productId);
    }

    @GetMapping("/{productId}/availability")
    public InventoryAvailabilityResponse getAvailability(@PathVariable("productId") Long productId) {
        return inventoryService.getAvailability(productId);
    }
}
