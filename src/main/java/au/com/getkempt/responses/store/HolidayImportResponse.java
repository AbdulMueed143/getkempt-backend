package au.com.getkempt.responses.store;

import java.util.List;

/** Wire shape of the {@code POST /store/holidays/import} response. */
public record HolidayImportResponse(int imported, int skipped, List<HolidayResponse> holidays) {
}
