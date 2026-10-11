package au.com.getkempt.responses.retail;

import au.com.getkempt.common.money.Money;

import java.time.Instant;

public record ProductResponse(
        String id,
        String name,
        String sku,
        String description,
        String category,
        Money price,
        Money cost,
        boolean trackInventory,
        int stockQuantity,
        int lowStockThreshold,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
