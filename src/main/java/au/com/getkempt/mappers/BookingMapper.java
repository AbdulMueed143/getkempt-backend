package au.com.getkempt.mappers;

import au.com.getkempt.entities.booking.AppliedSurcharge;
import au.com.getkempt.entities.booking.AppointmentRecord;
import au.com.getkempt.entities.booking.Booking;
import au.com.getkempt.responses.booking.AppliedSurchargeResponse;
import au.com.getkempt.responses.booking.AppointmentRecordResponse;
import au.com.getkempt.responses.booking.BookingResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

    public AppointmentRecordResponse toResponse(AppointmentRecord record) {
        return new AppointmentRecordResponse(
                record.getId(),
                record.getBookingId(),
                record.getClientId(),
                record.getClientName(),
                record.getServiceName(),
                record.getDurationMinutes(),
                record.getStaffId(),
                record.getStaffName(),
                record.getScheduledAt(),
                record.getStatus(),
                record.getNote(),
                record.getStatusUpdatedAt(),
                record.getStatusUpdatedByStaffId());
    }

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getClientId(),
                booking.getClientName(),
                booking.getClientPhone(),
                booking.getClientEmail(),
                booking.getStaffId(),
                booking.getStaffName(),
                booking.getServiceId(),
                booking.getPackageId(),
                booking.getServiceName(),
                booking.getDurationMinutes(),
                booking.getPrice(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getStatus(),
                booking.getSource(),
                booking.getNotes(),
                booking.getCancellationReason(),
                booking.getPaymentIntentId(),
                booking.getDepositPaid(),
                booking.getInStoreMethodUsed(),
                toSurchargeResponses(booking.getSurchargesApplied()),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                booking.getVersion());
    }

    private List<AppliedSurchargeResponse> toSurchargeResponses(List<AppliedSurcharge> surcharges) {
        if (surcharges == null) {
            return List.of();
        }
        return surcharges.stream()
                .map(s -> new AppliedSurchargeResponse(s.getLabel(), s.getType(), s.getValue(), s.getAmount()))
                .toList();
    }
}
