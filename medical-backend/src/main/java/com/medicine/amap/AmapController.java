/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.amap;

import com.medicine.common.ApiResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 高德地图代理接口。
 * 前端不直接持有高德 Web Key，统一通过本接口转发逆地理编码请求。
 * /api/regeo 落入 SecurityConfig 的 anyRequest().authenticated()，需登录后调用。
 */
@RestController
@RequestMapping("/api")
public class AmapController {

    private final AmapService amapService;

    public AmapController(AmapService amapService) {
        this.amapService = amapService;
    }

    @GetMapping("/regeo")
    @PreAuthorize("hasAuthority('sale-map:read')")
    public ApiResponse<String> reverseGeocode(@RequestParam double lng,
                                              @RequestParam double lat) {
        return ApiResponse.success(amapService.reverseGeocode(lng, lat));
    }
}
