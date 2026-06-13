package com.tradevault.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.ExportBillStatus;
import com.tradevault.entity.enums.ExportBillType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "export_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private CorporateClient client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lc_id")
    private LetterOfCredit lc;

    @Column(name = "bill_number", unique = true, nullable = false, length = 50)
    private String billNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private ExportBillStatus status = ExportBillStatus.INITIATED; // INITIATED, DOCUMENTS_SENT, ACCEPTED, PAID, OVERDUE

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "bill_type", length = 30)
    private ExportBillType billType = ExportBillType.DocumentaryCollection;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "drawer_name", nullable = false, length = 150)
    private String drawerName;

    @Column(name = "drawee_name", nullable = false, length = 150)
    private String draweeName;

    @Column(name = "buyer_country", length = 100)
    private String buyerCountry;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "collection_bank", nullable = false, length = 150)
    private String collectionBank;

    @Column(name = "tracking_status", length = 100)
    private String trackingStatus = "DOCUMENTS_PREPARED";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();



}
