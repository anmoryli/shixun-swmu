/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard.task;

import com.medicine.dashboard.DashboardCacheNames;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DashboardCacheMaintenanceTask {

    @Scheduled(
            initialDelayString = "${app.cache.dashboard-eviction-delay-ms}",
            fixedDelayString = "${app.cache.dashboard-eviction-delay-ms}"
    )
    @CacheEvict(cacheNames = DashboardCacheNames.DASHBOARD, allEntries = true)
    public void evictDashboardCache() {
        // Periodic fallback for data changes made outside the application.
    }
}
