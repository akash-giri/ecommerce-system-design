package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.document.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
}
