package com.ecommerce.order.kafka;

import com.ecommerce.order.dto.StockUpdateEvent;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockUpdateConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "stock-updated", groupId = "order-service-group")
    public void onStockUpdated(StockUpdateEvent event) {
        log.info("Received stock update event for order {} with status {}", event.getOrderId(), event.getStatus());
        orderService.handleStockUpdate(event);
    }
}
