package au.com.getkempt.responses.booking;

import au.com.getkempt.common.money.Money;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailableSlotResponse(Instant startAt, Instant endAt, String staffId, String staffName, Money surchargeAmount) {
}
