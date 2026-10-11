package au.com.getkempt.enums.staff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Lifecycle of a {@code StaffInvitation}. */
public enum InvitationStatus {

    PENDING("pending"),
    ACCEPTED("accepted"),
    EXPIRED("expired"),
    REVOKED("revoked");

    private final String value;

    InvitationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static InvitationStatus from(String raw) {
        for (InvitationStatus s : values()) {
            if (s.value.equalsIgnoreCase(raw)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown invitation status: " + raw);
    }
}
