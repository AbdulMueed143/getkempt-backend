package au.com.getkempt.common.id;

import java.util.UUID;

/**
 * Single source of truth for generating prefixed, opaque, server-assigned IDs.
 *
 * <p>Format: {@code <prefix>_<32-hex-uuid>}, e.g. {@code bk_3f1c8a2b…}. Using a
 * UUIDv4 body keeps IDs unguessable and collision-free; the prefix is purely a
 * human/debugging aid.
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String generate(IdPrefix prefix) {
        return prefix.prefix() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
