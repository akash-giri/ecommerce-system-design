package com.ecommerce.order.kafka;

import com.ecommerce.order.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send("order-created", String.valueOf(event.getOrderId()), event);
        log.info("Published order-created event for order {}", event.getOrderId());
    }
}
