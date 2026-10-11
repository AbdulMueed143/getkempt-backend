package au.com.getkempt.common.error.exceptions;

import org.springframework.http.HttpStatus;

/**
 * 404 — resource does not exist OR the caller's shop has no access to it.
 *
 * <p>Deliberately ambiguous: a resource that lives in another tenant returns 404,
 * never 403, so callers cannot probe for the existence of other shops' data.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "not_found", message);
    }

    public static NotFoundException of(String resource, String id) {
        return new NotFoundException("%s with id %s not found".formatted(resource, id));
    }
}
