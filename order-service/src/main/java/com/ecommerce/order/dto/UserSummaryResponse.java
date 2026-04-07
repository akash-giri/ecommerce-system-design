package com.ecommerce.order.dto;

import lombok.Data;

@Data
public class UserSummaryResponse {
    private Long id;
    private String name;
    private String email;
    private String address;
}
