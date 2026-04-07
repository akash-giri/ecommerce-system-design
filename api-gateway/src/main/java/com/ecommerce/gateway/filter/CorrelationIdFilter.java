package com.ecommerce.gateway.filter;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER_NAME = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(HEADER_NAME, correlationId)
                .build();

        log.info("Gateway request {} {} correlationId={}",
                mutatedRequest.getMethod(),
                mutatedRequest.getURI().getPath(),
                correlationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
