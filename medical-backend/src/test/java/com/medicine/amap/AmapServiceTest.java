/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.amap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medicine.common.BusinessException;
import com.medicine.common.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

class AmapServiceTest {

    private RestTemplate restTemplate;
    private RestTemplateBuilder builder;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
    }

    @Test
    void rejectsCallsWhenServerKeyIsMissing() {
        AmapService service = new AmapService(" ", builder);

        assertInternalError(() -> service.reverseGeocode(104.0, 30.0), "未配置");
        verify(restTemplate, never()).getForObject(anyString(), eq(Map.class), any(), any());
    }

    @Test
    void returnsFormattedAddressFromAmapResponse() {
        when(restTemplate.getForObject(anyString(), eq(Map.class), any(), any()))
                .thenReturn(Map.of("regeocode", Map.of("formatted_address", "四川省泸州市")));

        String address = new AmapService("server-key", builder).reverseGeocode(105.44, 28.87);

        assertThat(address).isEqualTo("四川省泸州市");
        verify(restTemplate).getForObject(
                "https://restapi.amap.com/v3/geocode/regeo?key={key}&output=json&location={location}",
                Map.class, "server-key", "105.44,28.87");
    }

    @Test
    void rejectsEmptyAndMalformedServiceResponses() {
        when(restTemplate.getForObject(anyString(), eq(Map.class), any(), any())).thenReturn(null);
        assertInternalError(() -> new AmapService("key", builder).reverseGeocode(1, 2), "无响应");

        when(restTemplate.getForObject(anyString(), eq(Map.class), any(), any()))
                .thenReturn(Map.of("regeocode", Map.of("formatted_address", "")));
        assertInternalError(() -> new AmapService("key", builder).reverseGeocode(1, 2), "逆地理编码失败");
    }

    @Test
    void mapsTransportFailuresWithoutLeakingTheirDetails() {
        when(restTemplate.getForObject(anyString(), eq(Map.class), any(), any()))
                .thenThrow(new IllegalStateException("secret transport detail"));

        assertInternalError(() -> new AmapService("key", builder).reverseGeocode(1, 2), "服务调用失败");
    }

    private void assertInternalError(Runnable operation, String messageFragment) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(messageFragment)
                .extracting("code")
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }
}
