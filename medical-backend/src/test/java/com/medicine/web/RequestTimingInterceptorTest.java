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
}
