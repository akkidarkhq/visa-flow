package com.akkidarkhq.visaflow.model;

import com.akkidarkhq.visaflow.security.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * First-class domain entity representing a group travel itinerary / trip.
 * Every trip (whether for 1 solo traveler or a family of 5) has exactly one ApplicationGroup.
 * The groupReference (e.g. GRP-2026-X8K9M2) is guaranteed unique at the database level.
 */
@Entity
@Table(name = "application_groups", indexes = {
    @Index(name = "idx_application_groups_ref", columnList = "group_reference", unique = true),
    @Index(name = "idx_application_groups_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_reference", nullable = false, unique = true, length = 35)
    private String groupReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visa_type_id", nullable = false)
    private VisaType visaType;

    @Column(name = "trip_name", length = 150)
    private String tripName;

    @Column(name = "travel_start_date")
    private LocalDate travelStartDate;

    @Column(name = "travel_end_date")
    private LocalDate travelEndDate;

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VisaApplication> applications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
