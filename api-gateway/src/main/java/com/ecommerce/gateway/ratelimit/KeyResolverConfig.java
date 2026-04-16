package com.ecommerce.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class KeyResolverConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-Authenticated-UserId"))
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-Forwarded-For")))
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    if (exchange.getRequest().getRemoteAddress() == null) {
                        return "unknown";
                    }
                    return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                }));
    }
}
