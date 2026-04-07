package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.document.InventoryItem;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<InventoryItem, String> {

    Optional<InventoryItem> findByProductId(Long productId);
}
