package com.medicine.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
