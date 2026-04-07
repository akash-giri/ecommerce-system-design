package com.ecommerce.user.service;

import com.ecommerce.user.dto.CreateUserRequest;
import com.ecommerce.user.dto.OrderSummaryResponse;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.UserAlreadyExistsException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.feign.OrderServiceClient;
import com.ecommerce.user.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OrderServiceClient orderServiceClient;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new UserAlreadyExistsException(request.getEmail());
        });

        User user = userRepository.save(User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .address(request.getAddress())
                .build());

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(findUser(userId));
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrdersFallback")
    public List<OrderSummaryResponse> getOrdersForUser(Long userId) {
        findUser(userId);
        return orderServiceClient.getOrdersByUserId(userId);
    }

    @SuppressWarnings("unused")
    public List<OrderSummaryResponse> getOrdersFallback(Long userId, Throwable throwable) {
        log.warn("Falling back while fetching orders for user {} because {}", userId, throwable.getMessage());
        return Collections.emptyList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .address(user.getAddress())
                .build();
    }
}
