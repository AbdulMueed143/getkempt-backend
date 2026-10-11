package au.com.getkempt.entities.store;

import au.com.getkempt.enums.store.SurchargeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * A holiday / closure, embedded in {@link StoreSettings}. {@code appliesTo} in the
 * contract is a oneOf("all" | staffId[]); internally we normalise that to a boolean
 * plus an explicit staff list and re-assemble the oneOf at the API boundary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {

    private String id;
    private String name;
    private LocalDate date;

    private boolean appliesToAll;
    private List<String> appliesToStaffIds;

    private boolean hasSurcharge;
    private SurchargeType surchargeType;
    private Double surchargeValue;
}
