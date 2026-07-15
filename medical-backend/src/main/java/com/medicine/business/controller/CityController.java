/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.controller;

import com.medicine.business.service.CityService;
import com.medicine.common.ApiResponse;
import com.medicine.common.ErrorCode;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/citys")
public class CityController {
    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping("/{pn}/{size}")
    @PreAuthorize("hasAuthority('city:read')")
    public ApiResponse<Map<String, Object>> page(@PathVariable Integer pn,
                                                 @PathVariable Integer size,
                                                 @RequestParam(required = false) String name) {
        return BusinessResponses.wrapped("cityPageInfo", service.page(pn, size, name));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('city:read')")
    public ApiResponse<Map<String, Object>> all() {
        return BusinessResponses.wrapped("cityPageInfo", service.all());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('city:write')")
    public ApiResponse<Map<String, Object>> add(@RequestParam Integer cityNumber) {
        if (service.exists(cityNumber)) {
            return ApiResponse.error(ErrorCode.DUPLICATE_DATA, "该城市已存在");
        }
        return BusinessResponses.pages(service.add(cityNumber, 5));
    }

    @DeleteMapping("/{cityId}")
    @PreAuthorize("hasAuthority('city:write')")
    public ApiResponse<Void> delete(@PathVariable Long cityId) {
        service.delete(cityId);
        return ApiResponse.success();
    }
}
