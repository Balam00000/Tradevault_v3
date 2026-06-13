package com.tradevault.repository;

import com.tradevault.entity.TradeReport;
import com.tradevault.entity.enums.ReportScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeReportRepository extends JpaRepository<TradeReport, Long> {
    List<TradeReport> findByScopeOrderByGeneratedDateDesc(ReportScope scope);
}
