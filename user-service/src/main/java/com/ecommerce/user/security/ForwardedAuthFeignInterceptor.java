package com.ecommerce.user.security;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates the gateway-forwarded identity headers to downstream services when using Feign.
 */
@Configuration
public class ForwardedAuthFeignInterceptor {

    private static final List<String> FORWARDED_HEADERS = List.of(
            "X-Gateway-Secret",
            "X-Authenticated-User",
            "X-Authenticated-UserId",
            "X-Authenticated-Role",
            "X-Correlation-Id"
    );

    @Bean
    public RequestInterceptor forwardedAuthHeadersInterceptor() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }

            HttpServletRequest request = attrs.getRequest();
            FORWARDED_HEADERS.forEach(headerName -> {
                String value = request.getHeader(headerName);
                if (value != null && !value.isBlank()) {
                    template.header(headerName, value);
                }
            });
        };
    }
}
