package au.com.getkempt.common.pagination;

import au.com.getkempt.common.error.exceptions.BadRequestException;

/**
 * Normalised pagination inputs from the {@code ?cursor=&limit=} query params.
 *
 * <p>Enforces the contract limits ({@code docs/01-conventions.md §7}): default 25,
 * max 100; a {@code limit > 100} is a 400, not a silent clamp.
 *
 * @param boundaryId decoded boundary id from the cursor (null = first page)
 * @param limit      validated page size
 */
public record CursorPageRequest(String boundaryId, int limit) {

    public static final int DEFAULT_LIMIT = 25;
    public static final int MAX_LIMIT = 100;

    public static CursorPageRequest of(String cursor, Integer limit) {
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (resolvedLimit < 1) {
            throw new BadRequestException("limit must be >= 1");
        }
        if (resolvedLimit > MAX_LIMIT) {
            throw new BadRequestException("limit must be <= " + MAX_LIMIT);
        }
        String boundaryId = (cursor == null || cursor.isBlank()) ? null : CursorCodec.decode(cursor);
        return new CursorPageRequest(boundaryId, resolvedLimit);
    }

    public boolean hasCursor() {
        return boundaryId != null;
    }
}
