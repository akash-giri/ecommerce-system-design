package com.ecommerce.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class KeyResolverConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-Authenticated-UserId");
            String role = exchange.getRequest().getHeaders().getFirst("X-Authenticated-Role");
            if (userId != null && !userId.isBlank()) {
                // Key is per (role,userId). This keeps buckets separate and makes it easy to evolve into role-based policies later.
                String rolePart = (role == null || role.isBlank()) ? "unknown" : role.toLowerCase();
                return Mono.just("role:" + rolePart + ":uid:" + userId);
            }

            return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    if (exchange.getRequest().getRemoteAddress() == null) {
                        return "unknown";
                    }
                    return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                }));
        };
    }
}
