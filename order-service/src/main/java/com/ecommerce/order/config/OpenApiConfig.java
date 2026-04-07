package com.ecommerce.order.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Order Service API",
                version = "1.0.0",
                description = "Places orders, emits Kafka events, and updates order state from inventory events"
        )
)
public class OpenApiConfig {
}
