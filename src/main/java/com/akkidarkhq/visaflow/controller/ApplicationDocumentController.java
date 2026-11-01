package com.akkidarkhq.visaflow.controller;

import com.akkidarkhq.visaflow.dto.ApiResponse;
import com.akkidarkhq.visaflow.dto.request.ChecklistRequirementRequest;
import com.akkidarkhq.visaflow.dto.request.ReviewDocumentRequest;
import com.akkidarkhq.visaflow.dto.response.ApplicationChecklistItemResponse;
import com.akkidarkhq.visaflow.dto.response.ApplicationDocumentResponse;
import com.akkidarkhq.visaflow.model.ChecklistRequirement;
import com.akkidarkhq.visaflow.security.services.UserDetailsImpl;
import com.akkidarkhq.visaflow.service.ApplicationDocumentService;
import com.akkidarkhq.visaflow.service.ChecklistRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing checklist requirements, document uploads, file viewing, and admin verification reviews.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Document Checklist & Verification", description = "Endpoints for requirements, file uploads, file downloads, and admin reviews")
public class ApplicationDocumentController {

    private final ApplicationDocumentService documentService;
    private final ChecklistRequirementService checklistRequirementService;

    /**
     * Admin: Add a checklist requirement for a VisaType.
     *
     * @param request Requirement specifications
     * @return Created ChecklistRequirement
     */
    @PostMapping("/checklist-requirements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add checklist requirement (Admin)",
            description = "Defines a document requirement (e.g., Passport Front Page, Photo, Bank Statement) for a visa type.")
    public ResponseEntity<ApiResponse<ChecklistRequirement>> addRequirement(
            @Valid @RequestBody ChecklistRequirementRequest request) {
        ChecklistRequirement requirement = checklistRequirementService.addRequirement(request);
        return new ResponseEntity<>(
                ApiResponse.success("Checklist requirement added successfully", requirement),
                HttpStatus.CREATED
        );
    }

    /**
     * Admin: Bulk add checklist requirements for a VisaType.
     *
     * @param requests List of requirement specifications
     * @return List of created requirements
     */
    @PostMapping("/checklist-requirements/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk add checklist requirements (Admin)",
            description = "Creates multiple document checklist requirements for a visa type in a single request.")
    public ResponseEntity<ApiResponse<List<ChecklistRequirement>>> addRequirementsBulk(
            @Valid @RequestBody List<ChecklistRequirementRequest> requests) {
        List<ChecklistRequirement> requirements = checklistRequirementService.addRequirementsBulk(requests);
        return new ResponseEntity<>(
                ApiResponse.success("Checklist requirements added successfully in bulk", requirements),
                HttpStatus.CREATED
        );
    }

    /**
     * View the document checklist for an application (shows what is uploaded, under review, approved, or pending).
     *
     * @param applicationId Application UUID
     * @return Checklist requirements with upload status and remarks
     */
    @GetMapping("/visa-applications/{applicationId}/checklist")
    @Operation(summary = "Get document checklist for application",
            description = "Lists all mandatory and optional requirements for an application along with their upload and review status.")
    public ResponseEntity<ApiResponse<List<ApplicationChecklistItemResponse>>> getChecklist(
            @Parameter(description = "Application UUID")
            @PathVariable UUID applicationId) {
        List<ApplicationChecklistItemResponse> checklist = documentService.getChecklistForApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(checklist));
    }

    /**
     * Uploads or re-uploads a document for a specific requirement on an application.
     *
     * @param applicationId Application UUID
     * @param requirementId Checklist requirement ID
     * @param file Binary file payload (PDF, JPG, PNG)
     * @return Uploaded document details with UNDER_REVIEW status
     */
    @PostMapping(value = "/visa-applications/{applicationId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document for requirement",
            description = "Uploads a file for a specific checklist requirement. Sets document status to UNDER_REVIEW and evaluates workflow.")
    public ResponseEntity<ApiResponse<ApplicationDocumentResponse>> uploadDocument(
            @Parameter(description = "Application UUID")
            @PathVariable UUID applicationId,
            @Parameter(description = "Requirement ID")
            @RequestParam("requirementId") Long requirementId,
            @Parameter(description = "Document file")
            @RequestParam("file") MultipartFile file) {

        ApplicationDocumentResponse response = documentService.uploadDocument(applicationId, requirementId, file);
        return new ResponseEntity<>(
                ApiResponse.success("Document uploaded successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * View or download document file.
     *
     * @param documentId Document UUID
     * @param currentUser Authenticated user principal
     * @return File resource stream
     */
    @GetMapping("/documents/{documentId}/file")
    @Operation(summary = "View/download document file",
            description = "Retrieves the file binary stream. Accessible to ADMIN or the application owner.")
    public ResponseEntity<Resource> getDocumentFile(
            @Parameter(description = "Document UUID")
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        ApplicationDocumentService.FileDownloadPayload payload = documentService.getDocumentFile(documentId, currentUser);

        String contentType = payload.mimeType() != null ? payload.mimeType() : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + payload.fileName() + "\"")
                .body(payload.resource());
    }

    /**
     * Admin: Review an uploaded document (Approve, Reject, Correction Requested).
     *
     * @param documentId Document UUID
     * @param request Review status and remark
     * @param currentUser Authenticated Admin user
     * @return Updated document details
     */
    @PatchMapping("/documents/{documentId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Review document (Admin)",
            description = "Approves, rejects, or requests correction on a document. If all mandatory docs are APPROVED, application status advances to DOCUMENTS_COMPLETE.")
    public ResponseEntity<ApiResponse<ApplicationDocumentResponse>> reviewDocument(
            @Parameter(description = "Document UUID")
            @PathVariable UUID documentId,
            @Valid @RequestBody ReviewDocumentRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        ApplicationDocumentResponse response = documentService.reviewDocument(documentId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Document review updated successfully", response));
    }
}
