package com.akkidarkhq.visaflow.service;

import com.akkidarkhq.visaflow.event.DocumentReviewedEvent;
import com.akkidarkhq.visaflow.event.DocumentUploadedEvent;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.model.ApplicationDocument;
import com.akkidarkhq.visaflow.model.ChecklistRequirement;
import com.akkidarkhq.visaflow.model.VisaApplication;
import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import com.akkidarkhq.visaflow.model.enums.DocumentStatus;
import com.akkidarkhq.visaflow.repository.ApplicationDocumentRepository;
import com.akkidarkhq.visaflow.repository.VisaApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisaApplicationWorkflowListener {

    private final VisaApplicationRepository visaApplicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ChecklistRequirementService checklistRequirementService;

    @EventListener
    @Transactional
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        evaluateAndAdvanceWorkflow(event.getApplicationId(), "Document uploaded for requirement " + event.getRequirementId());
    }

    @EventListener
    @Transactional
    public void onDocumentReviewed(DocumentReviewedEvent event) {
        evaluateAndAdvanceWorkflow(event.getApplicationId(), "Document reviewed with status " + event.getStatus());
    }

    /**
     * Pure workflow transition logic.
     * Evaluates mandatory requirement coverage and transitions VisaApplication status.
     * Guard: only modifies applications in pre-submission states.
     */
    public void evaluateAndAdvanceWorkflow(UUID applicationId, String reason) {
        VisaApplication application = visaApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + applicationId));

        // State machine guard: Do not alter status if already paid, submitted, or in embassy review
        if (application.getStatus() != ApplicationStatus.APPLICATION_STARTED
                && application.getStatus() != ApplicationStatus.DOCUMENTS_PENDING
                && application.getStatus() != ApplicationStatus.DOCUMENTS_UNDER_REVIEW
                && application.getStatus() != ApplicationStatus.DOCUMENTS_COMPLETE) {
            return;
        }

        Long visaTypeId = application.getVisaType().getId();
        List<ChecklistRequirement> mandatoryReqs = checklistRequirementService.getMandatoryRequirementsByVisaType(visaTypeId);
        List<ApplicationDocument> uploadedDocs = applicationDocumentRepository.findByApplicationId(applicationId);

        Map<Long, ApplicationDocument> docMap = new HashMap<>();
        for (ApplicationDocument d : uploadedDocs) {
            docMap.put(d.getRequirement().getId(), d);
        }

        boolean allMandatoryUploaded = true;
        boolean allMandatoryApproved = true;

        for (ChecklistRequirement req : mandatoryReqs) {
            ApplicationDocument d = docMap.get(req.getId());
            if (d == null) {
                allMandatoryUploaded = false;
                allMandatoryApproved = false;
                break;
            }
            if (d.getStatus() != DocumentStatus.APPROVED) {
                allMandatoryApproved = false;
            }
        }

        ApplicationStatus targetStatus;
        if (allMandatoryApproved && !mandatoryReqs.isEmpty()) {
            targetStatus = ApplicationStatus.DOCUMENTS_COMPLETE;
        } else if (allMandatoryUploaded && !mandatoryReqs.isEmpty()) {
            targetStatus = ApplicationStatus.DOCUMENTS_UNDER_REVIEW;
        } else {
            targetStatus = ApplicationStatus.DOCUMENTS_PENDING;
        }

        if (application.getStatus() != targetStatus) {
            application.setStatus(targetStatus);
            visaApplicationRepository.save(application);
        }
    }
}
