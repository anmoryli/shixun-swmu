/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard.service;

import com.medicine.dashboard.dto.DashboardView;
import com.medicine.dashboard.mapper.DashboardMapper;

import org.springframework.stereotype.Service;

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
