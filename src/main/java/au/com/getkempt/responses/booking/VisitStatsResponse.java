package au.com.getkempt.responses.booking;

public record VisitStatsResponse(
        long pendingReviewCount,
        long attendedTodayCount,
        long noShowsLast7Days,
        double attendanceRatePercent) {
}
