package au.com.getkempt.common.error;

/**
 * A single field-level validation failure, used by {@code ValidationError} (422).
 *
 * @param field   JSON-pointer-ish path, e.g. {@code services[0].price}
 * @param message human-readable reason
 * @param code    optional machine code, e.g. {@code too_short}, {@code invalid_email}
 */
public record FieldViolation(String field, String message, String code) {

    public static FieldViolation of(String field, String message) {
        return new FieldViolation(field, message, null);
    }
}
