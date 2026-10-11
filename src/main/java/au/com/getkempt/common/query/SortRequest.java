package au.com.getkempt.common.query;

import au.com.getkempt.common.error.exceptions.BadRequestException;

import java.util.Set;

/**
 * A validated sort instruction parsed from {@code ?sort=field} (asc) or
 * {@code ?sort=-field} (desc), per {@code docs/01-conventions.md §8}.
 *
 * <p>Only documented sort keys are accepted — an undocumented key is a 400, never
 * a silent default — which prevents callers sorting on un-indexed fields.
 *
 * @param field     the document field to sort on
 * @param ascending direction
 */
public record SortRequest(String field, boolean ascending) {

    public static SortRequest asc(String field) {
        return new SortRequest(field, true);
    }

    public static SortRequest desc(String field) {
        return new SortRequest(field, false);
    }

    /**
     * Parse {@code sortParam} against the allow-list, falling back to {@code defaultSort}
     * when the param is absent.
     *
     * @param sortParam     raw value, e.g. {@code -createdAt} (may be null/blank)
     * @param allowedFields documented, sortable field names
     * @param defaultSort   applied when {@code sortParam} is absent
     */
    public static SortRequest parse(String sortParam, Set<String> allowedFields, SortRequest defaultSort) {
        if (sortParam == null || sortParam.isBlank()) {
            return defaultSort;
        }
        boolean ascending = !sortParam.startsWith("-");
        String field = ascending ? sortParam : sortParam.substring(1);
        if (!allowedFields.contains(field)) {
            throw new BadRequestException(
                    "Unsupported sort key '%s'. Allowed: %s".formatted(field, allowedFields));
        }
        return new SortRequest(field, ascending);
    }
}
