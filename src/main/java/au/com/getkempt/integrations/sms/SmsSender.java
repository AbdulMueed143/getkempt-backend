package au.com.getkempt.integrations.sms;

/**
 * Port for outbound SMS (Twilio in production). Used by reminders + campaigns;
 * each send decrements the shop's SMS credit ledger by {@code segments}.
 */
public interface SmsSender {

    record Result(String providerMessageId, int segments) {
    }

    Result send(String toPhoneE164, String body);
}
