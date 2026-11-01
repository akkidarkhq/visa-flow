package com.akkidarkhq.visaflow.dto.response;

import com.akkidarkhq.visaflow.model.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDocumentResponse {
    private UUID id;
    private UUID applicationId;
    private Long requirementId;
    private String requirementTitle;
    private String fileName;
    private Long fileSizeBytes;
    private String mimeType;
    private String s3FileKey;
    private DocumentStatus status;
    private String adminRemark;
    private UUID reviewedById;
    private String reviewedByUsername;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
