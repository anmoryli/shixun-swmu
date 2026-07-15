/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.aspect;

import com.medicine.business.event.BusinessDataChangedEvent;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessDataChangeAspectTest {

    @Test
    void pointcutMarkerCanBeInvoked() {
        new BusinessDataChangeAspect(mock(ApplicationEventPublisher.class)).businessMutation();
    }

    @Test
    void publishesOperationAfterSuccessfulMutation() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("DoctorService.add(..)");

        new BusinessDataChangeAspect(publisher).publishDataChangedEvent(joinPoint);

        verify(publisher).publishEvent(argThat((Object event) -> event instanceof BusinessDataChangedEvent
                && "DoctorService.add(..)".equals(((BusinessDataChangedEvent) event).operation())));
    }
}
