package com.ecommerce.user.feign;

import com.ecommerce.user.dto.OrderSummaryResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/user/{userId}")
    List<OrderSummaryResponse> getOrdersByUserId(@PathVariable("userId") Long userId);
}
