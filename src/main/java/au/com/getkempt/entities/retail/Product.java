package au.com.getkempt.entities.retail;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.persistence.BaseDocument;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A retail item a shop stocks for sale (shampoo, wax, gift sets, ...) —
 * separate from {@code Service}/{@code Package}, which are things a staff
 * member performs rather than a physical thing a client walks out with.
 *
 * <p>{@code stockQuantity} is only meaningful/maintained when {@code trackInventory}
 * is true — a shop selling made-to-order or unlimited-stock items (e.g. gift
 * cards printed on demand) can disable tracking and {@code SaleService} then
 * skips the stock check/decrement for that product entirely.
 */
@Document(collection = "products")
@Getter
@Setter
public class Product extends BaseDocument {

    private String name;

    @Indexed
    private String sku;

    private String description;
    private String category;

    private Money price;
    private Money cost;

    private boolean trackInventory = true;
    private int stockQuantity;

    /** Below this, {@code SaleService} raises a low-stock bell notification to the owner. */
    private int lowStockThreshold;

    private boolean active = true;
}
