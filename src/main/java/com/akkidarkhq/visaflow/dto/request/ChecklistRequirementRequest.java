package com.akkidarkhq.visaflow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistRequirementRequest {

    @NotNull(message = "visaTypeId is required")
    private Long visaTypeId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "documentCategory is required")
    private String documentCategory; // e.g., "PASSPORT", "PHOTO", "FINANCIAL"

    @Builder.Default
    private Boolean isMandatory = true;

    @Builder.Default
    private String allowedFormats = "PDF,JPG,PNG";

    @Builder.Default
    @Min(value = 1, message = "maxFileSizeMb must be at least 1")
    private Integer maxFileSizeMb = 5;

    @Builder.Default
    private Integer displayOrder = 0;
}
