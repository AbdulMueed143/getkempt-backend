package au.com.getkempt.common.money;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Money value object — the single representation of monetary amounts across the
 * API and persistence layers.
 *
 * <p>Per {@code docs/01-conventions.md §5}: money is ALWAYS integer cents, never
 * floats, and the currency is AUD in v1. This type is immutable and self-validating.
 */
public record Money(long amountCents, String currency) {

    public static final String AUD = "AUD";

    @JsonCreator
    public Money(
            @JsonProperty("amountCents") long amountCents,
            @JsonProperty("currency") String currency) {
        if (amountCents < 0) {
            throw new IllegalArgumentException("Money.amountCents must not be negative");
        }
        this.amountCents = amountCents;
        this.currency = (currency == null || currency.isBlank()) ? AUD : currency;
    }

    public static Money ofAud(long amountCents) {
        return new Money(amountCents, AUD);
    }

    public static Money zero() {
        return ofAud(0);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amountCents + other.amountCents, this.currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amountCents - other.amountCents, this.currency);
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot operate on mixed currencies: %s vs %s".formatted(this.currency, other.currency));
        }
    }
}
