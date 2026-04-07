package com.ecommerce.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "User Service API",
                version = "1.0.0",
                description = "Manages users and fetches their orders from the order service"
        )
)
public class OpenApiConfig {
}
