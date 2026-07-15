/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successUsesFrontendCompatibleCode() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.getCode()).isEqualTo(20000);
        assertThat(response.getData()).isEqualTo("ok");
    }

    @Test
    void javaBeanAccessorsAndEmptySuccessAreCovered() {
        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(123);
        response.setMessage("message");
        response.setData("data");
        assertThat(response.getCode()).isEqualTo(123);
        assertThat(response.getMessage()).isEqualTo("message");
        assertThat(response.getData()).isEqualTo("data");
        assertThat(ApiResponse.success().getData()).isNull();
        assertThat(ApiResponse.error(500, "error").getMessage()).isEqualTo("error");
    }
}
