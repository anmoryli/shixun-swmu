package com.medicine.business.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.medicine.business.service.SaleService;
import com.medicine.common.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SaleController {
    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @GetMapping("/{pn}/{size}")
    @PreAuthorize("hasAnyRole('1','2')")
    public ApiResponse<Map<String, Object>> page(@PathVariable Integer pn,
                                                 @PathVariable Integer size,
                                                 @RequestParam(required = false) String name) {
        return BusinessResponses.wrapped("salePageInfo", service.page(pn, size, name));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('1','2')")
    public ApiResponse<Map<String, Object>> all() {
        return BusinessResponses.wrapped("salePageInfo", service.all());
    }

    @PostMapping
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> request) {
        return BusinessResponses.pages(service.add(request, 5));
    }

    @PutMapping("/{saleId}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> update(@PathVariable Long saleId, @RequestBody Map<String, Object> request) {
        service.update(saleId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{saleId}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> delete(@PathVariable Long saleId) {
        service.delete(saleId);
        return ApiResponse.success();
    }
}
