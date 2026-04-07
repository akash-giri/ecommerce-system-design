package com.ecommerce.order.feign;

import com.ecommerce.order.dto.InventoryAvailabilityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-service")
public interface InventoryServiceClient {

    @GetMapping("/api/inventory/{productId}/availability")
    InventoryAvailabilityResponse getAvailability(@PathVariable("productId") Long productId);
}
