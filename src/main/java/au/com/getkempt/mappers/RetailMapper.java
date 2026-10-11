package au.com.getkempt.mappers;

import au.com.getkempt.entities.retail.Product;
import au.com.getkempt.entities.retail.Sale;
import au.com.getkempt.entities.retail.SaleLineItem;
import au.com.getkempt.responses.retail.ProductResponse;
import au.com.getkempt.responses.retail.SaleLineItemResponse;
import au.com.getkempt.responses.retail.SaleResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RetailMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getCost(),
                product.isTrackInventory(),
                product.getStockQuantity(),
                product.getLowStockThreshold(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getVersion());
    }

    public SaleResponse toResponse(Sale sale) {
        return new SaleResponse(
                sale.getId(),
                sale.getClientId(),
                sale.getClientName(),
                sale.getStaffId(),
                sale.getBookingId(),
                toLineItemResponses(sale.getLineItems()),
                sale.getSubtotal(),
                sale.getTotal(),
                sale.getPaymentChannel(),
                sale.getInStoreMethod(),
                sale.getPaymentIntentId(),
                sale.getStatus(),
                sale.getSoldAt(),
                sale.getVoidReason(),
                sale.getCreatedAt(),
                sale.getVersion());
    }

    private List<SaleLineItemResponse> toLineItemResponses(List<SaleLineItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(i -> new SaleLineItemResponse(i.getProductId(), i.getName(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList();
    }
}
