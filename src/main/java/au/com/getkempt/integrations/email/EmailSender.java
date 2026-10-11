package au.com.getkempt.integrations.email;

/**
 * Port for transactional + marketing email (SendGrid/Postmark in production).
 * Used by auth emails, reminders, and campaigns.
 */
public interface EmailSender {

    record Result(String providerMessageId) {
    }

    Result send(String toEmail, String subject, String htmlBody);
}
