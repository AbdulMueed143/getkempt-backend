package au.com.getkempt.responses.retail;

import au.com.getkempt.common.money.Money;

public record SaleLineItemResponse(String productId, String name, Money unitPrice, int quantity, Money lineTotal) {
}
