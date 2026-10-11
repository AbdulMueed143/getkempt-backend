package au.com.getkempt.services.retail;

import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.common.web.IfMatchSupport;
import au.com.getkempt.entities.retail.Product;
import au.com.getkempt.mappers.RetailMapper;
import au.com.getkempt.repositories.retail.ProductRepository;
import au.com.getkempt.request.retail.ProductWriteRequest;
import au.com.getkempt.request.retail.StockAdjustRequest;
import au.com.getkempt.responses.retail.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/** Product catalogue administration — the retail-side sibling of {@code CatalogService}. */
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> SORTABLE = Set.of("name", "createdAt", "stockQuantity");

    private final ProductRepository productRepository;
    private final RetailMapper mapper;

    public CursorPage<ProductResponse> list(String cursor, Integer limit, String sortParam,
                                            String category, Boolean activeOnly, String q) {
        Criteria filter = new Criteria();
        if (category != null && !category.isBlank()) {
            filter.and("category").is(category);
        }
        if (Boolean.TRUE.equals(activeOnly)) {
            filter.and("active").is(true);
        }
        if (q != null && !q.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(q.trim()), Pattern.CASE_INSENSITIVE);
            filter.and("name").regex(pattern);
        }
        SortRequest sort = SortRequest.parse(sortParam, SORTABLE, SortRequest.asc("name"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return productRepository.list(filter, sort, page).map(mapper::toResponse);
    }

    public ProductResponse get(String productId) {
        return mapper.toResponse(productRepository.requireById(productId));
    }

    public ProductResponse create(ProductWriteRequest request) {
        requireManageAccess();
        requireUniqueSku(null, request.sku());
        Product product = new Product();
        applyWrite(product, request, true);
        productRepository.insert(product);
        return mapper.toResponse(product);
    }

    public ProductResponse update(String productId, String ifMatch, ProductWriteRequest request) {
        requireManageAccess();
        Product product = productRepository.requireById(productId);
        IfMatchSupport.check(ifMatch, product.getVersion());
        requireUniqueSku(productId, request.sku());
        applyWrite(product, request, false);
        productRepository.save(product);
        return mapper.toResponse(product);
    }

    public void delete(String productId) {
        requireManageAccess();
        Product product = productRepository.requireById(productId);
        productRepository.softDelete(product);
    }

    /**
     * Restock / shrinkage correction. Deliberately separate from {@link #update}
     * so stock changes always go through one auditable, validated path rather
     * than being smuggled in via a general-purpose PATCH — the same reasoning
     * {@code ClientService}'s dedicated notes endpoints follow.
     */
    public ProductResponse adjustStock(String productId, StockAdjustRequest request) {
        requireManageAccess();
        Product product = productRepository.requireById(productId);
        if (!product.isTrackInventory()) {
            throw new UnprocessableEntityException("This product does not track inventory");
        }
        int newQuantity = product.getStockQuantity() + request.delta();
        if (newQuantity < 0) {
            throw new ConflictException("Stock adjustment would result in a negative quantity");
        }
        product.setStockQuantity(newQuantity);
        productRepository.save(product);
        return mapper.toResponse(product);
    }

    private void applyWrite(Product product, ProductWriteRequest request, boolean creating) {
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.sku() != null) {
            product.setSku(request.sku());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.category() != null) {
            product.setCategory(request.category());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        } else if (creating) {
            product.setPrice(Money.zero());
        }
        if (request.cost() != null) {
            product.setCost(request.cost());
        }
        if (request.trackInventory() != null) {
            product.setTrackInventory(request.trackInventory());
        } else if (creating) {
            product.setTrackInventory(true);
        }
        if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        } else if (creating) {
            product.setStockQuantity(0);
        }
        if (request.lowStockThreshold() != null) {
            product.setLowStockThreshold(request.lowStockThreshold());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        } else if (creating) {
            product.setActive(true);
        }
    }

    private void requireUniqueSku(String excludingProductId, String sku) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        productRepository.findBySku(sku)
                .filter(existing -> !existing.getId().equals(excludingProductId))
                .ifPresent(existing -> {
                    throw new ConflictException("SKU '" + sku + "' is already in use by another product");
                });
    }

    private void requireManageAccess() {
        var ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.PRODUCTS_MANAGE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to manage products");
        }
    }
}
