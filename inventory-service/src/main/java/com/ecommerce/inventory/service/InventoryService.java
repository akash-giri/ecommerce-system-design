package com.ecommerce.inventory.service;

import com.ecommerce.inventory.document.InventoryItem;
import com.ecommerce.inventory.document.ProcessedEvent;
import com.ecommerce.inventory.dto.InventoryAvailabilityResponse;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.OrderCreatedEvent;
import com.ecommerce.inventory.dto.StockUpdateEvent;
import com.ecommerce.inventory.dto.UpsertInventoryRequest;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.inventory.kafka.InventoryEventProducer;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryResponse createOrUpdateInventory(UpsertInventoryRequest request) {
        InventoryItem item = inventoryRepository.findByProductId(request.getProductId())
                .orElse(InventoryItem.builder()
                        .productId(request.getProductId())
                        .reservedQuantity(0)
                        .build());

        item.setProductName(request.getProductName());
        item.setAvailableQuantity(request.getAvailableQuantity());
        item.setUpdatedAt(Instant.now());

        return toResponse(inventoryRepository.save(item));
    }

    public InventoryResponse getInventory(Long productId) {
        return toResponse(findInventory(productId));
    }

    public InventoryAvailabilityResponse getAvailability(Long productId) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElse(InventoryItem.builder()
                        .productId(productId)
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .build());

        return InventoryAvailabilityResponse.builder()
                .productId(productId)
                .availableQuantity(item.getAvailableQuantity())
                .inStock(item.getAvailableQuantity() > 0)
                .build();
    }

    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Skipping duplicate event {}", event.getEventId());
            return;
        }

        InventoryItem item = findInventory(event.getProductId());
        boolean enoughStock = item.getAvailableQuantity() >= event.getQuantity();

        if (enoughStock) {
            item.setAvailableQuantity(item.getAvailableQuantity() - event.getQuantity());
            item.setReservedQuantity(item.getReservedQuantity() + event.getQuantity());
            inventoryRepository.save(item);
            inventoryEventProducer.publishStockUpdate(StockUpdateEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(event.getOrderId())
                    .productId(event.getProductId())
                    .requestedQuantity(event.getQuantity())
                    .status("RESERVED")
                    .reason("Stock reserved successfully")
                    .processedAt(Instant.now())
                    .build());
        } else {
            inventoryEventProducer.publishStockUpdate(StockUpdateEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .orderId(event.getOrderId())
                    .productId(event.getProductId())
                    .requestedQuantity(event.getQuantity())
                    .status("REJECTED")
                    .reason("Not enough stock")
                    .processedAt(Instant.now())
                    .build());
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.getEventId())
                .processedAt(Instant.now())
                .build());
    }

    private InventoryItem findInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    private InventoryResponse toResponse(InventoryItem item) {
        return InventoryResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .availableQuantity(item.getAvailableQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .build();
    }
}
