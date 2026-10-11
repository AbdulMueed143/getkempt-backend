package au.com.getkempt.entities;

import au.com.getkempt.common.media.StoredImageRef;
import au.com.getkempt.common.money.Money;
import au.com.getkempt.common.persistence.BaseDocument;
import au.com.getkempt.enums.service.ServiceCategory;
import au.com.getkempt.enums.service.ServiceStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * A bookable service in the shop's catalogue. Price/duration here are the current
 * defaults; bookings snapshot them at creation time so later edits never rewrite
 * history ({@code docs} note on services.patch).
 */
@Document(collection = "services")
@Getter
@Setter
public class Service extends BaseDocument {

    private String name;
    private ServiceCategory category;
    private String description;
    private StoredImageRef image;
    private int durationMinutes;
    private int bufferMinutes;
    private Money price;
    private boolean depositRequired;
    private Money depositAmount;
    private ServiceStatus status;
    private boolean onlineBookingEnabled;

    private List<String> staffIds = new ArrayList<>();

    private int displayOrder;
}
