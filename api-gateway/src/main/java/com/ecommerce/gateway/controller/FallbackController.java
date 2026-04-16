package com.ecommerce.gateway.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping(value = "/inventory-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> inventoryFallback(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        return buildFallback("inventory-service", correlationId);
    }

    @RequestMapping(value = "/order-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> orderFallback(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        return buildFallback("order-service", correlationId);
    }

    @RequestMapping(value = "/user-service", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> userFallback(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        return buildFallback("user-service", correlationId);
    }

    private ResponseEntity<Map<String, Object>> buildFallback(String serviceName, String correlationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("service", serviceName);
        body.put("message", "Temporary fallback response from API gateway (circuit breaker / timeout).");
        body.put("correlationId", correlationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
