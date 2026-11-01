package com.akkidarkhq.visaflow.dto.response;

import com.akkidarkhq.visaflow.model.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationChecklistItemResponse {
    private Long requirementId;
    private String title;
    private String description;
    private String documentCategory;
    private Boolean isMandatory;
    private String allowedFormats;
    private Integer maxFileSizeMb;
    private Integer displayOrder;

    // Current upload status for this requirement on the application
    private Boolean isUploaded;
    private UUID documentId;
    private String fileName;
    private DocumentStatus status;
    private String adminRemark;
}
