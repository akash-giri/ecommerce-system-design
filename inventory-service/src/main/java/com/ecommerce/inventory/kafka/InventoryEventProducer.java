package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.dto.StockUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStockUpdate(StockUpdateEvent event) {
        kafkaTemplate.send("stock-updated", String.valueOf(event.getOrderId()), event);
        log.info("Published stock-updated event for order {} with status {}", event.getOrderId(), event.getStatus());
    }
}
