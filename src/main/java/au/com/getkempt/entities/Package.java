package au.com.getkempt.entities;

import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.pkg.DiscountType;
import au.com.getkempt.enums.pkg.PackageStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * A bundle of ≥ 2 services with an optional discount. Base price, discounted price
 * and effective duration are NEVER stored — they are derived from the referenced
 * services at read time so they always reflect current service pricing.
 */
@Document(collection = "packages")
@Getter
@Setter
public class Package extends BaseDocument {

    private String name;
    private String description;
    private StoredImageRef image;

    private List<String> serviceIds = new ArrayList<>();

    private Integer customDurationMinutes;
    private boolean discountEnabled;
    private DiscountType discountType;
    private Double discountValue;
    private PackageStatus status;
    private boolean onlineBookingEnabled;

    private List<String> staffIds = new ArrayList<>();
}
