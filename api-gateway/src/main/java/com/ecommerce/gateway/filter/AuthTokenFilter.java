package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthTokenFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/auth/",
            "/actuator/"
    );

    private final JwtTokenService jwtTokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(exchange.getRequest().getMethod(), path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtTokenService.parseToken(token);
            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .header("X-Authenticated-User", claims.getSubject())
                    .header("X-Authenticated-UserId", String.valueOf(claims.get("userId")))
                    .header("X-Authenticated-Role", String.valueOf(claims.get("role")))
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected request {} because token validation failed: {}", path, ex.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private boolean isPublic(HttpMethod method, String path) {
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
