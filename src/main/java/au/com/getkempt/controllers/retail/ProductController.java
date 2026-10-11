package au.com.getkempt.controllers.retail;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.request.retail.ProductWriteRequest;
import au.com.getkempt.request.retail.StockAdjustRequest;
import au.com.getkempt.responses.retail.ProductResponse;
import au.com.getkempt.services.retail.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/products} — retail catalogue. See {@code ProductService} javadoc. */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public CursorPage<ProductResponse> listProducts(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String q) {
        return productService.list(cursor, limit, sort, category, activeOnly, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductWriteRequest request) {
        return productService.create(request);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable String productId) {
        return productService.get(productId);
    }

    @PatchMapping("/{productId}")
    public ProductResponse updateProduct(
            @PathVariable String productId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody ProductWriteRequest request) {
        return productService.update(productId, ifMatch, request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable String productId) {
        productService.delete(productId);
    }

    @PostMapping("/{productId}/stock-adjustments")
    public ProductResponse adjustStock(@PathVariable String productId, @Valid @RequestBody StockAdjustRequest request) {
        return productService.adjustStock(productId, request);
    }
}
