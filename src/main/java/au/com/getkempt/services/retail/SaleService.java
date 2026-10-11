package au.com.getkempt.services.retail;

import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.error.exceptions.UnprocessableEntityException;
import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.security.StaffRole;
import au.com.getkempt.common.tenant.TenantContext;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.entities.Client;
import au.com.getkempt.entities.retail.Product;
import au.com.getkempt.entities.retail.Sale;
import au.com.getkempt.entities.retail.SaleLineItem;
import au.com.getkempt.entities.store.StoreSettings;
import au.com.getkempt.enums.notification.NotificationType;
import au.com.getkempt.enums.payment.PaymentChannel;
import au.com.getkempt.enums.retail.SaleStatus;
import au.com.getkempt.integrations.payment.PaymentGateway;
import au.com.getkempt.mappers.RetailMapper;
import au.com.getkempt.repositories.client.ClientRepository;
import au.com.getkempt.repositories.retail.ProductRepository;
import au.com.getkempt.repositories.retail.SaleRepository;
import au.com.getkempt.repositories.store.StoreSettingsRepository;
import au.com.getkempt.request.retail.SaleCreateRequest;
import au.com.getkempt.request.retail.SaleItemRequest;
import au.com.getkempt.request.retail.SaleVoidRequest;
import au.com.getkempt.responses.payment.PaymentIntentResponse;
import au.com.getkempt.responses.retail.SaleCreateResponse;
import au.com.getkempt.responses.retail.SaleResponse;
import au.com.getkempt.services.notification.NotificationService;
import au.com.getkempt.services.payment.PaymentMethodPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Point-of-sale checkout for retail {@link Product} line items — the "basic
 * mechanism" for shops that sell product, not just services.
 *
 * <p>Deliberately thin: no split payments, no partial refunds, no tax/discount
 * line (see {@code Sale} class javadoc). What IS implemented is the actual
 * point users asked for — a shop can sell an online-paid item (via the same
 * {@code PaymentGateway} stub Bookings uses) OR record that payment was taken
 * directly at the shop, and in the latter case the specific method used is
 * validated against that shop's own {@code StoreSettings.inStoreMethods}
 * allow-list via the shared {@link PaymentMethodPolicy} — exactly the same
 * rule {@code BookingService#complete} enforces, so the policy can never drift
 * between the two checkout paths.
 */
@Service
@RequiredArgsConstructor
public class SaleService {

    private static final Set<String> SORTABLE = Set.of("soldAt", "createdAt");

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentMethodPolicy paymentMethodPolicy;
    private final NotificationService notificationService;
    private final RetailMapper mapper;

    public CursorPage<SaleResponse> list(String cursor, Integer limit, String sortParam,
                                         SaleStatus status, String clientId, String staffId) {
        TenantContext ctx = TenantContextHolder.require();
        List<Criteria> ands = new ArrayList<>();
        if (staffId != null) {
            ands.add(Criteria.where("staffId").is(staffId));
        } else if (!ctx.hasPermission(Permission.SALES_READ_ALL) && !ctx.isOwner()) {
            // Same "no read-all -> scoped to self" behaviour as BookingService#list.
            ands.add(Criteria.where("staffId").is(ctx.staffId()));
        }
        if (clientId != null) {
            ands.add(Criteria.where("clientId").is(clientId));
        }
        if (status != null) {
            ands.add(Criteria.where("status").is(status));
        }
        Criteria filter = ands.isEmpty() ? new Criteria() : new Criteria().andOperator(ands.toArray(new Criteria[0]));
        SortRequest sort = SortRequest.parse(sortParam, SORTABLE, SortRequest.desc("soldAt"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return saleRepository.list(filter, sort, page).map(mapper::toResponse);
    }

    public SaleResponse get(String saleId) {
        Sale sale = saleRepository.requireById(saleId);
        requireCanSee(sale);
        return mapper.toResponse(sale);
    }

    /**
     * Transactional: N products' stock decrements and the {@code Sale} insert
     * must commit together, or a losing failure partway through would either
     * oversell stock (sale never recorded but items already decremented) or
     * silently give away product (sale recorded but stock untouched) — the
     * same "several collections must move together" reasoning as
     * {@code BookingService#createBooking}'s slot locks.
     */
    @Transactional("mongoTransactionManager")
    public SaleCreateResponse create(SaleCreateRequest request) {
        TenantContext ctx = requireCanCreate();
        StoreSettings settings = requireSettings();

        List<SaleLineItem> lineItems = new ArrayList<>();
        List<Product> stockChanges = new ArrayList<>();
        Money subtotal = Money.zero();
        for (SaleItemRequest item : request.items()) {
            Product product = productRepository.requireById(item.productId());
            if (!product.isActive()) {
                throw new UnprocessableEntityException("Product '" + product.getName() + "' is not active");
            }
            if (product.isTrackInventory() && product.getStockQuantity() < item.quantity()) {
                throw new ConflictException("Insufficient stock for '" + product.getName() + "'");
            }
            Money lineTotal = Money.ofAud(product.getPrice().amountCents() * (long) item.quantity());
            lineItems.add(SaleLineItem.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(item.quantity())
                    .lineTotal(lineTotal)
                    .build());
            subtotal = subtotal.plus(lineTotal);
            if (product.isTrackInventory()) {
                product.setStockQuantity(product.getStockQuantity() - item.quantity());
                stockChanges.add(product);
            }
        }
        Money total = subtotal; // v1 has no tax/discount line — see class javadoc

        Client client = request.clientId() != null ? clientRepository.findById(request.clientId()).orElse(null) : null;

        Sale sale = new Sale();
        sale.setClientId(request.clientId());
        sale.setClientName(client != null ? (client.getFirstName() + " " + client.getLastName()) : null);
        sale.setStaffId(ctx.staffId());
        sale.setBookingId(request.bookingId());
        sale.setLineItems(lineItems);
        sale.setSubtotal(subtotal);
        sale.setTotal(total);
        sale.setPaymentChannel(request.paymentChannel());
        sale.setSoldAt(Instant.now());

        PaymentIntentResponse paymentIntentResponse = null;
        if (request.paymentChannel() == PaymentChannel.IN_STORE) {
            if (request.inStoreMethod() == null) {
                throw new UnprocessableEntityException("inStoreMethod is required when paymentChannel=in_store");
            }
            paymentMethodPolicy.requireAllowed(settings, request.inStoreMethod());
            sale.setInStoreMethod(request.inStoreMethod());
            sale.setStatus(SaleStatus.PAID);
        } else {
            if (request.inStoreMethod() != null) {
                throw new UnprocessableEntityException("inStoreMethod must be omitted when paymentChannel=online");
            }
            paymentMethodPolicy.requireOnlineCapable(settings);
            PaymentGateway.IntentResult result = paymentGateway.createIntent(new PaymentGateway.CreateIntent(
                    total, "retail_sale", false, Map.of("shopId", TenantContextHolder.requireShopId())));
            sale.setPaymentIntentId(result.intentId());
            sale.setStatus(SaleStatus.PENDING);
            paymentIntentResponse = new PaymentIntentResponse(
                    result.intentId(), null, request.clientId(), total, "automatic", result.status(), result.clientSecret(), Instant.now());
        }

        saleRepository.insert(sale);
        for (Product product : stockChanges) {
            productRepository.save(product);
            raiseLowStockAlertIfNeeded(product);
        }

        return new SaleCreateResponse(mapper.toResponse(sale), paymentIntentResponse);
    }

    /**
     * Transactional for the same reason {@link #create} is: restoring N
     * products' stock and flipping the sale's status must move together.
     */
    @Transactional("mongoTransactionManager")
    public SaleResponse voidSale(String saleId, SaleVoidRequest request) {
        requireManageAccess();
        Sale sale = saleRepository.requireById(saleId);
        if (sale.getStatus() == SaleStatus.VOIDED || sale.getStatus() == SaleStatus.REFUNDED) {
            throw new ConflictException("Sale is already " + sale.getStatus().value());
        }
        for (SaleLineItem item : sale.getLineItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                if (product.isTrackInventory()) {
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);
                }
            });
        }
        if (sale.getPaymentIntentId() != null) {
            paymentGateway.cancel(sale.getPaymentIntentId());
        }
        sale.setStatus(SaleStatus.VOIDED);
        sale.setVoidReason(request != null ? request.reason() : null);
        saleRepository.save(sale);
        return mapper.toResponse(sale);
    }

    private void raiseLowStockAlertIfNeeded(Product product) {
        if (product.getStockQuantity() <= product.getLowStockThreshold()) {
            // Reuses the generic SYSTEM type rather than adding a new NotificationType
            // constant purely for this — keeps the enum from growing one value per
            // future minor alert; promote to a dedicated type if the UI ever needs
            // to filter specifically on low-stock events.
            notificationService.notifyRole(StaffRole.OWNER, NotificationType.SYSTEM,
                    "Low stock", "'" + product.getName() + "' is low on stock (" + product.getStockQuantity() + " left)",
                    "/products/" + product.getId());
        }
    }

    private TenantContext requireCanCreate() {
        TenantContext ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.SALES_CREATE) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to create sales");
        }
        return ctx;
    }

    private void requireCanSee(Sale sale) {
        TenantContext ctx = TenantContextHolder.require();
        if (ctx.hasPermission(Permission.SALES_READ_ALL) || ctx.isOwner()) {
            return;
        }
        if (sale.getStaffId() == null || !sale.getStaffId().equals(ctx.staffId())) {
            throw new ForbiddenException("You can only view your own sales");
        }
    }

    private void requireManageAccess() {
        TenantContext ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.SALES_READ_ALL) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to void sales");
        }
    }

    private StoreSettings requireSettings() {
        String shopId = TenantContextHolder.requireShopId();
        return storeSettingsRepository.findById(shopId).orElseThrow(() -> NotFoundException.of("StoreSettings", shopId));
    }
}
