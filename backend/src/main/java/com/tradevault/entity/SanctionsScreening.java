package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.ScreeningEntityType;
import com.tradevault.entity.enums.SanctionsScreeningStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "sanctions_screenings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SanctionsScreening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_name", nullable = false, length = 150)
    private String entityName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "entity_type", nullable = false, length = 50)
    private ScreeningEntityType entityType; // APPLICANT, BENEFICIARY, BANK

    @Column(name = "entity_country", length = 100)
    private String entityCountry;

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType; // LC, BG, EXPORT_BILL

    @Column(name = "transaction_id", nullable = false, length = 50)
    private String transactionId;

    @Column(name = "match_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchScore;

    @Column(name = "watchlist_source", nullable = false, length = 100)
    private String watchlistSource; // OFAC, UN_SECURITY_COUNCIL, EU_WATCHLIST

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private SanctionsScreeningStatus status = SanctionsScreeningStatus.UNDER_REVIEW; // CLEARED, FLAGGED, UNDER_REVIEW

    @Column(name = "screened_at", updatable = false)
    private LocalDateTime screenedAt = LocalDateTime.now();

    @Column(name = "compliance_notes", columnDefinition = "TEXT")
    private String complianceNotes;



}
