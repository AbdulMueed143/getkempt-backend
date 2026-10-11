package au.com.getkempt.common.idempotency;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Cached result of a mutating request, keyed per tenant on the client-supplied
 * {@code Idempotency-Key} ({@code docs/01-conventions.md §10}).
 *
 * <p>A repeated request with the same key returns the cached status + body; a
 * repeated key with a different body hash is a 409. Records self-expire after 24h
 * via a TTL index on {@code createdAt}.
 */
@Document(collection = "idempotency_records")
@CompoundIndex(name = "uniq_shop_key", def = "{'shopId': 1, 'idempotencyKey': 1}", unique = true)
@Getter
@Setter
@Builder
public class IdempotencyRecord {

    @Id
    private String id;

    private String shopId;
    private String idempotencyKey;
    private String route;
    private String requestHash;

    private int responseStatus;
    private String responseBody;
    private String responseContentType;

    @Indexed(expireAfter = "24h")
    private Instant createdAt;
}
