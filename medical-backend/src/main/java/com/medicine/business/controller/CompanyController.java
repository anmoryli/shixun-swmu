package com.medicine.business.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.medicine.business.service.CompanyService;
import com.medicine.common.ApiResponse;

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
