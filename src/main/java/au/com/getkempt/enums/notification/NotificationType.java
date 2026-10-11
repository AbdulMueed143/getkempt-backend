package au.com.getkempt.enums.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {

    NEW_BOOKING("new_booking"),
    BOOKING_CANCELLED("booking_cancelled"),
    BOOKING_RESCHEDULED("booking_rescheduled"),
    LOW_SMS_CREDITS("low_sms_credits"),
    PAYMENT_RECEIVED("payment_received"),
    PAYOUT_ARRIVED("payout_arrived"),
    STAFF_INVITATION_ACCEPTED("staff_invitation_accepted"),
    SUBSCRIPTION_RENEWAL("subscription_renewal"),
    SYSTEM("system");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static NotificationType from(String raw) {
        for (NotificationType type : values()) {
            if (type.value.equalsIgnoreCase(raw)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type: " + raw);
    }
}
