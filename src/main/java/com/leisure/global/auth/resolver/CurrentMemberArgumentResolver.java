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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof MemberPrincipal)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        MemberPrincipal result = (MemberPrincipal) auth.getPrincipal();

        return result.publicId();
    }
}