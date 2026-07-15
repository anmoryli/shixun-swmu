/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.dashboard.event;

import com.medicine.business.event.BusinessDataChangedEvent;
import com.medicine.dashboard.DashboardCacheNames;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DashboardCacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(DashboardCacheInvalidationListener.class);

    @EventListener
    @CacheEvict(cacheNames = DashboardCacheNames.DASHBOARD, allEntries = true)
    public void invalidateDashboard(BusinessDataChangedEvent event) {
        log.info("Invalidating dashboard cache after {}", event.operation());
    }
}
