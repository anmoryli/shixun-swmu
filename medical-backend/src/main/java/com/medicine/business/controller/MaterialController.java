/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.controller;

import com.medicine.business.service.MaterialService;
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
@RequestMapping("/api/materials")
public class MaterialController {
    private final MaterialService service;

    public MaterialController(MaterialService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('material:read')")
    public ApiResponse<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer pn,
                                                 @RequestParam(defaultValue = "5") Integer size,
                                                 @RequestParam(required = false) String keyword) {
        return BusinessResponses.wrapped("materialInfo", service.page(pn, size, keyword));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('material:write')")
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> request) {
        return BusinessResponses.pages(service.add(request, 5));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('material:write')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        service.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('material:write')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success();
    }
}
