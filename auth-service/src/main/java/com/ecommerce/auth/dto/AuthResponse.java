package com.ecommerce.auth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    Long userId;
    String username;
    String role;
    String accessToken;
    long expiresInSeconds;
}
