package au.com.getkempt.enums.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UploadPurpose {

    STAFF_AVATAR("staff_avatar"),
    SERVICE_IMAGE("service_image"),
    PACKAGE_IMAGE("package_image"),
    STORE_LOGO("store_logo"),
    CLIENT_ATTACHMENT("client_attachment");

    private final String value;

    UploadPurpose(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static UploadPurpose from(String raw) {
        for (UploadPurpose purpose : values()) {
            if (purpose.value.equalsIgnoreCase(raw)) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("Unknown upload purpose: " + raw);
    }
}
