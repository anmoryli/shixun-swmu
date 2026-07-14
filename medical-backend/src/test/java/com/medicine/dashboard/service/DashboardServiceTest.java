/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.medicine.dashboard.dto.DashboardCounts;
import com.medicine.dashboard.dto.DashboardNews;
import com.medicine.dashboard.dto.DashboardView;
import com.medicine.dashboard.dto.NameValue;
import com.medicine.dashboard.mapper.DashboardMapper;

import org.junit.jupiter.api.Test;

import java.util.Collections;

class DashboardServiceTest {

    @Test
    void assemblesAllDashboardSections() {
        DashboardMapper mapper = mock(DashboardMapper.class);
        DashboardCounts counts = new DashboardCounts();
        counts.setDoctorCount(12);
        when(mapper.findCounts()).thenReturn(counts);
        when(mapper.findDoctorLevels()).thenReturn(Collections.singletonList(new NameValue("主任医师", 3)));
        when(mapper.findTreatTypes()).thenReturn(Collections.singletonList(new NameValue("肩部", 4)));
        when(mapper.findNews()).thenReturn(Collections.singletonList(new DashboardNews()));

        DashboardView result = new DashboardService(mapper).getDashboard();

        assertThat(result.getCounts().getDoctorCount()).isEqualTo(12);
        assertThat(result.getDoctorLevels()).hasSize(1);
        assertThat(result.getTreatTypes()).hasSize(1);
        assertThat(result.getNews()).hasSize(1);
    }
}
