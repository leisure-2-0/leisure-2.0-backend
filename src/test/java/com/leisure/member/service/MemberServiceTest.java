package com.leisure.member.service;

import com.leisure.auth.dto.result.ReissueResult;
import com.leisure.global.auth.JwtTokenProvider;
import com.leisure.global.auth.store.RedisRefreshTokenStore;
import com.leisure.global.auth.store.RedisTokenStatusStore;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.domain.MemberRole;
import com.leisure.member.dto.request.PasswordChangeRequest;
import com.leisure.member.dto.request.ProfileChangeRequest;
import com.leisure.member.dto.request.SignUpRequest;
import com.leisure.member.dto.response.ProfileChangeResponse;
import com.leisure.member.dto.response.SignUpResponse;
import com.leisure.member.event.MemberWithdrawnEvent;
import com.leisure.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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

    @Mock
    private MemberReader reader;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RedisTokenStatusStore tokenStatusStore;

    @Mock
    private RedisRefreshTokenStore refreshTokenStore;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private MemberService memberService;

    private SignUpRequest request(String email, String password, String passwordCheck, String nickname) {
        return new SignUpRequest(email, password, passwordCheck, nickname);
    }

    @Nested
    @DisplayName("회원가입 성공")
    class Success {

        @Test
        @DisplayName("정상 요청 시 발급된 publicId를 담아 응답한다")
        void signUp_success() {
            // given
            SignUpRequest request = request("user@leisure.com", "Passw0rd!", "Passw0rd!", "nickname");

            given(repository.existsByEmailAndDeletedAtIsNull(request.email())).willReturn(false);
            given(repository.existsByNicknameAndDeletedAtIsNull(request.nickname())).willReturn(false);
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

            given(repository.existsByEmailAndDeletedAtIsNull(anyString())).willReturn(false);
            given(repository.existsByNicknameAndDeletedAtIsNull(anyString())).willReturn(false);
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
            given(repository.existsByEmailAndDeletedAtIsNull(request.email())).willReturn(true);

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
            given(repository.existsByEmailAndDeletedAtIsNull(request.email())).willReturn(false);
            given(repository.existsByNicknameAndDeletedAtIsNull(request.nickname())).willReturn(true);

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

            verify(repository, never()).existsByEmailAndDeletedAtIsNull(anyString());
            verify(encoder, never()).encode(anyString());
            verify(repository, never()).save(any(Member.class));
        }

        @Test
        @DisplayName("사전 검사를 통과했더라도 DB 유니크 제약 위반 시 EMAIL_DUPLICATE 예외를 던진다")
        void signUp_dataIntegrityViolation() {
            // given (동시성으로 사전 검사를 통과한 뒤 DB 제약에서 걸리는 경우)
            SignUpRequest request = request("race@leisure.com", "Passw0rd!", "Passw0rd!", "nickname");
            given(repository.existsByEmailAndDeletedAtIsNull(request.email())).willReturn(false);
            given(repository.existsByNicknameAndDeletedAtIsNull(request.nickname())).willReturn(false);
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

    @Nested
    @DisplayName("프로필 수정(changeProfile)")
    class ChangeProfile {

        private static final String PUBLIC_ID = "public-id";

        private Member existingMember() {
            Member member = Member.create("user@leisure.com", "ENCODED", "oldNick");
            member.changeProfileImageUrl("old.png");   // 프로필수정으로 이미 저장된 상태 흉내
            ReflectionTestUtils.setField(member, "memberId", 1L);
            ReflectionTestUtils.setField(member, "publicId", PUBLIC_ID);
            return member;
        }

        @Test
        @DisplayName("닉네임과 프로필 이미지를 함께 수정한다")
        void success() {
            Member member = existingMember();
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);
            given(repository.existsByNicknameAndDeletedAtIsNull("newNick")).willReturn(false);
            ProfileChangeRequest request = new ProfileChangeRequest("newNick", "new.png");

            ProfileChangeResponse response = memberService.changeProfile(PUBLIC_ID, request);

            assertThat(response.nickname()).isEqualTo("newNick");
            assertThat(response.profileImageUrl()).isEqualTo("new.png");
        }

        @Test
        @DisplayName("바꾸려는 닉네임이 이미 사용 중이면 NICKNAME_DUPLICATE 예외를 던지고 변경하지 않는다")
        void nicknameDuplicate() {
            Member member = existingMember();
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);
            given(repository.existsByNicknameAndDeletedAtIsNull("dupNick")).willReturn(true);
            ProfileChangeRequest request = new ProfileChangeRequest("dupNick", null);

            assertThatThrownBy(() -> memberService.changeProfile(PUBLIC_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_DUPLICATE);

            assertThat(member.getNickname()).isEqualTo("oldNick");
        }

        @Test
        @DisplayName("현재와 같은 닉네임이면 중복 검사를 건너뛴다(내 것 제외)")
        void sameNicknameSkipsDuplicateCheck() {
            Member member = existingMember();
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);
            ProfileChangeRequest request = new ProfileChangeRequest("oldNick", null);

            memberService.changeProfile(PUBLIC_ID, request);

            verify(repository, never()).existsByNicknameAndDeletedAtIsNull(anyString());
            assertThat(member.getNickname()).isEqualTo("oldNick");
        }

        @Test
        @DisplayName("프로필 이미지만 보내면 닉네임 중복 검사 없이 이미지만 바꾼다")
        void partialProfileOnly() {
            Member member = existingMember();
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);
            ProfileChangeRequest request = new ProfileChangeRequest(null, "new.png");

            ProfileChangeResponse response = memberService.changeProfile(PUBLIC_ID, request);

            verify(repository, never()).existsByNicknameAndDeletedAtIsNull(anyString());
            assertThat(response.nickname()).isEqualTo("oldNick");
            assertThat(response.profileImageUrl()).isEqualTo("new.png");
        }

        @Test
        @DisplayName("프로필 이미지를 빈 문자열로 보내면 이미지를 제거한다(null)")
        void removeProfileImage() {
            Member member = existingMember();
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);
            ProfileChangeRequest request = new ProfileChangeRequest(null, "");

            ProfileChangeResponse response = memberService.changeProfile(PUBLIC_ID, request);

            assertThat(response.profileImageUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("회원 탈퇴(withdraw)")
    class Withdraw {

        private static final String PUBLIC_ID = "public-id";

        @Test
        @DisplayName("회원을 소프트 삭제하고 MemberWithdrawnEvent를 발행한다")
        void success() {
            Member member = Member.create("user@leisure.com", "ENCODED", "nick");
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);

            memberService.withdraw(PUBLIC_ID);

            assertThat(member.getDeletedAt()).isNotNull();
            verify(eventPublisher).publishEvent(new MemberWithdrawnEvent(PUBLIC_ID));
        }
    }

    @Nested
    @DisplayName("비밀번호 변경(changePassword)")
    class ChangePassword {

        private static final String PUBLIC_ID = "public-id";
        private static final String EMAIL = "user@leisure.com";
        private static final String STORED_PASSWORD = "STORED_ENCODED";

        private Member member() {
            return Member.create(EMAIL, STORED_PASSWORD, "nick");
        }

        @Test
        @DisplayName("현재 비밀번호 확인 후 교체하고, 전 세션 무효화 + 새 토큰을 재발급한다")
        void success() {
            Member member = member();
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);
            given(encoder.matches("curPw1!", STORED_PASSWORD)).willReturn(true);
            given(encoder.encode("newPw1!")).willReturn("NEW_ENCODED");
            given(tokenStatusStore.getCurrentInvalidationVersion(PUBLIC_ID)).willReturn(1L);
            given(tokenProvider.issueAccessToken(PUBLIC_ID, EMAIL, MemberRole.MEMBER, 1L)).willReturn("access");
            given(tokenProvider.issueRefreshToken(PUBLIC_ID, EMAIL, MemberRole.MEMBER, 1L)).willReturn("refresh");
            given(tokenProvider.getRefreshTokenTtl()).willReturn(1000L);
            PasswordChangeRequest request = new PasswordChangeRequest("curPw1!", "newPw1!", "newPw1!");

            ReissueResult result = memberService.changePassword(PUBLIC_ID, request);

            assertThat(result.accessToken()).isEqualTo("access");
            assertThat(result.refreshToken()).isEqualTo("refresh");
            verify(tokenStatusStore).increaseInvalidationVersion(PUBLIC_ID);
            verify(refreshTokenStore).save(PUBLIC_ID, "refresh", 1000L);
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 PASSWORD_MISMATCH 예외를 던진다")
        void currentPasswordMismatch() {
            Member member = member();
            given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member);
            given(encoder.matches("wrong1!", STORED_PASSWORD)).willReturn(false);
            PasswordChangeRequest request = new PasswordChangeRequest("wrong1!", "newPw1!", "newPw1!");

            assertThatThrownBy(() -> memberService.changePassword(PUBLIC_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

            verify(tokenStatusStore, never()).increaseInvalidationVersion(anyString());
        }

        @Test
        @DisplayName("새 비밀번호와 확인값이 다르면 PASSWORD_MISMATCH 예외를 던지고 회원 조회도 하지 않는다")
        void newPasswordConfirmMismatch() {
            PasswordChangeRequest request = new PasswordChangeRequest("curPw1!", "newPw1!", "different1!");

            assertThatThrownBy(() -> memberService.changePassword(PUBLIC_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PASSWORD_MISMATCH);

            verify(reader, never()).getMemberByPublicId(anyString());
        }
    }

    @Nested
    @DisplayName("이메일 중복 확인(checkEmail)")
    class CheckEmail {

        @Test
        @DisplayName("사용 가능한 이메일이면 예외 없이 통과한다")
        void available() {
            given(repository.existsByEmailAndDeletedAtIsNull("new@leisure.com")).willReturn(false);

            memberService.checkEmail("new@leisure.com");
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 EMAIL_DUPLICATE 예외를 던진다")
        void duplicate() {
            given(repository.existsByEmailAndDeletedAtIsNull("dup@leisure.com")).willReturn(true);

            assertThatThrownBy(() -> memberService.checkEmail("dup@leisure.com"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATE);
        }
    }

    @Nested
    @DisplayName("닉네임 중복 확인(checkNickname)")
    class CheckNickname {

        @Test
        @DisplayName("사용 가능한 닉네임이면 예외 없이 통과한다")
        void available() {
            given(repository.existsByNicknameAndDeletedAtIsNull("newNick")).willReturn(false);

            memberService.checkNickname("newNick");
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 NICKNAME_DUPLICATE 예외를 던진다")
        void duplicate() {
            given(repository.existsByNicknameAndDeletedAtIsNull("dupNick")).willReturn(true);

            assertThatThrownBy(() -> memberService.checkNickname("dupNick"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NICKNAME_DUPLICATE);
        }
    }
}
