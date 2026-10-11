package au.com.getkempt.entities.store;

import au.com.getkempt.enums.store.SurchargeDay;
import au.com.getkempt.enums.store.SurchargeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** A recurring time/day-based surcharge rule, embedded in {@link StoreSettings}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurchargeRule {

    private String id;
    private String name;
    private boolean active;
    private List<SurchargeDay> days;
    private String timeStart;
    private String timeEnd;
    private SurchargeType surchargeType;
    private Double surchargeValue;
}
