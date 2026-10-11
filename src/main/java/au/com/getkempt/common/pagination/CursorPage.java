package au.com.getkempt.common.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * Standard list envelope returned by every collection endpoint — matches the
 * {@code Page} schema: {@code { "data": [...], "pageInfo": {...} }}.
 */
public record CursorPage<T>(List<T> data, PageInfo pageInfo) {

    /** Map the elements (e.g. entity → DTO) while preserving pagination metadata. */
    public <R> CursorPage<R> map(Function<? super T, ? extends R> mapper) {
        return new CursorPage<>(data.stream().<R>map(mapper).toList(), pageInfo);
    }
}
