package com.medicine.business.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.medicine.business.service.DrugService;
import com.medicine.common.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/drugs")
public class DrugController {
    private final DrugService service;

    public DrugController(DrugService service) {
        this.service = service;
    }

    @GetMapping("/{pn}/{size}")
    @PreAuthorize("hasAnyRole('1','2')")
    public ApiResponse<Map<String, Object>> page(@PathVariable Integer pn,
                                                 @PathVariable Integer size,
                                                 @RequestParam(required = false) String name) {
        return BusinessResponses.wrapped("drugPageInfo", service.page(pn, size, name));
    }

    @PostMapping
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Map<String, Object>> add(@RequestBody Map<String, Object> request) {
        return BusinessResponses.pages(service.add(request, 5));
    }

    @PutMapping("/{drugId}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> update(@PathVariable Long drugId, @RequestBody Map<String, Object> request) {
        service.update(drugId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{drugId}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> delete(@PathVariable Long drugId) {
        service.delete(drugId);
        return ApiResponse.success();
    }
}
