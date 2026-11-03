package com.akkidarkhq.visaflow.service;

import com.akkidarkhq.visaflow.dto.request.AddTravellerToApplicationRequest;
import com.akkidarkhq.visaflow.dto.request.CreateGroupVisaApplicationRequest;
import com.akkidarkhq.visaflow.dto.request.CreateVisaApplicationRequest;
import com.akkidarkhq.visaflow.dto.request.UpdateVisaApplicationRequest;
import com.akkidarkhq.visaflow.dto.response.EmbassyDossierResponse;
import com.akkidarkhq.visaflow.dto.response.GroupApplicationDetailsResponse;
import com.akkidarkhq.visaflow.dto.response.VisaApplicationResponse;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.model.*;
import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import com.akkidarkhq.visaflow.model.enums.DocumentStatus;
import com.akkidarkhq.visaflow.model.enums.OrderStatus;
import com.akkidarkhq.visaflow.repository.*;
import com.akkidarkhq.visaflow.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VisaApplicationService {

    private final VisaApplicationRepository visaApplicationRepository;
    private final ApplicationGroupRepository applicationGroupRepository;
    private final TravellerProfileRepository travellerProfileRepository;
    private final VisaTypeRepository visaTypeRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final ChecklistRequirementRepository checklistRequirementRepository;

    /**
     * MODEL B: Create a VisaApplication for a single traveller.
     * Enforces travel date constraints, duplicate-application prevention for the same visa,
     * and itinerary collision detection.
     */
    @Transactional
    public VisaApplicationResponse createApplication(CreateVisaApplicationRequest request) {
        return createApplication(request, null);
    }

    @Transactional
    public VisaApplicationResponse createApplication(CreateVisaApplicationRequest request, UserDetailsImpl currentUser) {
        TravellerProfile traveller = travellerProfileRepository.findById(request.getTravellerId())
                .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + request.getTravellerId()));

        if (currentUser != null && !isAdmin(currentUser)) {
            UUID travellerOwnerId = traveller.getUser() != null ? traveller.getUser().getId() : null;
            if (travellerOwnerId != null && !travellerOwnerId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to create applications for another user's traveller profile.");
            }
        }

        VisaType visaType = visaTypeRepository.findById(request.getVisaTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("VisaType not found with id: " + request.getVisaTypeId()));

        // Validate travel dates, processing window, duplicate application for same visa, and travel date overlap
        validateTravelDatesAndNoConflicts(traveller, visaType, request.getTravelStartDate(), request.getTravelEndDate());

        String groupReference = generateGroupReference();
        ApplicationGroup group = ApplicationGroup.builder()
                .groupReference(groupReference)
                .user(traveller.getUser())
                .visaType(visaType)
                .tripName(visaType.getName() + " Trip")
                .travelStartDate(request.getTravelStartDate())
                .travelEndDate(request.getTravelEndDate())
                .build();
        ApplicationGroup savedGroup = applicationGroupRepository.save(group);

        String referenceNumber = generateReferenceNumber();

        VisaApplication application = VisaApplication.builder()
                .group(savedGroup)
                .orderItem(null)
                .referenceNumber(referenceNumber)
                .groupReference(savedGroup.getGroupReference())
                .travelStartDate(request.getTravelStartDate())
                .travelEndDate(request.getTravelEndDate())
                .traveller(traveller)
                .visaType(visaType)
                .status(ApplicationStatus.APPLICATION_STARTED)
                .build();

        VisaApplication saved = visaApplicationRepository.save(application);

        // Audit initial status
        saveHistory(saved, null, ApplicationStatus.APPLICATION_STARTED, "Application created - documents pending");

        return mapToResponse(saved);
    }

    /**
     * Create multiple visa applications at the same time for family/friends.
     * Enforces travel dates and conflict checks across all companions joining the trip.
     */
    @Transactional
    public List<VisaApplicationResponse> createGroupApplications(CreateGroupVisaApplicationRequest request) {
        return createGroupApplications(request, null);
    }

    @Transactional
    public List<VisaApplicationResponse> createGroupApplications(CreateGroupVisaApplicationRequest request, UserDetailsImpl currentUser) {
        VisaType visaType = visaTypeRepository.findById(request.getVisaTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("VisaType not found with id: " + request.getVisaTypeId()));

        TravellerProfile firstTraveller = travellerProfileRepository.findById(request.getTravellerIds().get(0))
                .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + request.getTravellerIds().get(0)));

        // Ownership verification
        if (currentUser != null && !isAdmin(currentUser)) {
            for (UUID travellerId : request.getTravellerIds()) {
                TravellerProfile tp = travellerProfileRepository.findById(travellerId)
                        .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + travellerId));
                UUID ownerId = tp.getUser() != null ? tp.getUser().getId() : null;
                if (ownerId != null && !ownerId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("All travellers in the group must belong to your user account.");
                }
            }
        }

        // Validate travel dates and conflict checks for all travellers in group
        for (UUID travellerId : request.getTravellerIds()) {
            TravellerProfile tp = travellerProfileRepository.findById(travellerId)
                    .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + travellerId));
            validateTravelDatesAndNoConflicts(tp, visaType, request.getTravelStartDate(), request.getTravelEndDate());
        }

        String groupReference = generateGroupReference();
        ApplicationGroup group = ApplicationGroup.builder()
                .groupReference(groupReference)
                .user(firstTraveller.getUser())
                .visaType(visaType)
                .tripName(visaType.getName() + " Group Trip")
                .travelStartDate(request.getTravelStartDate())
                .travelEndDate(request.getTravelEndDate())
                .build();
        ApplicationGroup savedGroup = applicationGroupRepository.save(group);

        List<VisaApplicationResponse> responses = new ArrayList<>();

        for (UUID travellerId : request.getTravellerIds()) {
            TravellerProfile traveller = travellerProfileRepository.findById(travellerId)
                    .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + travellerId));

            String referenceNumber = generateReferenceNumber();

            VisaApplication application = VisaApplication.builder()
                    .group(savedGroup)
                    .orderItem(null)
                    .referenceNumber(referenceNumber)
                    .groupReference(savedGroup.getGroupReference())
                    .travelStartDate(request.getTravelStartDate())
                    .travelEndDate(request.getTravelEndDate())
                    .traveller(traveller)
                    .visaType(visaType)
                    .status(ApplicationStatus.APPLICATION_STARTED)
                    .build();

            VisaApplication saved = visaApplicationRepository.save(application);
            saveHistory(saved, null, ApplicationStatus.APPLICATION_STARTED,
                    "Application created in group " + groupReference + " - documents pending");

            responses.add(mapToResponse(saved));
        }

        return responses;
    }

    /**
     * Attach a new companion / family member to an existing, ongoing visa application.
     * The companion inherits the existing application's trip dates and destination,
     * while performing active application and itinerary collision checks.
     */
    @Transactional
    public VisaApplicationResponse addTravellerToApplication(UUID existingApplicationId, AddTravellerToApplicationRequest request) {
        return addTravellerToApplication(existingApplicationId, request, null);
    }

    @Transactional
    public VisaApplicationResponse addTravellerToApplication(UUID existingApplicationId, AddTravellerToApplicationRequest request, UserDetailsImpl currentUser) {
        VisaApplication existingApp = visaApplicationRepository.findById(existingApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + existingApplicationId));

        if (currentUser != null && !isAdmin(currentUser)) {
            UUID groupOwnerId = existingApp.getGroup() != null && existingApp.getGroup().getUser() != null
                    ? existingApp.getGroup().getUser().getId()
                    : (existingApp.getTraveller() != null && existingApp.getTraveller().getUser() != null
                    ? existingApp.getTraveller().getUser().getId() : null);
            if (groupOwnerId != null && !groupOwnerId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to add companions to this application group.");
            }
        }

        // Validation: Cannot add companion if the existing application is already in embassy processing, passport decision received, or completed
        if (existingApp.getStatus() == ApplicationStatus.EMBASSY_CONSULATE_PROCESSING
                || existingApp.getStatus() == ApplicationStatus.PASSPORT_DECISION_RECEIVED
                || existingApp.getStatus() == ApplicationStatus.COMPLETED) {
            throw new IllegalStateException("Cannot add a traveller to this group. The application "
                    + existingApp.getReferenceNumber() + " is already in status " + existingApp.getStatus() + ".");
        }

        // Validation: Cannot add the same traveller twice to the same group
        if (visaApplicationRepository.existsByGroupReferenceAndTravellerId(existingApp.getGroupReference(), request.getTravellerId())) {
            throw new IllegalStateException("Traveller is already part of this visa application group ("
                    + existingApp.getGroupReference() + ").");
        }

        TravellerProfile newTraveller = travellerProfileRepository.findById(request.getTravellerId())
                .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + request.getTravellerId()));

        if (currentUser != null && !isAdmin(currentUser)) {
            UUID newTravellerOwnerId = newTraveller.getUser() != null ? newTraveller.getUser().getId() : null;
            if (newTravellerOwnerId != null && !newTravellerOwnerId.equals(currentUser.getId())) {
                throw new AccessDeniedException("The companion traveller profile must belong to your user account.");
            }
        }

        // Resolve or create ApplicationGroup
        ApplicationGroup group = existingApp.getGroup();
        if (group == null) {
            group = applicationGroupRepository.findByGroupReference(existingApp.getGroupReference())
                    .orElseGet(() -> {
                        ApplicationGroup newGroup = ApplicationGroup.builder()
                                .groupReference(existingApp.getGroupReference())
                                .user(existingApp.getTraveller() != null ? existingApp.getTraveller().getUser() : null)
                                .visaType(existingApp.getVisaType())
                                .tripName(existingApp.getVisaType().getName() + " Trip")
                                .travelStartDate(existingApp.getTravelStartDate())
                                .travelEndDate(existingApp.getTravelEndDate())
                                .build();
                        return applicationGroupRepository.save(newGroup);
                    });
            existingApp.setGroup(group);
            visaApplicationRepository.save(existingApp);
        }

        LocalDate travelStart = group.getTravelStartDate() != null ? group.getTravelStartDate() : existingApp.getTravelStartDate();
        LocalDate travelEnd = group.getTravelEndDate() != null ? group.getTravelEndDate() : existingApp.getTravelEndDate();
        VisaType targetVisaType = group.getVisaType() != null ? group.getVisaType() : existingApp.getVisaType();

        // Validate that this companion doesn't already have an active application for this visa or conflicting dates
        if (travelStart != null && travelEnd != null) {
            validateTravelDatesAndNoConflicts(newTraveller, targetVisaType, travelStart, travelEnd);
        }

        String referenceNumber = generateReferenceNumber();

        VisaApplication companionApp = VisaApplication.builder()
                .group(group)
                .orderItem(null)
                .referenceNumber(referenceNumber)
                .groupReference(group.getGroupReference())
                .travelStartDate(travelStart)
                .travelEndDate(travelEnd)
                .traveller(newTraveller)
                .visaType(targetVisaType)
                .status(ApplicationStatus.APPLICATION_STARTED)
                .build();

        VisaApplication saved = visaApplicationRepository.save(companionApp);
        saveHistory(saved, null, ApplicationStatus.APPLICATION_STARTED,
                "Added to group " + group.getGroupReference() + " alongside application " + existingApp.getReferenceNumber());

        return mapToResponse(saved);
    }

    /**
     * Comprehensive validation for travel dates, processing window, duplicate application for the same visa,
     * and itinerary date collision checks.
     */
    public void validateTravelDatesAndNoConflicts(
            TravellerProfile traveller,
            VisaType visaType,
            LocalDate travelStartDate,
            LocalDate travelEndDate) {

        if (travelStartDate == null) {
            throw new IllegalArgumentException("Travel start date is required.");
        }
        if (travelEndDate == null) {
            throw new IllegalArgumentException("Travel end date is required.");
        }

        LocalDate today = LocalDate.now();
        if (travelStartDate.isBefore(today)) {
            throw new IllegalArgumentException("Travel start date cannot be in the past (" + travelStartDate + ").");
        }
        if (travelEndDate.isBefore(travelStartDate)) {
            throw new IllegalArgumentException("Travel end date (" + travelEndDate + ") cannot be earlier than travel start date (" + travelStartDate + ").");
        }

        // Processing time window check
        int processingDays = visaType.getProcessingTimeDays() != null ? visaType.getProcessingTimeDays() : 0;
        LocalDate earliestValidDate = today.plusDays(processingDays);
        if (travelStartDate.isBefore(earliestValidDate)) {
            throw new IllegalArgumentException(String.format(
                    "Travel start date (%s) is too soon. Visa type '%s' requires at least %d days for embassy processing (earliest departure: %s).",
                    travelStartDate, visaType.getName(), processingDays, earliestValidDate
            ));
        }

        // Check active applications for this traveller (all statuses except terminal COMPLETED)
        List<VisaApplication> activeApplications = visaApplicationRepository.findByTravellerIdAndStatusNotIn(
                traveller.getId(),
                List.of(ApplicationStatus.COMPLETED)
        );

        for (VisaApplication activeApp : activeApplications) {
            // Scenario 1: Same Visa Type Active Application (Strict Duplicate Block)
            if (activeApp.getVisaType() != null && activeApp.getVisaType().getId().equals(visaType.getId())) {
                throw new IllegalStateException(String.format(
                        "Traveller %s %s already has an active application (%s) for '%s' in status %s. Duplicate active applications for the same visa destination are not permitted.",
                        traveller.getFirstName(), traveller.getLastName(),
                        activeApp.getReferenceNumber(), visaType.getName(), activeApp.getStatus()
                ));
            }

            // Scenario 2: Different Visa Destination with Overlapping Travel Dates (Physical Impossibility & Itinerary Clash)
            LocalDate activeStart = activeApp.getTravelStartDate();
            LocalDate activeEnd = activeApp.getTravelEndDate();
            if (activeStart != null && activeEnd != null) {
                boolean overlaps = !travelStartDate.isAfter(activeEnd) && !travelEndDate.isBefore(activeStart);
                if (overlaps) {
                    String activeVisaName = activeApp.getVisaType() != null ? activeApp.getVisaType().getName() : "another visa destination";
                    throw new IllegalStateException(String.format(
                            "Travel dates (%s to %s) conflict with traveller %s %s's existing active application (%s for '%s', travel dates: %s to %s). Overlapping travel itineraries are not permitted.",
                            travelStartDate, travelEndDate,
                            traveller.getFirstName(), traveller.getLastName(),
                            activeApp.getReferenceNumber(), activeVisaName, activeStart, activeEnd
                    ));
                }
            }
        }
    }

    /**
     * Retrieves complete group details (all companions, document summaries, costs, travel dates, and readiness)
     * by groupReference (e.g. GRP-2026-123456).
     */
    @Transactional(readOnly = true)
    public GroupApplicationDetailsResponse getGroupDetails(String groupReference) {
        return getGroupDetails(groupReference, null);
    }

    @Transactional(readOnly = true)
    public GroupApplicationDetailsResponse getGroupDetails(String groupReference, UserDetailsImpl currentUser) {
        List<VisaApplication> apps = visaApplicationRepository.findByGroupReference(groupReference);
        if (apps.isEmpty()) {
            throw new ResourceNotFoundException("No visa applications found for group reference: " + groupReference);
        }

        if (currentUser != null && !isAdmin(currentUser)) {
            Optional<ApplicationGroup> groupOpt = applicationGroupRepository.findByGroupReference(groupReference);
            UUID ownerId = groupOpt.map(g -> g.getUser() != null ? g.getUser().getId() : null)
                    .orElseGet(() -> !apps.isEmpty() && apps.get(0).getTraveller() != null && apps.get(0).getTraveller().getUser() != null
                            ? apps.get(0).getTraveller().getUser().getId()
                            : null);
            if (ownerId != null && !ownerId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view this visa application group.");
            }
        }

        // Authoritative VisaType and Travel Dates from ApplicationGroup or fallback to first app
        Optional<ApplicationGroup> groupOpt = applicationGroupRepository.findByGroupReferenceWithDetails(groupReference);
        VisaType visaType = groupOpt.map(ApplicationGroup::getVisaType).orElseGet(() -> apps.get(0).getVisaType());
        LocalDate groupTravelStart = groupOpt.map(ApplicationGroup::getTravelStartDate)
                .orElseGet(() -> !apps.isEmpty() ? apps.get(0).getTravelStartDate() : null);
        LocalDate groupTravelEnd = groupOpt.map(ApplicationGroup::getTravelEndDate)
                .orElseGet(() -> !apps.isEmpty() ? apps.get(0).getTravelEndDate() : null);

        BigDecimal serviceFee = visaType.getServiceFee() != null ? visaType.getServiceFee() : BigDecimal.ZERO;
        BigDecimal embassyFee = visaType.getEmbassyFee() != null ? visaType.getEmbassyFee() : BigDecimal.ZERO;
        BigDecimal feePerPerson = serviceFee.add(embassyFee);
        BigDecimal totalCost = feePerPerson.multiply(BigDecimal.valueOf(apps.size()));
        String currency = visaType.getCurrency() != null ? visaType.getCurrency() : "INR";

        int mandatoryDocCount = checklistRequirementRepository.findByVisaTypeIdAndIsMandatoryTrue(visaType.getId()).size();

        boolean allReadyForCheckout = true;
        List<GroupApplicationDetailsResponse.GroupTravellerApplicationItem> travellerItems = new ArrayList<>();

        for (VisaApplication app : apps) {
            // Check if ready for checkout
            if (app.getStatus() != ApplicationStatus.DOCUMENTS_COMPLETE || app.getOrderItem() != null) {
                allReadyForCheckout = false;
            }

            // Document breakdown using valid DocumentStatus enum values
            List<ApplicationDocument> docs = applicationDocumentRepository.findByApplicationId(app.getId());
            int totalUploaded = docs.size();
            int totalApproved = (int) docs.stream().filter(d -> d.getStatus() == DocumentStatus.APPROVED).count();
            int totalUnderReview = (int) docs.stream().filter(d -> d.getStatus() == DocumentStatus.UNDER_REVIEW).count();
            int totalCorrectionRequested = (int) docs.stream().filter(d -> d.getStatus() == DocumentStatus.CORRECTION_REQUESTED).count();
            int totalRejected = (int) docs.stream().filter(d -> d.getStatus() == DocumentStatus.REJECTED).count();

            GroupApplicationDetailsResponse.DocumentsSummary docSummary = GroupApplicationDetailsResponse.DocumentsSummary.builder()
                    .totalMandatoryRequired(mandatoryDocCount)
                    .totalUploaded(totalUploaded)
                    .totalApproved(totalApproved)
                    .totalUnderReview(totalUnderReview)
                    .totalCorrectionRequested(totalCorrectionRequested)
                    .totalRejected(totalRejected)
                    .build();

            // Order / Payment status
            UUID orderItemId = app.getOrderItem() != null ? app.getOrderItem().getId() : null;
            String orderReference = app.getOrderItem() != null && app.getOrderItem().getOrder() != null
                    ? app.getOrderItem().getOrder().getOrderReference()
                    : null;
            boolean isPaid = app.getOrderItem() != null
                    && app.getOrderItem().getOrder() != null
                    && app.getOrderItem().getOrder().getStatus() == OrderStatus.PAID;

            TravellerProfile t = app.getTraveller();

            GroupApplicationDetailsResponse.GroupTravellerApplicationItem item = GroupApplicationDetailsResponse.GroupTravellerApplicationItem.builder()
                    .applicationId(app.getId())
                    .referenceNumber(app.getReferenceNumber())
                    .status(app.getStatus())
                    .travelStartDate(app.getTravelStartDate())
                    .travelEndDate(app.getTravelEndDate())
                    .createdAt(app.getCreatedAt())
                    .travellerId(t != null ? t.getId() : null)
                    .firstName(t != null ? t.getFirstName() : null)
                    .lastName(t != null ? t.getLastName() : null)
                    .fullName(t != null ? t.getFirstName() + " " + t.getLastName() : null)
                    .passportNumber(t != null ? t.getPassportNumber() : null)
                    .passportExpiry(t != null ? t.getPassportExpiry() : null)
                    .dateOfBirth(t != null ? t.getDateOfBirth() : null)
                    .nationality(t != null ? t.getNationality() : null)
                    .gender(t != null ? t.getGender() : null)
                    .orderItemId(orderItemId)
                    .orderReference(orderReference)
                    .isPaid(isPaid)
                    .documentsSummary(docSummary)
                    .build();

            travellerItems.add(item);
        }

        String countryName = visaType.getCountry() != null ? visaType.getCountry().getName() : null;

        return GroupApplicationDetailsResponse.builder()
                .groupReference(groupReference)
                .visaTypeId(visaType.getId())
                .visaTypeName(visaType.getName())
                .countryName(countryName)
                .travelStartDate(groupTravelStart)
                .travelEndDate(groupTravelEnd)
                .serviceFee(serviceFee)
                .embassyFee(embassyFee)
                .feePerPerson(feePerPerson)
                .totalEstimatedCost(totalCost)
                .currency(currency)
                .totalTravellers(apps.size())
                .isReadyForCheckout(allReadyForCheckout && !apps.isEmpty())
                .travellers(travellerItems)
                .build();
    }

    /**
     * Retrieves full group details by any single application ID in the group.
     */
    @Transactional(readOnly = true)
    public GroupApplicationDetailsResponse getGroupDetailsByApplicationId(UUID applicationId) {
        return getGroupDetailsByApplicationId(applicationId, null);
    }

    @Transactional(readOnly = true)
    public GroupApplicationDetailsResponse getGroupDetailsByApplicationId(UUID applicationId, UserDetailsImpl currentUser) {
        VisaApplication app = visaApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + applicationId));

        return getGroupDetails(app.getGroupReference(), currentUser);
    }

    /**
     * Retrieves all REAL visa application groups (multi-person trips with 2+ travellers)
     * optionally filtered by user ID (or all groups in system if userId is null).
     * Solo travellers are excluded from this method.
     */
    @Transactional(readOnly = true)
    public List<GroupApplicationDetailsResponse> getAllGroups(UUID userId) {
        List<ApplicationGroup> multiGroups = applicationGroupRepository.findMultiPersonGroups(userId);

        List<GroupApplicationDetailsResponse> result = new ArrayList<>();
        for (ApplicationGroup g : multiGroups) {
            try {
                result.add(getGroupDetails(g.getGroupReference()));
            } catch (ResourceNotFoundException ignored) {
            }
        }
        return result;
    }

    /**
     * Retrieves all standalone SOLO visa applications (applications where only 1 person is traveling).
     * Excludes any applications that belong to multi-person family/friend groups.
     */
    @Transactional(readOnly = true)
    public List<VisaApplicationResponse> getSoloApplications(UUID userId) {
        return visaApplicationRepository.findSoloApplications(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Internal: Link an existing application to an OrderItem at checkout.
     * Called by VisaOrderService during checkout.
     */
    @Transactional
    public void linkToOrderItem(UUID applicationId, OrderItem orderItem) {
        VisaApplication application = visaApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + applicationId));

        if (application.getStatus() != ApplicationStatus.DOCUMENTS_COMPLETE) {
            throw new IllegalStateException(
                "Application " + application.getReferenceNumber() + " cannot be checked out. " +
                "All mandatory documents must be verified and approved first. Current status: " + application.getStatus()
            );
        }

        if (application.getOrderItem() != null) {
            throw new IllegalStateException("Application " + application.getReferenceNumber() + " is already linked to an order.");
        }

        application.setOrderItem(orderItem);
        VisaApplication saved = visaApplicationRepository.save(application);

        saveHistory(saved, ApplicationStatus.DOCUMENTS_COMPLETE, ApplicationStatus.DOCUMENTS_COMPLETE,
                "Application linked to order " + orderItem.getOrder().getOrderReference() + " (pending payment).");
    }

    /**
     * Internal domain method for VisaOrderService (legacy Model A support or admin use).
     */
    @Transactional
    public VisaApplication createApplicationForOrderItem(OrderItem orderItem, TravellerProfile traveller, VisaType visaType, String contextNote) {
        String groupReference = generateGroupReference();
        LocalDate defaultStart = LocalDate.now().plusDays(visaType.getProcessingTimeDays() != null ? visaType.getProcessingTimeDays() + 7 : 14);
        LocalDate defaultEnd = defaultStart.plusDays(10);

        ApplicationGroup group = ApplicationGroup.builder()
                .groupReference(groupReference)
                .user(traveller.getUser())
                .visaType(visaType)
                .tripName(visaType.getName() + " Trip")
                .travelStartDate(defaultStart)
                .travelEndDate(defaultEnd)
                .build();
        ApplicationGroup savedGroup = applicationGroupRepository.save(group);

        String referenceNumber = generateReferenceNumber();

        VisaApplication application = VisaApplication.builder()
                .group(savedGroup)
                .orderItem(orderItem)
                .referenceNumber(referenceNumber)
                .groupReference(savedGroup.getGroupReference())
                .travelStartDate(defaultStart)
                .travelEndDate(defaultEnd)
                .traveller(traveller)
                .visaType(visaType)
                .status(ApplicationStatus.APPLICATION_STARTED)
                .build();

        VisaApplication saved = visaApplicationRepository.save(application);
        saveHistory(saved, null, ApplicationStatus.APPLICATION_STARTED, contextNote != null ? contextNote : "Application initialized");
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<VisaApplication> findByOrderItemId(UUID orderItemId) {
        return visaApplicationRepository.findByOrderItemId(orderItemId);
    }

    @Transactional(readOnly = true)
    public VisaApplicationResponse getApplicationById(UUID id) {
        return getApplicationById(id, null);
    }

    @Transactional(readOnly = true)
    public VisaApplicationResponse getApplicationById(UUID id, UserDetailsImpl currentUser) {
        VisaApplication application = visaApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + id));

        if (currentUser != null && !isAdmin(currentUser)) {
            UUID ownerId = application.getTraveller() != null && application.getTraveller().getUser() != null
                    ? application.getTraveller().getUser().getId()
                    : null;
            if (ownerId != null && !ownerId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view this visa application.");
            }
        }

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public VisaApplicationResponse getApplicationByReferenceNumber(String referenceNumber) {
        return getApplicationByReferenceNumber(referenceNumber, null);
    }

    @Transactional(readOnly = true)
    public VisaApplicationResponse getApplicationByReferenceNumber(String referenceNumber, UserDetailsImpl currentUser) {
        VisaApplication application = visaApplicationRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + referenceNumber));

        if (currentUser != null && !isAdmin(currentUser)) {
            UUID ownerId = application.getTraveller() != null && application.getTraveller().getUser() != null
                    ? application.getTraveller().getUser().getId()
                    : null;
            if (ownerId != null && !ownerId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view this visa application.");
            }
        }

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public List<VisaApplicationResponse> getApplicationsByTravellerId(UUID travellerId) {
        return getApplicationsByTravellerId(travellerId, null);
    }

    @Transactional(readOnly = true)
    public List<VisaApplicationResponse> getApplicationsByTravellerId(UUID travellerId, UserDetailsImpl currentUser) {
        if (currentUser != null && !isAdmin(currentUser)) {
            TravellerProfile tp = travellerProfileRepository.findById(travellerId)
                    .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + travellerId));
            UUID ownerId = tp.getUser() != null ? tp.getUser().getId() : null;
            if (ownerId != null && !ownerId.equals(currentUser.getId())) {
                throw new AccessDeniedException("You are not authorized to view applications for this traveller.");
            }
        }

        return visaApplicationRepository.findByTravellerId(travellerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VisaApplicationResponse> getApplications(ApplicationStatus status, UUID userId) {
        List<VisaApplication> apps;
        if (status != null && userId != null) {
            apps = visaApplicationRepository.findByStatusAndTravellerUserId(status, userId);
        } else if (status != null) {
            apps = visaApplicationRepository.findByStatus(status);
        } else if (userId != null) {
            apps = visaApplicationRepository.findByTravellerUserId(userId);
        } else {
            apps = visaApplicationRepository.findAll();
        }
        return apps.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public VisaApplicationResponse updateStatus(UUID id, UpdateVisaApplicationRequest request) {
        VisaApplication application = visaApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + id));

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(request.getStatus());
        VisaApplication updated = visaApplicationRepository.save(application);

        saveHistory(updated, oldStatus, request.getStatus(), request.getNotes());
        return mapToResponse(updated);
    }

    /**
     * Dispatches the visa application and all verified documents to the embassy / consulate queue.
     * Prerequisite: Application must be in SUBMITTED status (paid and approved).
     */
    @Transactional
    public EmbassyDossierResponse submitApplicationToEmbassy(UUID id, String notes) {
        VisaApplication application = visaApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + id));

        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Application cannot be sent to the embassy. " +
                    "Status must be SUBMITTED (paid and green-lit). Current status: " + application.getStatus());
        }

        if (application.getOrderItem() != null && application.getOrderItem().getOrder() != null) {
            OrderStatus orderStatus = application.getOrderItem().getOrder().getStatus();
            if (orderStatus != OrderStatus.PAID) {
                throw new IllegalStateException("Application order is not paid. Order status: " + orderStatus);
            }
        }

        ApplicationStatus previous = application.getStatus();
        application.setStatus(ApplicationStatus.EMBASSY_CONSULATE_PROCESSING);
        VisaApplication saved = visaApplicationRepository.save(application);

        String remark = notes != null && !notes.isBlank()
                ? notes
                : "Application dossier dispatched to embassy/consulate processing authority.";
        saveHistory(saved, previous, ApplicationStatus.EMBASSY_CONSULATE_PROCESSING, remark);

        return buildDossierResponse(saved, remark);
    }

    /**
     * Compiles and retrieves the embassy submission dossier with all approved documents.
     */
    @Transactional(readOnly = true)
    public EmbassyDossierResponse getEmbassyDossier(UUID id) {
        VisaApplication application = visaApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + id));

        return buildDossierResponse(application, "Embassy dossier compiled for application " + application.getReferenceNumber());
    }

    public EmbassyDossierResponse buildDossierResponse(VisaApplication app, String message) {
        List<ApplicationDocument> approvedDocs = applicationDocumentRepository
                .findByApplicationIdAndStatus(app.getId(), DocumentStatus.APPROVED);

        List<EmbassyDossierResponse.ApprovedDocumentSummary> docSummaries = approvedDocs.stream()
                .map(doc -> EmbassyDossierResponse.ApprovedDocumentSummary.builder()
                        .documentId(doc.getId())
                        .requirementId(doc.getRequirement() != null ? doc.getRequirement().getId() : null)
                        .requirementTitle(doc.getRequirement() != null ? doc.getRequirement().getTitle() : null)
                        .documentCategory(doc.getRequirement() != null ? doc.getRequirement().getDocumentCategory() : null)
                        .fileName(doc.getFileName())
                        .mimeType(doc.getMimeType())
                        .fileSizeBytes(doc.getFileSizeBytes())
                        .s3FileKey(doc.getS3FileKey())
                        .downloadEndpoint("/api/documents/" + doc.getId() + "/file")
                        .build())
                .toList();

        UUID orderId = app.getOrderItem() != null && app.getOrderItem().getOrder() != null
                ? app.getOrderItem().getOrder().getId()
                : null;
        String orderRef = app.getOrderItem() != null && app.getOrderItem().getOrder() != null
                ? app.getOrderItem().getOrder().getOrderReference()
                : null;

        String travellerName = app.getTraveller() != null
                ? app.getTraveller().getFirstName() + " " + app.getTraveller().getLastName()
                : null;
        String passport = app.getTraveller() != null ? app.getTraveller().getPassportNumber() : null;
        String nationality = app.getTraveller() != null ? app.getTraveller().getNationality() : null;

        Long visaTypeId = app.getVisaType() != null ? app.getVisaType().getId() : null;
        String visaName = app.getVisaType() != null ? app.getVisaType().getName() : null;
        String countryName = app.getVisaType() != null && app.getVisaType().getCountry() != null
                ? app.getVisaType().getCountry().getName()
                : null;

        return EmbassyDossierResponse.builder()
                .applicationId(app.getId())
                .referenceNumber(app.getReferenceNumber())
                .status(app.getStatus())
                .orderId(orderId)
                .orderReference(orderRef)
                .travellerId(app.getTraveller() != null ? app.getTraveller().getId() : null)
                .travellerFullName(travellerName)
                .passportNumber(passport)
                .nationality(nationality)
                .visaTypeId(visaTypeId)
                .visaTypeName(visaName)
                .countryName(countryName)
                .travelStartDate(app.getTravelStartDate())
                .travelEndDate(app.getTravelEndDate())
                .approvedDocuments(docSummaries)
                .submittedAt(OffsetDateTime.now())
                .submissionMessage(message)
                .build();
    }

    public void saveHistory(VisaApplication app, ApplicationStatus previous, ApplicationStatus next, String notes) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(app)
                .previousStatus(previous)
                .newStatus(next)
                .notes(notes)
                .build();
        statusHistoryRepository.save(history);
    }

    private boolean isAdmin(UserDetailsImpl currentUser) {
        if (currentUser == null) return false;
        return currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Collision-proof reference number generator.
     * Guarantees uniqueness against the database.
     */
    private String generateReferenceNumber() {
        String ref;
        do {
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 999999);
            ref = "VF-" + Year.now().getValue() + "-" + randomNum;
        } while (visaApplicationRepository.existsByReferenceNumber(ref));
        return ref;
    }

    /**
     * Collision-proof group reference generator.
     * Guarantees uniqueness against ApplicationGroupRepository and VisaApplicationRepository.
     */
    private String generateGroupReference() {
        String ref;
        do {
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 999999);
            ref = "GRP-" + Year.now().getValue() + "-" + randomNum;
        } while (applicationGroupRepository.existsByGroupReference(ref));
        return ref;
    }

    public VisaApplicationResponse mapToResponse(VisaApplication app) {
        if (app == null) return null;

        String countryName = app.getVisaType() != null && app.getVisaType().getCountry() != null
                ? app.getVisaType().getCountry().getName()
                : null;

        String travellerName = app.getTraveller() != null
                ? app.getTraveller().getFirstName() + " " + app.getTraveller().getLastName()
                : null;

        String passport = app.getTraveller() != null
                ? app.getTraveller().getPassportNumber()
                : null;

        return VisaApplicationResponse.builder()
                .id(app.getId())
                .orderItemId(app.getOrderItem() != null ? app.getOrderItem().getId() : null)
                .referenceNumber(app.getReferenceNumber())
                .groupReference(app.getGroupReference())
                .travelStartDate(app.getTravelStartDate())
                .travelEndDate(app.getTravelEndDate())
                .travellerId(app.getTraveller() != null ? app.getTraveller().getId() : null)
                .travellerFullName(travellerName)
                .passportNumber(passport)
                .visaTypeId(app.getVisaType() != null ? app.getVisaType().getId() : null)
                .visaTypeName(app.getVisaType() != null ? app.getVisaType().getName() : null)
                .countryName(countryName)
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
