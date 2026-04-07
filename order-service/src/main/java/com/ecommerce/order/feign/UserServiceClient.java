package com.ecommerce.order.feign;

import com.ecommerce.order.dto.UserSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserSummaryResponse getUser(@PathVariable("userId") Long userId);
}
