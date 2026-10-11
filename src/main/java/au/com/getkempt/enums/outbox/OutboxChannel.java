package au.com.getkempt.enums.outbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Which provider port ({@code EmailSender}/{@code SmsSender}/{@code PushSender}) an {@code OutboxMessage} is routed to. */
public enum OutboxChannel {

    EMAIL("email"),
    SMS("sms"),
    PUSH("push");

    private final String value;

    OutboxChannel(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static OutboxChannel from(String raw) {
        for (OutboxChannel channel : values()) {
            if (channel.value.equalsIgnoreCase(raw)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown outbox channel: " + raw);
    }
}
