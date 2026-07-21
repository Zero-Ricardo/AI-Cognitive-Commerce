package com.aishop.commerce.security;

import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.repository.SessionTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final SessionTokenRepository tokens;

    public SessionAuthenticationFilter(SessionTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = cookie(request, "access_token");
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokens.findByAccessTokenHashAndRevokedFalse(TokenSupport.sha256(token))
                    .filter(session -> session.getAccessExpiresAt().isAfter(Instant.now()))
                    .filter(session -> session.getUser().getStatus() == Enums.UserStatus.ACTIVE)
                    .ifPresent(session -> {
                        var user = session.getUser();
                        var principal = new AppUserPrincipal(user.getId(), user.getUsername(), user.getNickname(), user.getRoles());
                        var authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .toList();
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, authorities));
                    });
        }
        chain.doFilter(request, response);
    }

    public static String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies()).filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }
}
