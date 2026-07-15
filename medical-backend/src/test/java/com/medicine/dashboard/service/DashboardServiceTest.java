/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medicine.dashboard.DashboardCacheNames;
import com.medicine.dashboard.dto.DashboardCounts;
import com.medicine.dashboard.dto.DashboardNews;
import com.medicine.dashboard.dto.DashboardView;
import com.medicine.dashboard.dto.NameValue;
import com.medicine.dashboard.mapper.DashboardMapper;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheInterceptor;

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

    @Test
    void reusesCachedDashboardResult() {
        DashboardMapper mapper = mock(DashboardMapper.class);
        when(mapper.findCounts()).thenReturn(new DashboardCounts());
        when(mapper.findDoctorLevels()).thenReturn(Collections.emptyList());
        when(mapper.findTreatTypes()).thenReturn(Collections.emptyList());
        when(mapper.findNews()).thenReturn(Collections.emptyList());

        CacheInterceptor cacheInterceptor = new CacheInterceptor();
        cacheInterceptor.setCacheManager(new ConcurrentMapCacheManager(DashboardCacheNames.DASHBOARD));
        cacheInterceptor.setCacheOperationSources(new AnnotationCacheOperationSource());
        cacheInterceptor.afterPropertiesSet();
        ProxyFactory proxyFactory = new ProxyFactory(new DashboardService(mapper));
        proxyFactory.addAdvice(cacheInterceptor);
        DashboardService cachedService = (DashboardService) proxyFactory.getProxy();

        cachedService.getDashboard();
        cachedService.getDashboard();

        verify(mapper, times(1)).findCounts();
        verify(mapper, times(1)).findDoctorLevels();
        verify(mapper, times(1)).findTreatTypes();
        verify(mapper, times(1)).findNews();
    }
}
