package au.com.getkempt.responses.service;

import au.com.getkempt.enums.service.ServiceCategory;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Wire shape of an entry in {@code GET /services/categories}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceCategoryResponse(ServiceCategory value, String label, String color, String bg) {
}
