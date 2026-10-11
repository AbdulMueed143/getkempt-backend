package au.com.getkempt.common.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 403 — the caller is in the right tenant but their role/permission denies the op.
 *
 * <p>NOTE: cross-tenant access must NOT use 403 — it returns 404 to prevent tenant
 * enumeration ({@code docs/03-multi-tenancy.md}). 403 is strictly role/permission.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "forbidden", message);
    }
}
