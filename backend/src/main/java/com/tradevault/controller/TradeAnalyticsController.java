package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.entity.User;
import com.tradevault.service.TradeAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@CrossOrigin(origins = "*")
public class TradeAnalyticsController {

    @Autowired
    private TradeAnalyticsService analyticsService;

    @Autowired
    private com.tradevault.repository.UserRepository userRepository;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsSummary(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if ("CLIENT".equals(user.getRole())) {
            if (user.getCorporateClient() == null) {
                return ResponseEntity.ok(ApiResponse.success("Analytics summary retrieved", java.util.Collections.emptyMap()));
            }
            return ResponseEntity.ok(ApiResponse.success("Analytics summary retrieved", 
                    analyticsService.getAnalyticsSummaryForClient(user.getCorporateClient().getId())));
        }
        return ResponseEntity.ok(ApiResponse.success("Analytics summary retrieved", analyticsService.getAnalyticsSummary()));
    }
}
