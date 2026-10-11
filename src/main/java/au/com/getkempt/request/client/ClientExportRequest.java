package au.com.getkempt.request.client;

/** Optional body for {@code POST /clients/export}. */
public record ClientExportRequest(Filters filters) {

    public record Filters(Boolean hasUpcoming, String tag) {
    }
}
