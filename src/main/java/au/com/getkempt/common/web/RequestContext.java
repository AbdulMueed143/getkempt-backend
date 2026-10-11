package au.com.getkempt.common.web;

/**
 * Thread-bound trace id for the in-flight request, used to stamp logs and the
 * {@code requestId} field of every error envelope ({@code docs/01-conventions.md §15}).
 */
public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
