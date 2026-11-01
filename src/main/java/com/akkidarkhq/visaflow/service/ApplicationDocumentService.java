package com.akkidarkhq.visaflow.service;

import com.akkidarkhq.visaflow.dto.request.ReviewDocumentRequest;
import com.akkidarkhq.visaflow.dto.response.ApplicationChecklistItemResponse;
import com.akkidarkhq.visaflow.dto.response.ApplicationDocumentResponse;
import com.akkidarkhq.visaflow.event.DocumentReviewedEvent;
import com.akkidarkhq.visaflow.event.DocumentUploadedEvent;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.model.ApplicationDocument;
import com.akkidarkhq.visaflow.model.ChecklistRequirement;
import com.akkidarkhq.visaflow.model.VisaApplication;
import com.akkidarkhq.visaflow.model.enums.DocumentStatus;
import com.akkidarkhq.visaflow.repository.ApplicationDocumentRepository;
import com.akkidarkhq.visaflow.repository.VisaApplicationRepository;
import com.akkidarkhq.visaflow.security.repository.UserRepository;
import com.akkidarkhq.visaflow.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApplicationDocumentService {

    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final VisaApplicationRepository visaApplicationRepository;
    private final ChecklistRequirementService checklistRequirementService;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    /**
     * Fetches the checklist for a specific VisaApplication,
     * indicating which requirements are already uploaded and their status.
     */
    @Transactional(readOnly = true)
    public List<ApplicationChecklistItemResponse> getChecklistForApplication(UUID applicationId) {
        VisaApplication application = visaApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + applicationId));

        Long visaTypeId = application.getVisaType().getId();
        List<ChecklistRequirement> requirements = checklistRequirementService.getRequirementsByVisaType(visaTypeId);
        List<ApplicationDocument> uploadedDocs = applicationDocumentRepository.findByApplicationId(applicationId);

        Map<Long, ApplicationDocument> uploadedMap = new HashMap<>();
        for (ApplicationDocument doc : uploadedDocs) {
            uploadedMap.put(doc.getRequirement().getId(), doc);
        }

        List<ApplicationChecklistItemResponse> responses = new ArrayList<>();
        for (ChecklistRequirement req : requirements) {
            ApplicationDocument doc = uploadedMap.get(req.getId());
            responses.add(ApplicationChecklistItemResponse.builder()
                    .requirementId(req.getId())
                    .title(req.getTitle())
                    .description(req.getDescription())
                    .documentCategory(req.getDocumentCategory())
                    .isMandatory(req.getIsMandatory())
                    .allowedFormats(req.getAllowedFormats())
                    .maxFileSizeMb(req.getMaxFileSizeMb())
                    .displayOrder(req.getDisplayOrder())
                    .isUploaded(doc != null)
                    .documentId(doc != null ? doc.getId() : null)
                    .fileName(doc != null ? doc.getFileName() : null)
                    .status(doc != null ? doc.getStatus() : DocumentStatus.PENDING_UPLOAD)
                    .adminRemark(doc != null ? doc.getAdminRemark() : null)
                    .build());
        }

        return responses;
    }

    /**
     * Uploads or re-uploads a document for a specific requirement on an application.
     */
    @Transactional
    public ApplicationDocumentResponse uploadDocument(UUID applicationId, Long requirementId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file");
        }

        VisaApplication application = visaApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + applicationId));

        ChecklistRequirement requirement = checklistRequirementService.getRequirementById(requirementId);

        // Validate file size
        long maxBytes = (long) requirement.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("File size (" + (file.getSize() / (1024 * 1024)) + "MB) exceeds maximum allowed size of " + requirement.getMaxFileSizeMb() + "MB");
        }

        // Store file physically / S3
        String s3Key = fileStorageService.storeFile(file, applicationId, requirementId);

        // Check if an existing document was uploaded for this requirement (for re-upload)
        Optional<ApplicationDocument> existing = applicationDocumentRepository.findByApplicationIdAndRequirementId(applicationId, requirementId);

        ApplicationDocument doc;
        if (existing.isPresent()) {
            doc = existing.get();
            doc.setFileName(file.getOriginalFilename());
            doc.setFileSizeBytes(file.getSize());
            doc.setMimeType(file.getContentType());
            doc.setS3FileKey(s3Key);
            doc.setStatus(DocumentStatus.UNDER_REVIEW);
            doc.setAdminRemark(null);
        } else {
            doc = ApplicationDocument.builder()
                    .application(application)
                    .requirement(requirement)
                    .fileName(file.getOriginalFilename())
                    .fileSizeBytes(file.getSize())
                    .mimeType(file.getContentType())
                    .s3FileKey(s3Key)
                    .status(DocumentStatus.UNDER_REVIEW)
                    .build();
        }

        ApplicationDocument saved = applicationDocumentRepository.save(doc);

        // Publish domain event: DocumentUploadedEvent
        eventPublisher.publishEvent(new DocumentUploadedEvent(this, applicationId, saved.getId(), requirementId));

        return mapToDocumentResponse(saved);
    }

    /**
     * Loads the document file for viewing/downloading with strict Authorization checks:
     * - Admins (ROLE_ADMIN) can view any document.
     * - Clients (ROLE_CLIENT) can only view documents belonging to their own traveler profile / order.
     */
    @Transactional(readOnly = true)
    public FileDownloadPayload getDocumentFile(UUID documentId, UserDetailsImpl currentUser) {
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationDocument not found with id: " + documentId));

        if (currentUser != null) {
            boolean isAdmin = currentUser.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                // Check ownership: does this traveler profile belong to the current authenticated user?
                UUID ownerUserId = doc.getApplication().getTraveller().getUser().getId();
                if (!ownerUserId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not authorized to view this document");
                }
            }
        }

        Resource resource = fileStorageService.loadFileAsResource(doc.getS3FileKey());
        return new FileDownloadPayload(resource, doc.getFileName(), doc.getMimeType());
    }

    /**
     * Admin reviews an uploaded document (Approve, Reject, Request Correction).
     */
    @Transactional
    public ApplicationDocumentResponse reviewDocument(UUID documentId, ReviewDocumentRequest request, UserDetailsImpl currentUser) {
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationDocument not found with id: " + documentId));

        doc.setStatus(request.getStatus());
        doc.setAdminRemark(request.getAdminRemark());
        doc.setReviewedAt(OffsetDateTime.now());

        if (currentUser != null && currentUser.getId() != null) {
            userRepository.findById(currentUser.getId()).ifPresent(doc::setReviewedBy);
        }

        ApplicationDocument saved = applicationDocumentRepository.save(doc);

        // Publish domain event: DocumentReviewedEvent
        eventPublisher.publishEvent(new DocumentReviewedEvent(this, saved.getApplication().getId(), saved.getId(), saved.getStatus()));

        return mapToDocumentResponse(saved);
    }

    @Transactional
    public ApplicationDocumentResponse reviewDocument(UUID documentId, ReviewDocumentRequest request) {
        return reviewDocument(documentId, request, null);
    }

    private ApplicationDocumentResponse mapToDocumentResponse(ApplicationDocument doc) {
        UUID reviewerId = doc.getReviewedBy() != null ? doc.getReviewedBy().getId() : null;
        String reviewerUsername = doc.getReviewedBy() != null ? doc.getReviewedBy().getUsername() : null;

        return ApplicationDocumentResponse.builder()
                .id(doc.getId())
                .applicationId(doc.getApplication() != null ? doc.getApplication().getId() : null)
                .requirementId(doc.getRequirement() != null ? doc.getRequirement().getId() : null)
                .requirementTitle(doc.getRequirement() != null ? doc.getRequirement().getTitle() : null)
                .fileName(doc.getFileName())
                .fileSizeBytes(doc.getFileSizeBytes())
                .mimeType(doc.getMimeType())
                .s3FileKey(doc.getS3FileKey())
                .status(doc.getStatus())
                .adminRemark(doc.getAdminRemark())
                .reviewedById(reviewerId)
                .reviewedByUsername(reviewerUsername)
                .reviewedAt(doc.getReviewedAt())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public record FileDownloadPayload(Resource resource, String fileName, String mimeType) {}
}
