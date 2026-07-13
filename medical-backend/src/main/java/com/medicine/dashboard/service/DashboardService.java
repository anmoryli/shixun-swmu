package com.medicine.dashboard.service;

import org.springframework.stereotype.Service;

import com.medicine.dashboard.dto.DashboardView;
import com.medicine.dashboard.mapper.DashboardMapper;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardView getDashboard() {
        return new DashboardView(
                dashboardMapper.findCounts(),
                dashboardMapper.findDoctorLevels(),
                dashboardMapper.findTreatTypes(),
                dashboardMapper.findNews()
        );
    }
}
