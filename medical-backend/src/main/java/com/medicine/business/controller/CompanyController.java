/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.controller;

import com.medicine.business.service.CompanyService;
import com.medicine.common.ApiResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/companys")
public class CompanyController {
    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @GetMapping("/{pn}/{size}")
    @PreAuthorize("hasAnyRole('1','2')")
    public ApiResponse<Map<String, Object>> page(@PathVariable Integer pn,
                                                 @PathVariable Integer size,
                                                 @RequestParam(required = false) String name) {
        return BusinessResponses.wrapped("pageInfo", service.page(pn, size, name));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('1','2')")
    public ApiResponse<Map<String, Object>> all() {
        return BusinessResponses.wrapped("pageInfo", service.all());
    }

    @PostMapping
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> request) {
        return BusinessResponses.pages(service.add(request, 5));
    }

    @PutMapping("/{companyId}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> update(@PathVariable Long companyId, @RequestBody Map<String, Object> request) {
        service.update(companyId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{companyId}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> delete(@PathVariable Long companyId) {
        service.delete(companyId);
        return ApiResponse.success();
    }
}
