package com.tradevault.service;

import java.util.Map;

public interface TradeAnalyticsService {
    Map<String, Object> getAnalyticsSummary();
    Map<String, Object> getAnalyticsSummaryForClient(Long clientId);
    Map<String, Object> getAnalyticsSummaryForRelationshipManager(Long rmId);
}
