package au.com.getkempt.responses.dashboard;

import au.com.getkempt.common.money.Money;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardStatsResponse(
        int bookingsToday,
        Double bookingsTrendPercent,
        Money revenueToday,
        Double revenueTrendPercent,
        int newClientsThisWeek,
        double occupancyRate,
        Integer pendingReviewCount,
        Integer smsCreditsRemaining) {
}
