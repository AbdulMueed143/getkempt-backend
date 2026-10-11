package au.com.getkempt.responses.booking;

import au.com.getkempt.common.money.Money;
import au.com.getkempt.enums.store.SurchargeType;

public record AppliedSurchargeResponse(String label, SurchargeType type, Double value, Money amount) {
}
