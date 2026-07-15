/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BusinessDataChangeAspect {

    private final ApplicationEventPublisher eventPublisher;

    public BusinessDataChangeAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
