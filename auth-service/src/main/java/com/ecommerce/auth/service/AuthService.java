package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.entity.AuthUser;
import com.ecommerce.auth.exception.AuthUserAlreadyExistsException;
import com.ecommerce.auth.exception.InvalidCredentialsException;
import com.ecommerce.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final AuthUserRepository authUserRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        authUserRepository.findByUsername(request.getUsername()).ifPresent(existing -> {
            throw new AuthUserAlreadyExistsException("username", request.getUsername());
        });
        authUserRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            throw new AuthUserAlreadyExistsException("email", request.getEmail());
        });

        AuthUser authUser = authUserRepository.save(AuthUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(DEFAULT_ROLE)
                .build());

        return buildAuthResponse(authUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AuthUser authUser = authUserRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), authUser.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(authUser);
    }

    private AuthResponse buildAuthResponse(AuthUser authUser) {
        return AuthResponse.builder()
                .userId(authUser.getId())
                .username(authUser.getUsername())
                .role(authUser.getRole())
                .accessToken(jwtService.generateToken(authUser))
                .expiresInSeconds(jwtService.getExpirationSeconds())
                .build();
    }
}
