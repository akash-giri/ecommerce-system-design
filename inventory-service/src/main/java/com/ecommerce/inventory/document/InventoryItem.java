package com.ecommerce.inventory.document;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long productId;

    private String productName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Instant updatedAt;
}
