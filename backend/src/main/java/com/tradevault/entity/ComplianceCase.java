package com.tradevault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.ComplianceCaseStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "compliance_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "screening_id", nullable = false)
    private SanctionsScreening screening;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "case_status", length = 30)
    private ComplianceCaseStatus caseStatus = ComplianceCaseStatus.OPEN; // OPEN, UNDER_INVESTIGATION, ESCALATED, RESOLVED_CLEARED, RESOLVED_BLOCKED

    @Column(name = "assigned_officer_id")
    private Long assignedOfficerId;

    @Column(name = "assigned_to", length = 50)
    private String assignedTo;

    @Column(name = "opened_date")
    private LocalDateTime openedDate = LocalDateTime.now();

    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    


}
