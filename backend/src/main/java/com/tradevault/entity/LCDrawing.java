package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.LCDrawingStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "lc_drawings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LCDrawing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lc_id", nullable = false)
    private LetterOfCredit lc;

    @Column(name = "drawing_ref", unique = true, nullable = false, length = 50)
    private String drawingRef;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private LCDrawingStatus status = LCDrawingStatus.PENDING_REVIEW; // PENDING_REVIEW, DISCREPANT, APPROVED, PAID, REJECTED

    @Column(name = "presentation_date", nullable = false)
    private LocalDate presentationDate;

    @Column(name = "documents_presented", columnDefinition = "TEXT")
    private String documentsPresented;

    @Column(name = "discrepancy_notes", columnDefinition = "TEXT")
    private String discrepancyNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();



}
