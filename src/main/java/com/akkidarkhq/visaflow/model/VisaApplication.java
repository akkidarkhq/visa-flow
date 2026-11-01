package com.akkidarkhq.visaflow.model;

import com.akkidarkhq.visaflow.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "visa_applications", indexes = {
    @Index(name = "idx_visa_applications_group_ref", columnList = "group_reference"),
    @Index(name = "idx_visa_applications_reference_num", columnList = "reference_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Nullable for Model B: Application is created BEFORE payment.
     * Linked to an OrderItem only AFTER payment is confirmed.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", unique = true)  // removed nullable = false
    private OrderItem orderItem;

    @Column(name = "reference_number", nullable = false, unique = true, length = 35)
    private String referenceNumber;

    /**
     * Shared group identifier across companions traveling together (e.g., GRP-2026-123456).
     * All family members or friends applying together share the same group_reference.
     */
    @Column(name = "group_reference", length = 35)
    private String groupReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traveller_id", nullable = false)
    private TravellerProfile traveller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visa_type_id", nullable = false)
    private VisaType visaType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
