package com.leisure.member.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.dto.request.SignUpRequest;
import com.leisure.member.dto.response.SignUpResponse;
import com.leisure.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private MemberService memberService;

    private SignUpRequest request(String email, String password, String passwordCheck, String nickname) {
        return new SignUpRequest(email, password, passwordCheck, nickname, null);
    }

    @Nested
    @DisplayName("회원가입 성공")
    class Success {

        @Test
        @DisplayName("정상 요청 시 발급된 publicId를 담아 응답한다")
        void signUp_success() {
            // given
            SignUpRequest request = request("user@leisure.com", "Passw0rd!", "Passw0rd!", "nickname");

            given(repository.existsByEmail(request.email())).willReturn(false);
            given(repository.existsByNickname(request.nickname())).willReturn(false);
            given(encoder.encode(request.password())).willReturn("ENCODED_PASSWORD");
            // save 시점에 @PrePersist가 하는 publicId 발급을 흉내
            given(repository.save(any(Member.class))).willAnswer(invocation -> {
                Member saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "publicId", "generated-public-id");
                return saved;
            });

            // when
            SignUpResponse response = memberService.signUp(request);

            // then
            assertThat(response.publicId()).isEqualTo("generated-public-id");
            verify(repository).save(any(Member.class));
        }

        @Test
        @DisplayName("비밀번호는 인코딩된 값으로 저장된다")
        void signUp_encodesPassword() {
            // given
            SignUpRequest request = request("user@leisure.com", "Passw0rd!", "Passw0rd!", "nickname");

            given(repository.existsByEmail(anyString())).willReturn(false);
            given(repository.existsByNickname(anyString())).willReturn(false);
            given(encoder.encode(request.password())).willReturn("ENCODED_PASSWORD");
            given(repository.save(any(Member.class))).willAnswer(invocation -> {
                Member saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "publicId", "generated-public-id");
                return saved;
            });

            // when
            memberService.signUp(request);

            // then
            verify(encoder).encode("Passw0rd!");
        }
    }

    @Nested
    @DisplayName("회원가입 실패")
    class Failure {

        @Test
        @DisplayName("이메일이 중복되면 EMAIL_DUPLICATE 예외를 던진다")
        void signUp_duplicateEmail() {
            // given
            SignUpRequest request = request("dup@leisure.com", "Passw0rd!", "Passw0rd!", "nickname");
            given(repository.existsByEmail(request.email())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);

            verify(repository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("닉네임이 중복되면 NICKNAME_DUPLICATE 예외를 던진다")
        void signUp_duplicateNickname() {
            // given
            SignUpRequest request = request("user@leisure.com", "Passw0rd!", "Passw0rd!", "dupNick");
            given(repository.existsByEmail(request.email())).willReturn(false);
            given(repository.existsByNickname(request.nickname())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_DUPLICATE);

            verify(repository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("비밀번호와 확인값이 다르면 PASSWORD_MISMATCH 예외를 던진다")
        void signUp_passwordMismatch() {
            // given (비밀번호 검사가 중복 검사보다 먼저이므로 existsBy* 는 호출되지 않는다)
            SignUpRequest request = request("user@leisure.com", "Passw0rd!", "Different1!", "nickname");

            // when & then
            assertThatThrownBy(() -> memberService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

            verify(repository, never()).existsByEmail(anyString());
            verify(encoder, never()).encode(anyString());
            verify(repository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("사전 검사를 통과했더라도 DB 유니크 제약 위반 시 EMAIL_DUPLICATE 예외를 던진다")
        void signUp_dataIntegrityViolation() {
            // given (동시성으로 사전 검사를 통과한 뒤 DB 제약에서 걸리는 경우)
            SignUpRequest request = request("race@leisure.com", "Passw0rd!", "Passw0rd!", "nickname");
            given(repository.existsByEmail(request.email())).willReturn(false);
            given(repository.existsByNickname(request.nickname())).willReturn(false);
            given(encoder.encode(request.password())).willReturn("ENCODED_PASSWORD");
            given(repository.save(any(Member.class)))
                    .willThrow(new DataIntegrityViolationException("unique constraint"));

            // when & then
            assertThatThrownBy(() -> memberService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);
        }
    }
}
