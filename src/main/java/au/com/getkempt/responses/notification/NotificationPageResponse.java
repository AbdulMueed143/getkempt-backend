package au.com.getkempt.responses.notification;

import au.com.getkempt.common.pagination.PageInfo;

import java.util.List;

/**
 * {@code GET /notifications}'s envelope extends the standard {@code Page} shape
 * with an {@code unreadCount} sibling field, which the generic {@code CursorPage<T>}
 * (used everywhere else) has no room for — hence this one bespoke wrapper.
 */
public record NotificationPageResponse(List<NotificationResponse> data, PageInfo pageInfo, long unreadCount) {
}
