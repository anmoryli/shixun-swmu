package com.medicine.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenServiceTest {

    @Test
    void acceptsLegacyRawAuthorizationToken() {
        assertThat(TokenService.normalizeAuthorization("  abc123  ")).isEqualTo("abc123");
    }

    @Test
    void alsoAcceptsBearerToken() {
        assertThat(TokenService.normalizeAuthorization("Bearer abc123")).isEqualTo("abc123");
    }
}
