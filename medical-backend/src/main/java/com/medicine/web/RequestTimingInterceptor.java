/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Records the result and execution time of requests handled by Spring MVC.
 */
@Component
public class RequestTimingInterceptor implements HandlerInterceptor {

    static final String START_TIME_ATTRIBUTE = RequestTimingInterceptor.class.getName() + ".startTime";

    private static final Logger log = LoggerFactory.getLogger(RequestTimingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception exception) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        request.removeAttribute(START_TIME_ATTRIBUTE);
        if (!(startTime instanceof Long)) {
            return;
        }

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - (Long) startTime);
        if (exception == null) {
            log.info("MVC request completed: method={}, path={}, status={}, elapsedMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMillis);
        } else {
            log.warn("MVC request failed: method={}, path={}, status={}, elapsedMs={}, exception={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMillis,
                    exception.getClass().getSimpleName());
        }
    }
}
