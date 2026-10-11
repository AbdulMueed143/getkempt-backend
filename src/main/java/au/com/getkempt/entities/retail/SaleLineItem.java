package au.com.getkempt.entities.retail;

import au.com.getkempt.common.money.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One product line on a {@link Sale}. {@code name}/{@code unitPrice} are
 * snapshotted at sale time — same reasoning as {@code Booking.serviceName}:
 * editing a product's price later must never rewrite sale history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleLineItem {

    private String productId;
    private String name;
    private Money unitPrice;
    private int quantity;
    private Money lineTotal;
}
