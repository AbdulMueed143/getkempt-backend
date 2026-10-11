package au.com.getkempt.common.idempotency;

import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/**
 * Enforces idempotent writes ({@code docs/01-conventions.md §10}):
 * <ul>
 *   <li>same key + same body → returns the cached status &amp; body (no re-execution);</li>
 *   <li>same key + different body → 409 Conflict;</li>
 *   <li>new key → executes once and caches the 2xx result for 24h.</li>
 * </ul>
 * Runs as an interceptor (after security) so it has the resolved tenant and can
 * scope keys per shop.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String ATTR = IdempotencyInterceptor.class.getName() + ".context";
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH");

    private final IdempotencyRecordRepository repository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!MUTATING.contains(request.getMethod())) {
            return true;
        }
        String key = request.getHeader(IdempotencyFilter.HEADER);
        if (key == null || key.isBlank()) {
            return true;
        }
        Optional<String> shopId = TenantContextHolder.current().map(c -> c.shopId());
        if (shopId.isEmpty()) {
            return true; // unauthenticated / no tenant — nothing to scope a key to
        }

        CachedBodyHttpServletRequest cached = WebUtils.getNativeRequest(request, CachedBodyHttpServletRequest.class);
        if (cached == null) {
            return true; // body wasn't buffered — cannot safely dedupe
        }

        String requestHash = sha256(cached.getCachedBody());
        String route = request.getMethod() + " " + request.getRequestURI();

        Optional<IdempotencyRecord> existing = repository.findByShopIdAndIdempotencyKey(shopId.get(), key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new ConflictException("Idempotency-Key was reused with a different request body");
            }
            replay(response, record);
            return false;
        }

        request.setAttribute(ATTR, new PendingRecord(shopId.get(), key, route, requestHash));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object attr = request.getAttribute(ATTR);
        if (!(attr instanceof PendingRecord pending) || ex != null) {
            return;
        }
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper == null) {
            return;
        }
        int status = wrapper.getStatus();
        if (status < 200 || status >= 300) {
            return; // only cache successful results
        }

        String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        try {
            repository.insert(IdempotencyRecord.builder()
                    .shopId(pending.shopId())
                    .idempotencyKey(pending.key())
                    .route(pending.route())
                    .requestHash(pending.requestHash())
                    .responseStatus(status)
                    .responseBody(body)
                    .responseContentType(wrapper.getContentType())
                    .createdAt(Instant.now())
                    .build());
        } catch (DuplicateKeyException dup) {
            log.debug("Idempotency record already stored concurrently for key {}", pending.key());
        }
    }

    private void replay(HttpServletResponse response, IdempotencyRecord record) throws Exception {
        response.setStatus(record.getResponseStatus());
        if (record.getResponseContentType() != null) {
            response.setContentType(record.getResponseContentType());
        }
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        }
    }

    private String sha256(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private record PendingRecord(String shopId, String key, String route, String requestHash) {
    }
}
