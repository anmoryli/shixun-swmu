package com.medicine.business.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.medicine.business.service.MedicalPolicyService;
import com.medicine.common.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/medical_policys")
public class MedicalPolicyController {
    private final MedicalPolicyService service;

    public MedicalPolicyController(MedicalPolicyService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('1','2')")
    public ApiResponse<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer pn,
                                                 @RequestParam(defaultValue = "5") Integer size,
                                                 @RequestParam(required = false) Long id,
                                                 @RequestParam(required = false) Long cityId,
                                                 @RequestParam(required = false) String title,
                                                 @RequestParam(required = false) String updateTime) {
        return BusinessResponses.wrapped("policyInfo", service.page(pn, size, id, cityId, title, updateTime));
    }

    @PostMapping
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> request) {
        return BusinessResponses.pages(service.add(request, 5));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        service.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success();
    }
}
