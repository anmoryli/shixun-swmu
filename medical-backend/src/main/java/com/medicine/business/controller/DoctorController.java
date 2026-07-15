/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.controller;

import com.medicine.business.service.DoctorService;
import com.medicine.common.ApiResponse;
import com.medicine.common.ErrorCode;
import com.medicine.security.AuthSession;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {
    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('doctor:read')")
    public ApiResponse<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer pn,
                                                 @RequestParam(defaultValue = "5") Integer size,
                                                 @RequestParam(required = false) String keyword) {
        return BusinessResponses.wrapped("doctorInfo", service.page(pn, size, keyword));
    }

    @GetMapping("/info")
    @PreAuthorize("hasAuthority('doctor:read')")
    public ApiResponse<Map<String, Object>> levelAndType() {
        return ApiResponse.success(service.levelAndType());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('doctor:write')")
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> request) {
        int pages = service.add(request, 5);
        return pages < 0
                ? ApiResponse.error(ErrorCode.DUPLICATE_DATA, "该手机号已被注册")
                : BusinessResponses.pages(pages);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('doctor:write')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        if (!service.update(id, request)) {
            return ApiResponse.error(ErrorCode.DUPLICATE_DATA, "该手机号已被注册");
        }
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('doctor:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success();
    }

    @PutMapping("/reset/{accountId}")
    @PreAuthorize("hasAuthority('doctor:reset-password')")
    public ApiResponse<Map<String, Object>> reset(@PathVariable Long accountId,
                                                   @AuthenticationPrincipal AuthSession operator) {
        String tempPassword = service.resetPassword(accountId, operator.getUserId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tempPassword", tempPassword);
        return ApiResponse.success(data);
    }
}
