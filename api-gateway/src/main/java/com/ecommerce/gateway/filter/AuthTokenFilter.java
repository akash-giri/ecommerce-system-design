package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String HEADER_AUTH_USER = "X-Authenticated-User";
    private static final String HEADER_AUTH_USER_ID = "X-Authenticated-UserId";
    private static final String HEADER_AUTH_ROLE = "X-Authenticated-Role";
    private static final String HEADER_GATEWAY_SECRET = "X-Gateway-Secret";

    private final JwtTokenService jwtTokenService;

    @Value("${gateway.forwarded-auth.secret:dev-gateway-secret}")
    private String gatewaySecret;

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
            if (!isAuthorized(exchange.getRequest().getMethod(), path, String.valueOf(claims.get("role")))) {
                return forbidden(exchange, "Forbidden for role");
            }

            // Prevent clients from spoofing identity headers; only the gateway should set these.
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove(HEADER_AUTH_USER);
                        headers.remove(HEADER_AUTH_USER_ID);
                        headers.remove(HEADER_AUTH_ROLE);
                        headers.remove(HEADER_GATEWAY_SECRET);

                        headers.add(HEADER_AUTH_USER, claims.getSubject());
                        headers.add(HEADER_AUTH_USER_ID, String.valueOf(claims.get("userId")));
                        headers.add(HEADER_AUTH_ROLE, String.valueOf(claims.get("role")));
                        headers.add(HEADER_GATEWAY_SECRET, gatewaySecret);
                    })
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

    private boolean isAuthorized(HttpMethod method, String path, String role) {
        // Demo RBAC rule: only ADMIN can modify inventory.
        if (path.startsWith("/api/inventory") && HttpMethod.POST.equals(method)) {
            return "ADMIN".equalsIgnoreCase(role);
        }
        return true;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
