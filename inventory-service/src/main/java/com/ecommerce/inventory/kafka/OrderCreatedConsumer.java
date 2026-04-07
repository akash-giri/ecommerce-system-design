package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.dto.OrderCreatedEvent;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "order-created", groupId = "inventory-service-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order-created event {}", event.getEventId());
        inventoryService.processOrderCreatedEvent(event);
    }
}
