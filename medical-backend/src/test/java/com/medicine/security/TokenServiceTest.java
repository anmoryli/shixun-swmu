/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenServiceTest {

    @Test
    void acceptsLegacyRawAuthorizationToken() {
        assertThat(TokenService.normalizeAuthorization("  abc123  ")).hasValue("abc123");
    }

    @Test
    void alsoAcceptsBearerToken() {
        assertThat(TokenService.normalizeAuthorization("Bearer abc123")).hasValue("abc123");
    }
}
