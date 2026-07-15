/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard;

import com.medicine.business.event.BusinessDataChangedEvent;
import com.medicine.dashboard.event.DashboardCacheInvalidationListener;
import com.medicine.dashboard.service.DashboardService;
import com.medicine.dashboard.task.DashboardCacheMaintenanceTask;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardCacheContractTest {

    @Test
    void cacheInvalidationHandlersCanRun() {
        new DashboardCacheInvalidationListener()
                .invalidateDashboard(new BusinessDataChangedEvent("DoctorService.add(..)"));
        new DashboardCacheMaintenanceTask().evictDashboardCache();
    }

    @Test
    void declaresCacheReadAndInvalidationPolicies() throws NoSuchMethodException {
        Method read = DashboardService.class.getMethod("getDashboard");
        Method eventEviction = DashboardCacheInvalidationListener.class.getMethod(
                "invalidateDashboard", com.medicine.business.event.BusinessDataChangedEvent.class);
        Method scheduledEviction = DashboardCacheMaintenanceTask.class.getMethod("evictDashboardCache");

        Cacheable cacheable = AnnotatedElementUtils.findMergedAnnotation(read, Cacheable.class);
        CacheEvict eventCacheEvict = AnnotatedElementUtils.findMergedAnnotation(eventEviction, CacheEvict.class);
        CacheEvict scheduledCacheEvict = AnnotatedElementUtils.findMergedAnnotation(scheduledEviction, CacheEvict.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.cacheNames()).containsExactly(DashboardCacheNames.DASHBOARD);
        assertThat(cacheable.sync()).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(eventEviction, EventListener.class)).isTrue();
        assertThat(eventCacheEvict).isNotNull();
        assertThat(eventCacheEvict.allEntries()).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(scheduledEviction, Scheduled.class)).isTrue();
        assertThat(scheduledCacheEvict).isNotNull();
        assertThat(scheduledCacheEvict.allEntries()).isTrue();
    }
}
