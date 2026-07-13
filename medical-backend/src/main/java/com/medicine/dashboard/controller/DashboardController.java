package com.medicine.dashboard.controller;

import com.medicine.common.ApiResponse;
import com.medicine.dashboard.dto.DashboardView;
import com.medicine.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiResponse<DashboardView> dashboard() {
        return ApiResponse.success(dashboardService.getDashboard());
    }
}
