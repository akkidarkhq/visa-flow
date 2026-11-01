package com.akkidarkhq.visaflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutOrderRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    /**
     * Recommended for Model B (Document-First Flow):
     * List of VisaApplication IDs that have all documents approved (DOCUMENTS_COMPLETE).
     */
    private List<UUID> applicationIds;

    /**
     * Optional for legacy checkout (Model A):
     * Creates an order with new blank applications directly.
     */
    private Long visaTypeId;

    private List<UUID> travellerIds;
}
