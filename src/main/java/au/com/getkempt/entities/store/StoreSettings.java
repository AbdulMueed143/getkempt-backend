package au.com.getkempt.entities.store;

import au.com.getkempt.enums.store.BookingWindowUnit;
import au.com.getkempt.enums.store.DepositType;
import au.com.getkempt.enums.store.FeeType;
import au.com.getkempt.enums.store.InStoreMethod;
import au.com.getkempt.enums.store.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduling + payment + policy configuration. One per shop ({@code id == shopId}).
 * Holidays, surcharge rules and cancellation-fee tiers live here as embedded lists so
 * a single read returns the whole policy surface and updates are atomic on one document.
 */
@Document(collection = "store_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreSettings {

    @Id
    private String id;

    // --- scheduling ---
    private int bookingWindowValue;
    private BookingWindowUnit bookingWindowUnit;
    private int slotInterval;

    @Builder.Default
    private List<Holiday> holidays = new ArrayList<>();

    @Builder.Default
    private List<SurchargeRule> surchargeRules = new ArrayList<>();

    // --- payment ---
    private PaymentMode paymentMode;
    private DepositType depositType;
    private Double depositValue;

    @Builder.Default
    private List<InStoreMethod> inStoreMethods = new ArrayList<>();

    private boolean stripeConnected;

    // --- cancellation / no-show policy ---
    private FeeType noShowFeeType;
    private Double noShowFeeValue;
    private boolean autoCharge;
    private String cancellationPolicy;

    @Builder.Default
    private List<CancellationFeeRule> cancellationFeeRules = new ArrayList<>();

    // --- privacy ---
    private String privacyPolicy;
    private Instant privacyPolicyUpdatedAt;

    @Version
    private Long version;

    @LastModifiedDate
    private Instant updatedAt;
}
