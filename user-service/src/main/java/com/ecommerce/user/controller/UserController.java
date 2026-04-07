package com.ecommerce.user.controller;

import com.ecommerce.user.dto.CreateUserRequest;
import com.ecommerce.user.dto.OrderSummaryResponse;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable("userId") Long userId) {
        return userService.getUser(userId);
    }

    @GetMapping("/{userId}/orders")
    public List<OrderSummaryResponse> getOrdersForUser(@PathVariable("userId") Long userId) {
        return userService.getOrdersForUser(userId);
    }
}
