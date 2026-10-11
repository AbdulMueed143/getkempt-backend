package au.com.getkempt.common.pagination;

import au.com.getkempt.common.error.exceptions.BadRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes/decodes the opaque pagination cursor.
 *
 * <p>Per {@code docs/01-conventions.md §7}, cursors are opaque base64 tokens and a
 * tampered cursor must be rejected with 400. We keyset-paginate, so the cursor only
 * needs to carry the boundary document's id; the boundary's sort value is re-read
 * server-side from that document, which keeps the cursor type-agnostic and correct
 * for any sort field.
 */
public final class CursorCodec {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorCodec() {
    }

    public static String encode(String boundaryId) {
        return ENCODER.encodeToString(boundaryId.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String cursor) {
        try {
            String decoded = new String(DECODER.decode(cursor), StandardCharsets.UTF_8);
            if (decoded.isBlank()) {
                throw new IllegalArgumentException("empty cursor");
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid or tampered pagination cursor");
        }
    }
}
