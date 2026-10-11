package au.com.getkempt.common.error;

/**
 * Minimal, dependency-free renderer for the error envelope. Used by the security
 * entry-point / access-denied handlers, which run in the servlet layer (before the
 * MVC message converters) and so cannot rely on the JSON converter or an injected
 * mapper. Business code uses {@link ApiErrorResponse} via the controller advice.
 */
public final class ErrorJson {

    private ErrorJson() {
    }

    public static String render(String code, String message, String requestId, String docsUrl) {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');
        sb.append("\"code\":\"").append(escape(code)).append('"');
        sb.append(",\"message\":\"").append(escape(message)).append('"');
        if (requestId != null) {
            sb.append(",\"requestId\":\"").append(escape(requestId)).append('"');
        }
        if (docsUrl != null) {
            sb.append(",\"docsUrl\":\"").append(escape(docsUrl)).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
