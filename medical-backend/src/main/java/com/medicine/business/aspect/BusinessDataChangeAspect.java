/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BusinessDataChangeAspect {

    private final ApplicationEventPublisher eventPublisher;

    public BusinessDataChangeAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Pointcut("execution(public * com.medicine.business.service.*Service.add(..))"
            + " || execution(public * com.medicine.business.service.*Service.update(..))"
            + " || execution(public * com.medicine.business.service.*Service.delete(..))"
            + " || execution(public * com.medicine.business.service.*Service.resetPassword(..))")
    public void businessMutation() {
    }
}
