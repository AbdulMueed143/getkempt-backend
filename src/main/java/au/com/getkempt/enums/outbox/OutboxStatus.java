package au.com.getkempt.enums.outbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle of one {@code OutboxMessage}.
 *
 * <p>{@code PROCESSING} exists purely so two dispatcher ticks (or, in a
 * multi-instance deployment, two app instances) can never both send the same
 * message — the dispatcher atomically flips {@code PENDING -> PROCESSING} via a
 * guarded update (only one caller's update matches) before doing any I/O, the
 * same "claim before you act" idea as {@code SlotLock} uses for booking slots.
 */
public enum OutboxStatus {

    PENDING("pending"),
    PROCESSING("processing"),
    SENT("sent"),
    FAILED("failed"),
    DEAD("dead");

    private final String value;

    OutboxStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static OutboxStatus from(String raw) {
        for (OutboxStatus status : values()) {
            if (status.value.equalsIgnoreCase(raw)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown outbox status: " + raw);
    }
}
