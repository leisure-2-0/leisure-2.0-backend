package com.leisure.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member 도메인")
class MemberTest {

    @Test
    @DisplayName("normalizeEmail은 앞뒤 공백을 제거하고 소문자로 정규화한다")
    void normalizeEmail_trimAndLowercase() {
        assertThat(Member.normalizeEmail("  John@X.COM ")).isEqualTo("john@x.com");
    }

    @Test
    @DisplayName("normalizeEmail은 이미 정규화된 이메일을 그대로 반환한다(멱등)")
    void normalizeEmail_idempotent() {
        assertThat(Member.normalizeEmail("user@leisure.com")).isEqualTo("user@leisure.com");
    }
}
