package com.medicine.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successUsesFrontendCompatibleCode() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.getCode()).isEqualTo(20000);
        assertThat(response.getData()).isEqualTo("ok");
    }
}
