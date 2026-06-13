package com.tradevault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.ReportScope;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "trade_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TradeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private ReportScope scope; // CLIENT, PRODUCT, BRANCH, PERIOD

    @Column(nullable = false, columnDefinition = "TEXT")
    private String metrics; // JSON representation of generated report metrics

    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate = LocalDateTime.now();


    public TradeReport(ReportScope scope, String metrics) {
        this.scope = scope;
        this.metrics = metrics;
    }


}
