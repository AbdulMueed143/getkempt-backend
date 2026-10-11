package au.com.getkempt.controllers.dashboard;

import au.com.getkempt.responses.booking.BookingResponse;
import au.com.getkempt.responses.dashboard.DashboardAlertResponse;
import au.com.getkempt.responses.dashboard.DashboardChartPointResponse;
import au.com.getkempt.responses.dashboard.DashboardStatsResponse;
import au.com.getkempt.services.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** {@code /api/v1/dashboard} — see {@code paths/dashboard.yaml}. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public DashboardStatsResponse getDashboardStats() {
        return dashboardService.getStats();
    }

    @GetMapping("/chart")
    public List<DashboardChartPointResponse> getDashboardChart(
            @RequestParam String range,
            @RequestParam(required = false) List<String> metrics) {
        return dashboardService.getChart(range, metrics);
    }

    @GetMapping("/upcoming-bookings")
    public List<BookingResponse> getUpcomingBookings(@RequestParam(required = false) Integer limit) {
        return dashboardService.getUpcomingBookings(limit);
    }

    @GetMapping("/alerts")
    public List<DashboardAlertResponse> listDashboardAlerts(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        return dashboardService.listAlerts(unreadOnly);
    }

    @PostMapping("/alerts/{alertId}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissDashboardAlert(@PathVariable String alertId) {
        dashboardService.dismissAlert(alertId);
    }
}
