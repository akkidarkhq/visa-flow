package com.akkidarkhq.visaflow.service;

import com.akkidarkhq.visaflow.dto.request.ChecklistRequirementRequest;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.model.ChecklistRequirement;
import com.akkidarkhq.visaflow.model.VisaType;
import com.akkidarkhq.visaflow.repository.ChecklistRequirementRepository;
import com.akkidarkhq.visaflow.repository.VisaTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistRequirementService {

    private final ChecklistRequirementRepository checklistRequirementRepository;
    private final VisaTypeRepository visaTypeRepository;

    @Transactional
    public ChecklistRequirement addRequirement(ChecklistRequirementRequest request) {
        VisaType visaType = visaTypeRepository.findById(request.getVisaTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("VisaType not found with id: " + request.getVisaTypeId()));

        ChecklistRequirement requirement = ChecklistRequirement.builder()
                .visaType(visaType)
                .title(request.getTitle())
                .description(request.getDescription())
                .documentCategory(request.getDocumentCategory())
                .isMandatory(request.getIsMandatory() != null ? request.getIsMandatory() : true)
                .allowedFormats(request.getAllowedFormats() != null ? request.getAllowedFormats() : "PDF,JPG,PNG")
                .maxFileSizeMb(request.getMaxFileSizeMb() != null ? request.getMaxFileSizeMb() : 5)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        return checklistRequirementRepository.save(requirement);
    }

    @Transactional
    public List<ChecklistRequirement> addRequirementsBulk(List<ChecklistRequirementRequest> requests) {
        List<ChecklistRequirement> results = new ArrayList<>();
        for (ChecklistRequirementRequest req : requests) {
            results.add(addRequirement(req));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<ChecklistRequirement> getRequirementsByVisaType(Long visaTypeId) {
        return checklistRequirementRepository.findByVisaTypeIdOrderByDisplayOrderAsc(visaTypeId);
    }

    @Transactional(readOnly = true)
    public List<ChecklistRequirement> getMandatoryRequirementsByVisaType(Long visaTypeId) {
        return checklistRequirementRepository.findByVisaTypeIdAndIsMandatoryTrue(visaTypeId);
    }

    @Transactional(readOnly = true)
    public ChecklistRequirement getRequirementById(Long id) {
        return checklistRequirementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistRequirement not found with id: " + id));
    }
}
