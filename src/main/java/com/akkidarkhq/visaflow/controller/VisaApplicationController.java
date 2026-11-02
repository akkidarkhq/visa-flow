package com.akkidarkhq.visaflow.controller;

import com.akkidarkhq.visaflow.dto.ApiResponse;
import com.akkidarkhq.visaflow.dto.request.AddTravellerToApplicationRequest;
import com.akkidarkhq.visaflow.dto.request.CreateGroupVisaApplicationRequest;
import com.akkidarkhq.visaflow.dto.request.CreateVisaApplicationRequest;
import com.akkidarkhq.visaflow.dto.request.UpdateVisaApplicationRequest;
import com.akkidarkhq.visaflow.dto.response.EmbassyDossierResponse;
import com.akkidarkhq.visaflow.dto.response.GroupApplicationDetailsResponse;
import com.akkidarkhq.visaflow.dto.response.TravellerResponseDto;
import com.akkidarkhq.visaflow.dto.response.VisaApplicationResponse;
import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import com.akkidarkhq.visaflow.security.services.UserDetailsImpl;
import com.akkidarkhq.visaflow.service.TravellerProfileService;
import com.akkidarkhq.visaflow.service.VisaApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing Visa Application lifecycles, group applications, dossiers, and embassy submissions.
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Visa Applications", description = "Endpoints for visa application lifecycle (Single/Group creation, Companion attachment, Group Dashboards, Dossier, Embassy Dispatch)")
public class VisaApplicationController {

    private final VisaApplicationService visaApplicationService;
    private final TravellerProfileService travellerProfileService;

    /**
     * Creates a draft visa application for a single traveller (Model B: Document-First Flow).
     *
     * @param request Traveller and VisaType details
     * @param currentUser Authenticated user
     * @return Created application in APPLICATION_STARTED status with its unique groupReference
     */
    @PostMapping
    @Operation(summary = "Create a single visa application",
            description = "Creates a visa application without an upfront order. Generates a groupReference allowing companions to be attached later.")
    public ResponseEntity<ApiResponse<VisaApplicationResponse>> createApplication(
            @Valid @RequestBody CreateVisaApplicationRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        VisaApplicationResponse response = visaApplicationService.createApplication(request, currentUser);
        return new ResponseEntity<>(
                ApiResponse.success("Visa application created successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Creates draft visa applications for multiple travellers in a group upfront.
     * All applications share the exact same groupReference.
     *
     * @param request VisaType and list of traveller IDs
     * @param currentUser Authenticated user
     * @return List of created applications sharing the groupReference
     */
    @PostMapping("/group")
    @Operation(summary = "Create group visa applications upfront",
            description = "Creates visa applications for multiple travellers simultaneously under a shared groupReference.")
    public ResponseEntity<ApiResponse<List<VisaApplicationResponse>>> createGroupApplications(
            @Valid @RequestBody CreateGroupVisaApplicationRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<VisaApplicationResponse> responses = visaApplicationService.createGroupApplications(request, currentUser);
        return new ResponseEntity<>(
                ApiResponse.success("Group visa applications created successfully", responses),
                HttpStatus.CREATED
        );
    }

    /**
     * Attaches a new companion/family member to an ongoing visa application.
     * The companion inherits the visa destination and groupReference of the existing application.
     *
     * @param id Existing Visa Application UUID
     * @param request New traveller profile UUID
     * @param currentUser Authenticated user
     * @return Created companion application in APPLICATION_STARTED status
     */
    @PostMapping("/{id}/add-traveller")
    @Operation(summary = "Add traveller to an ongoing application group",
            description = "Attaches a new companion/family member to an ongoing application. Automatically inherits the visa type and group reference.")
    public ResponseEntity<ApiResponse<VisaApplicationResponse>> addTravellerToApplication(
            @Parameter(description = "Existing Application UUID")
            @PathVariable UUID id,
            @Valid @RequestBody AddTravellerToApplicationRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        VisaApplicationResponse response = visaApplicationService.addTravellerToApplication(id, request, currentUser);
        return new ResponseEntity<>(
                ApiResponse.success("Companion traveller added to application group successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Retrieves all visa application groups (trips) with all travellers and status details.
     * Clients only see their own groups; Admins can see all or filter by userId.
     *
     * @param userId Optional filter by traveller's user ID (Admin only)
     * @param currentUser Authenticated user
     * @return List of all group application details
     */
    @GetMapping("/groups")
    @Operation(summary = "Get all visa application groups",
            description = "Retrieves all groups/trips with all companion travellers, document progress, and payment status. Clients are restricted to their own groups.")
    public ResponseEntity<ApiResponse<List<GroupApplicationDetailsResponse>>> getAllGroups(
            @Parameter(description = "Optional filter by User ID (Admin only)")
            @RequestParam(required = false) UUID userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        UUID effectiveUserId = resolveEffectiveUserId(userId, currentUser);
        List<GroupApplicationDetailsResponse> responses = visaApplicationService.getAllGroups(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Retrieves all visa application groups for a specific user ID.
     *
     * @param userId User UUID
     * @param currentUser Authenticated user
     * @return List of group application details belonging to the user
     */
    @GetMapping("/groups/user/{userId}")
    @Operation(summary = "Get all multi-person visa application groups for a User",
            description = "Retrieves all groups/trips with 2 or more travellers created by or belonging to the specified user.")
    public ResponseEntity<ApiResponse<List<GroupApplicationDetailsResponse>>> getGroupsByUserId(
            @Parameter(description = "User UUID")
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        UUID effectiveUserId = resolveEffectiveUserId(userId, currentUser);
        List<GroupApplicationDetailsResponse> responses = visaApplicationService.getAllGroups(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Retrieves all standalone SOLO visa applications (applications where only 1 person is traveling).
     * Excludes applications that belong to multi-person groups.
     * Clients only see their own applications; Admins can see all or filter by userId.
     *
     * @param userId Optional filter by User ID (Admin only)
     * @param currentUser Authenticated user
     * @return List of individual solo applications
     */
    @GetMapping("/solo")
    @Operation(summary = "Get all solo / individual visa applications",
            description = "Retrieves standalone solo applications (group size = 1). Excludes multi-person groups. Clients are restricted to their own applications.")
    public ResponseEntity<ApiResponse<List<VisaApplicationResponse>>> getSoloApplications(
            @Parameter(description = "Optional filter by User ID (Admin only)")
            @RequestParam(required = false) UUID userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        UUID effectiveUserId = resolveEffectiveUserId(userId, currentUser);
        List<VisaApplicationResponse> responses = visaApplicationService.getSoloApplications(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Retrieves all standalone SOLO visa applications for a specific user ID.
     *
     * @param userId User UUID
     * @param currentUser Authenticated user
     * @return List of individual solo applications belonging to the user
     */
    @GetMapping("/solo/user/{userId}")
    @Operation(summary = "Get solo / individual visa applications for a User",
            description = "Retrieves all standalone solo applications belonging to the specified user.")
    public ResponseEntity<ApiResponse<List<VisaApplicationResponse>>> getSoloApplicationsByUserId(
            @Parameter(description = "User UUID")
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        UUID effectiveUserId = resolveEffectiveUserId(userId, currentUser);
        List<VisaApplicationResponse> responses = visaApplicationService.getSoloApplications(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Retrieves full group details (all travellers, documents progress, and costs) by groupReference.
     *
     * @param groupReference Group tracking code (e.g., GRP-2026-123456)
     * @param currentUser Authenticated user
     * @return Complete group details response
     */
    @GetMapping("/group/{groupReference}")
    @Operation(summary = "Get full group details by Group Reference",
            description = "Retrieves all travellers, document checklists status, total cost, and checkout readiness for a trip group.")
    public ResponseEntity<ApiResponse<GroupApplicationDetailsResponse>> getGroupDetails(
            @Parameter(description = "Group Reference Code (e.g. GRP-2026-123456)")
            @PathVariable String groupReference,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        GroupApplicationDetailsResponse response = visaApplicationService.getGroupDetails(groupReference, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves full group details using any single application ID in that group.
     *
     * @param id Any Visa Application UUID belonging to the group
     * @param currentUser Authenticated user
     * @return Complete group details response
     */
    @GetMapping("/{id}/group")
    @Operation(summary = "Get full group details by Application ID",
            description = "Convenience endpoint: Fetches complete trip group details using any single application UUID in the group.")
    public ResponseEntity<ApiResponse<GroupApplicationDetailsResponse>> getGroupDetailsByApplicationId(
            @Parameter(description = "Any Application UUID belonging to the group")
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        GroupApplicationDetailsResponse response = visaApplicationService.getGroupDetailsByApplicationId(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Lists visa applications with optional status and user filters.
     *
     * @param status Optional filter by application status
     * @param userId Optional filter by traveller's user ID (Admin only)
     * @param currentUser Authenticated user
     * @return List of matching applications
     */
    @GetMapping
    @Operation(summary = "List visa applications",
            description = "Retrieves applications filtered by status or user ID. Useful for admin queues and user dashboards.")
    public ResponseEntity<ApiResponse<List<VisaApplicationResponse>>> getAllApplications(
            @Parameter(description = "Filter by Application Status")
            @RequestParam(required = false) ApplicationStatus status,
            @Parameter(description = "Filter by User ID (Admin only)")
            @RequestParam(required = false) UUID userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        UUID effectiveUserId = resolveEffectiveUserId(userId, currentUser);
        List<VisaApplicationResponse> responses = visaApplicationService.getApplications(status, effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Retrieves an application by its unique ID.
     *
     * @param id Application UUID
     * @param currentUser Authenticated user
     * @return VisaApplication details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get visa application by ID", description = "Fetches complete details of a specific visa application.")
    public ResponseEntity<ApiResponse<VisaApplicationResponse>> getApplicationById(
            @Parameter(description = "Application UUID")
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        VisaApplicationResponse response = visaApplicationService.getApplicationById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves the traveller profile associated with a specific Visa Application.
     *
     * @param id Application UUID
     * @param currentUser Authenticated user
     * @return Traveller profile details
     */
    @GetMapping("/{id}/traveller")
    @Operation(summary = "Get traveller by visa application ID",
            description = "Retrieves the full traveller personal and passport details for this visa application.")
    public ResponseEntity<ApiResponse<TravellerResponseDto>> getTravellerByApplication(
            @Parameter(description = "Application UUID")
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        // Enforce access verification on the application first
        visaApplicationService.getApplicationById(id, currentUser);
        TravellerResponseDto response = travellerProfileService.getTravellerByApplicationId(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves an application by reference number (e.g., VF-2026-123456).
     *
     * @param referenceNumber Public reference number
     * @param currentUser Authenticated user
     * @return VisaApplication details
     */
    @GetMapping("/reference/{referenceNumber}")
    @Operation(summary = "Get visa application by Reference Number",
            description = "Fetches an application using its formatted tracking reference (e.g., VF-2026-123456).")
    public ResponseEntity<ApiResponse<VisaApplicationResponse>> getApplicationByReference(
            @Parameter(description = "Application Reference Number")
            @PathVariable String referenceNumber,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        VisaApplicationResponse response = visaApplicationService.getApplicationByReferenceNumber(referenceNumber, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves all applications associated with a specific traveller profile.
     *
     * @param travellerId Traveller UUID
     * @param currentUser Authenticated user
     * @return List of applications
     */
    @GetMapping("/traveller/{travellerId}")
    @Operation(summary = "Get applications for a traveller profile",
            description = "Retrieves all visa applications associated with a specific traveller profile ID.")
    public ResponseEntity<ApiResponse<List<VisaApplicationResponse>>> getApplicationsByTraveller(
            @Parameter(description = "Traveller Profile UUID")
            @PathVariable UUID travellerId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<VisaApplicationResponse> responses = visaApplicationService.getApplicationsByTravellerId(travellerId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Updates an application status and records notes in history.
     *
     * @param id Application UUID
     * @param request New status and administrative notes
     * @return Updated application details
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update visa application status (Admin)",
            description = "Manually updates status and adds an entry to the status history audit trail.")
    public ResponseEntity<ApiResponse<VisaApplicationResponse>> updateStatus(
            @Parameter(description = "Application UUID")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVisaApplicationRequest request) {
        VisaApplicationResponse response = visaApplicationService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", response));
    }

    /**
     * Submits/dispatches the application and verified documents to the embassy / consulate queue.
     *
     * @param id Application UUID
     * @param notes Optional submission notes
     * @return Compiled embassy dossier response
     */
    @PostMapping("/{id}/submit-to-embassy")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dispatch application & verified documents to embassy/consulate",
            description = "Transitions a verified and paid (SUBMITTED) application to EMBASSY_CONSULATE_PROCESSING and compiles the final dossier.")
    public ResponseEntity<ApiResponse<EmbassyDossierResponse>> submitToEmbassy(
            @Parameter(description = "Application UUID")
            @PathVariable UUID id,
            @Parameter(description = "Optional submission remarks")
            @RequestParam(required = false) String notes) {
        EmbassyDossierResponse response = visaApplicationService.submitApplicationToEmbassy(id, notes);
        return ResponseEntity.ok(ApiResponse.success("Visa application successfully dispatched to embassy/consulate", response));
    }

    /**
     * Retrieves the assembled embassy dossier containing verified document download URLs.
     *
     * @param id Application UUID
     * @return Embassy dossier
     */
    @GetMapping("/{id}/dossier")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "View assembled embassy dossier",
            description = "Retrieves the complete applicant dossier including verified document download links for embassy processing.")
    public ResponseEntity<ApiResponse<EmbassyDossierResponse>> getDossier(
            @Parameter(description = "Application UUID")
            @PathVariable UUID id) {
        EmbassyDossierResponse response = visaApplicationService.getEmbassyDossier(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Resolves the effective target userId based on the authenticated principal and request parameters.
     * - If unauthenticated: Throws AccessDeniedException.
     * - If ROLE_ADMIN: Allows querying for any userId (or null for platform-wide queries).
     * - If ROLE_CLIENT: Strictly locks to currentUser.getId(). Rejects any request attempting to query another user's ID.
     */
    private UUID resolveEffectiveUserId(UUID requestedUserId, UserDetailsImpl currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("User must be authenticated to access visa applications");
        }
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return requestedUserId;
        }

        // Regular Client: Always restricted to their own account
        if (requestedUserId != null && !requestedUserId.equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You are only authorized to view your own applications");
        }
        return currentUser.getId();
    }
}
