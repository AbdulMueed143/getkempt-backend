package au.com.getkempt.common.web;

import au.com.getkempt.common.error.exceptions.BadRequestException;
import au.com.getkempt.common.error.exceptions.PreconditionFailedException;

/** Shared {@code If-Match} / {@code @Version} check used by PATCH endpoints. */
public final class IfMatchSupport {

    private IfMatchSupport() {
    }

    public static void check(String ifMatch, Long currentVersion) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return;
        }
        long expected;
        try {
            expected = Long.parseLong(ifMatch.replace("\"", "").trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Malformed If-Match header");
        }
        if (currentVersion != null && expected != currentVersion) {
            throw new PreconditionFailedException(
                    "Resource has changed since version %d; reload and retry".formatted(expected));
        }
    }
}
