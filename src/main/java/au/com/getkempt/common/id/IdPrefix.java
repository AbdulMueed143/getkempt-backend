package au.com.getkempt.common.id;

/**
 * Prefixed, opaque identifier types — per {@code docs/01-conventions.md §6}.
 *
 * <p>IDs look like {@code bk_3f1c…}. The prefix is informational only; clients
 * must not parse it. Stripe-issued IDs ({@code pi_…}, {@code ch_…}) are stored
 * as-is and never re-mapped, so they are intentionally absent here.
 */
public enum IdPrefix {

    USER("usr"),
    SHOP("shp"),
    STAFF("stf"),
    STAFF_INVITATION("sti"),
    CLIENT("cli"),
    CLIENT_NOTE("cln"),
    BOOKING("bk"),
    APPOINTMENT("apt"),
    BOOKING_NOTE("bkn"),
    SERVICE("svc"),
    PACKAGE("pkg"),
    LOYALTY_PROGRAM("lp"),
    LOYALTY_REWARD("lr"),
    CAMPAIGN("cmp"),
    REMINDER_RULE("rmd"),
    REMINDER_QUEUE("rmq"),
    GIFT_CARD("gc"),
    NOTIFICATION("ntf"),
    PUSH_DEVICE("dev"),
    UPLOAD("upl"),
    HOLIDAY("hol"),
    SURCHARGE_RULE("scr"),
    CANCELLATION_RULE("cfr"),
    AVAILABILITY_OVERRIDE("avo"),
    WEEKLY_SCHEDULE("wsc"),
    SESSION("ses"),
    SLOT_LOCK("slk"),
    REQUEST("req"),
    INVOICE("inv"),
    OUTBOX_MESSAGE("obx"),
    PRODUCT("prd"),
    SALE("sal");

    private final String prefix;

    IdPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
