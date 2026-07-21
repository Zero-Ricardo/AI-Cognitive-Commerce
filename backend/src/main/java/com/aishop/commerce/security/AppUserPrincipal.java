package com.aishop.commerce.security;

import com.aishop.commerce.domain.Enums;

import java.util.Set;

public record AppUserPrincipal(Long id, String username, String nickname, Set<Enums.Role> roles) {
    public boolean isAdmin() { return roles.contains(Enums.Role.ADMIN); }
}
