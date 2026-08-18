package com.leisure.global.auth.principal;

import java.security.Principal;

public record MemberPrincipal(String publicId, String email) implements Principal {

    @Override
    public String getName() {
        return publicId;
    }
}
