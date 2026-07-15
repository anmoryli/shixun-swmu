/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.business.controller;

import com.medicine.business.service.SaleService;
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
@RequestMapping("/api/sales")
public class SaleController {
    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @GetMapping("/{pn}/{size}")
    @PreAuthorize("hasAuthority('sale:read')")
    public ApiResponse<Map<String, Object>> page(@PathVariable Integer pn,
                                                 @PathVariable Integer size,
                                                 @RequestParam(required = false) String name) {
        return BusinessResponses.wrapped("salePageInfo", service.page(pn, size, name));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sale:read')")
    public ApiResponse<Map<String, Object>> all() {
        return BusinessResponses.wrapped("salePageInfo", service.all());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sale:write')")
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> request) {
        return BusinessResponses.pages(service.add(request, 5));
    }

    @PutMapping("/{saleId}")
    @PreAuthorize("hasAuthority('sale:write')")
    public ApiResponse<Void> update(@PathVariable Long saleId, @RequestBody Map<String, Object> request) {
        service.update(saleId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{saleId}")
    @PreAuthorize("hasAuthority('sale:write')")
    public ApiResponse<Void> delete(@PathVariable Long saleId) {
        service.delete(saleId);
        return ApiResponse.success();
    }
}
