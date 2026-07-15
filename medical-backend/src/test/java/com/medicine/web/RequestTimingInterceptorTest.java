/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestTimingInterceptorTest {

    private final RequestTimingInterceptor interceptor = new RequestTimingInterceptor();

    @Test
    void shouldRecordAndClearRequestStartTime() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/city/getCityInfo");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertTrue(request.getAttribute(RequestTimingInterceptor.START_TIME_ATTRIBUTE) instanceof Long);

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(request.getAttribute(RequestTimingInterceptor.START_TIME_ATTRIBUTE));
    }

    @Test
    void shouldIgnoreCompletionWhenStartTimeIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/session");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        assertNull(request.getAttribute(RequestTimingInterceptor.START_TIME_ATTRIBUTE));
    }

    @Test
    void shouldRecordFailedRequestCompletion() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/doctor/add");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), new IllegalStateException("failed"));

        assertNull(request.getAttribute(RequestTimingInterceptor.START_TIME_ATTRIBUTE));
    }
}
