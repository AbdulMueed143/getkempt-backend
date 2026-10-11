package au.com.getkempt.request.booking;

import au.com.getkempt.enums.booking.BookingStatus;

import java.time.LocalDate;
import java.util.List;

public record BookingExportRequest(LocalDate from, LocalDate to, List<BookingStatus> status) {
}
