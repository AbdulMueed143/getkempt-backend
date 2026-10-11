package au.com.getkempt.responses.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardChartPointResponse(
        LocalDate date,
        int bookings,
        long revenueCents,
        Integer loyaltyVisits,
        Integer noShows) {
}
