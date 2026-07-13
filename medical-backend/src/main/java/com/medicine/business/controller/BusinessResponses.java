package com.medicine.business.controller;

import com.medicine.common.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

final class BusinessResponses {
    private BusinessResponses() {
    }

    static ApiResponse<Map<String, Object>> wrapped(String key, Object value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(key, value);
        return ApiResponse.success(data);
    }

    static ApiResponse<Map<String, Object>> pages(int pages) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pages", pages);
        return ApiResponse.success(data);
    }
}
