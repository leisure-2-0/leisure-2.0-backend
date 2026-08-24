package com.leisure.global.auth.resolver;

import com.leisure.global.auth.CurrentMember;
import com.leisure.global.auth.principal.MemberPrincipal;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMember.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

        // JwtAuthenticationFilter가 토큰을 검증하면 채워둔 인증 정보
        // 토큰이 없는 익명 요청이면 auth가 null이거나 principal이 MemberPrincipal이 아니다
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 파라미터에 붙은 @CurrentMember의 required 값을 읽어 인증 필수 여부를 판단
        CurrentMember annotation = parameter.getParameterAnnotation(CurrentMember.class);

        // 인증이 없는 경우(비로그인 / 익명 토큰)
        if (auth == null || !(auth.getPrincipal() instanceof MemberPrincipal)) {
            // 로그인 필수 API면 예외, 비로그인 허용 API(required=false)면 null 주입
            if (annotation.required()) {
                throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
            }
            return null;
        }

        // 인증된 회원이면 publicId를 주입
        MemberPrincipal result = (MemberPrincipal) auth.getPrincipal();
        return result.publicId();
    }
}