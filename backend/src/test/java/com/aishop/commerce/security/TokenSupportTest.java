package com.aishop.commerce.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenSupportTest {
    @Test
    void generatedTokensAreRandomAndUrlSafe() {
        String first = TokenSupport.randomToken();
        String second = TokenSupport.randomToken();
        assertThat(first).hasSizeGreaterThan(40).doesNotContain("+", "/", "=");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void hashingIsStableAndDoesNotExposeRawToken() {
        String hash = TokenSupport.sha256("secret-token");
        assertThat(hash).hasSize(64).isEqualTo(TokenSupport.sha256("secret-token"));
        assertThat(hash).doesNotContain("secret-token");
    }
}
