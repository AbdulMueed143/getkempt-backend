package au.com.getkempt.common.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Pagination metadata for a list response — matches the {@code PageInfo} schema.
 *
 * @param nextCursor  opaque cursor for the next page (null on the last page)
 * @param hasNextPage whether another page exists
 * @param totalCount  advisory total; null when not cheaply computable
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageInfo(String nextCursor, boolean hasNextPage, Long totalCount) {

    public static PageInfo of(String nextCursor, boolean hasNextPage) {
        return new PageInfo(nextCursor, hasNextPage, null);
    }
}
