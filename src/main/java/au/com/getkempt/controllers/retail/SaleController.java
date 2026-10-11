package au.com.getkempt.controllers.retail;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.enums.retail.SaleStatus;
import au.com.getkempt.request.retail.SaleCreateRequest;
import au.com.getkempt.request.retail.SaleVoidRequest;
import au.com.getkempt.responses.retail.SaleCreateResponse;
import au.com.getkempt.responses.retail.SaleResponse;
import au.com.getkempt.services.retail.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/sales} — retail POS checkout. See {@code SaleService} javadoc. */
@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public CursorPage<SaleResponse> listSales(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String staffId) {
        return saleService.list(cursor, limit, sort, status, clientId, staffId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleCreateResponse createSale(@Valid @RequestBody SaleCreateRequest request) {
        return saleService.create(request);
    }

    @GetMapping("/{saleId}")
    public SaleResponse getSale(@PathVariable String saleId) {
        return saleService.get(saleId);
    }

    @PostMapping("/{saleId}/void")
    public SaleResponse voidSale(@PathVariable String saleId, @RequestBody(required = false) SaleVoidRequest request) {
        return saleService.voidSale(saleId, request);
    }
}
