package com.ecommerce.order.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Demo security: services accept forwarded identity only from the API gateway.
 */
@Component
public class GatewayForwardedAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_GATEWAY_SECRET = "X-Gateway-Secret";
    private static final String HEADER_AUTH_USER_ID = "X-Authenticated-UserId";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gateway.forwarded-auth.secret:dev-gateway-secret}")
    private String expectedSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/")
                || path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String secret = request.getHeader(HEADER_GATEWAY_SECRET);
        if (!StringUtils.hasText(secret) || !expectedSecret.equals(secret)) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Missing or invalid gateway secret");
            return;
        }

        String userId = request.getHeader(HEADER_AUTH_USER_ID);
        if (!StringUtils.hasText(userId)) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Missing forwarded user identity");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", status.value(),
                "error", error,
                "message", message
        ));
    }
}

