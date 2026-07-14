/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.amap;

import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 高德地图 Web 服务封装。
 * Web 服务 Key 仅在后端持有，前端通过 /api/regeo 代理调用，避免 Key 泄露到浏览器。
 */
@Service
public class AmapService {

    private final String webKey;
    private final RestTemplate restTemplate;

    public AmapService(@Value("${app.amap.web-key:}") String webKey,
                       RestTemplateBuilder restTemplateBuilder) {
        this.webKey = webKey;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * 调用高德逆地理编码 Web 服务，将经纬度转为格式化地址。
     */
    public String reverseGeocode(double longitude, double latitude) {
        if (webKey == null || webKey.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "高德地图服务未配置");
        }
        String url = "https://restapi.amap.com/v3/geocode/regeo?key={key}&output=json&location={location}";
        String location = longitude + "," + latitude;
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class, webKey, location);
            if (response == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "高德地图服务无响应");
            }
            Object regeocode = response.get("regeocode");
            if (regeocode instanceof Map) {
                Object formatted = ((Map<?, ?>) regeocode).get("formatted_address");
                if (formatted instanceof String && !((String) formatted).isEmpty()) {
                    return (String) formatted;
                }
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "高德逆地理编码失败");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "高德地图服务调用失败");
        }
    }
}
