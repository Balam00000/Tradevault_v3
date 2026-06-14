package com.tradevault.service;

import com.tradevault.entity.TradeReport;
import com.tradevault.entity.enums.ReportScope;
import java.util.List;

public interface TradeReportService {
    List<TradeReport> getAllReports();
    List<TradeReport> getReportsByScope(ReportScope scope);
    TradeReport generateReport(ReportScope scope);
}
