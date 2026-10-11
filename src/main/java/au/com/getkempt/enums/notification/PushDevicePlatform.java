package au.com.getkempt.enums.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PushDevicePlatform {

    IOS("ios"),
    ANDROID("android"),
    WEB("web");

    private final String value;

    PushDevicePlatform(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static PushDevicePlatform from(String raw) {
        for (PushDevicePlatform platform : values()) {
            if (platform.value.equalsIgnoreCase(raw)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Unknown push device platform: " + raw);
    }
}
