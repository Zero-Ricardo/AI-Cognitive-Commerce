package com.aishop.commerce.auth;

import com.aishop.commerce.common.BusinessException;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.domain.SessionToken;
import com.aishop.commerce.domain.User;
import com.aishop.commerce.repository.SessionTokenRepository;
import com.aishop.commerce.repository.UserRepository;
import com.aishop.commerce.security.TokenSupport;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {
    private final UserRepository users;
    private final SessionTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final long accessMinutes;
    private final long refreshDays;

    public AuthService(UserRepository users, SessionTokenRepository tokens, PasswordEncoder passwordEncoder,
                       @Value("${app.auth.access-token-minutes}") long accessMinutes,
                       @Value("${app.auth.refresh-token-days}") long refreshDays) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public User register(String username, String phone, String email, String password) {
        username = username.trim().toLowerCase(Locale.ROOT);
        phone = blankToNull(phone);
        email = blankToNull(email);
        if (email != null) email = email.toLowerCase(Locale.ROOT);
        if (users.existsByUsernameIgnoreCase(username) || (phone != null && users.existsByPhone(phone))
                || (email != null && users.existsByEmailIgnoreCase(email))) {
            throw BusinessException.conflict("RESOURCE_CONFLICT", "用户名、手机号或邮箱已被使用");
        }
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setNickname(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(Set.of(Enums.Role.USER));
        return users.save(user);
    }

    @Transactional
    public IssuedSession login(String account, String password) {
        User user = users.findByUsernameIgnoreCaseOrPhoneOrEmailIgnoreCase(account, account, account)
                .orElseThrow(() -> unauthorized("账号或密码错误"));
        if (user.getStatus() != Enums.UserStatus.ACTIVE || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw unauthorized("账号或密码错误");
        }
        return issue(user);
    }

    @Transactional
    public IssuedSession refresh(String rawRefreshToken) {
        if (rawRefreshToken == null) throw unauthorized("登录状态已失效");
        SessionToken old = tokens.findByRefreshTokenHashAndRevokedFalse(TokenSupport.sha256(rawRefreshToken))
                .filter(value -> value.getRefreshExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> unauthorized("登录状态已失效"));
        old.setRevoked(true);
        return issue(old.getUser());
    }

    @Transactional
    public void logout(String rawAccessToken) {
        if (rawAccessToken == null) return;
        tokens.findByAccessTokenHashAndRevokedFalse(TokenSupport.sha256(rawAccessToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private IssuedSession issue(User user) {
        String access = TokenSupport.randomToken();
        String refresh = TokenSupport.randomToken();
        Instant now = Instant.now();
        SessionToken session = new SessionToken();
        session.setUser(user);
        session.setAccessTokenHash(TokenSupport.sha256(access));
        session.setRefreshTokenHash(TokenSupport.sha256(refresh));
        session.setAccessExpiresAt(now.plus(accessMinutes, ChronoUnit.MINUTES));
        session.setRefreshExpiresAt(now.plus(refreshDays, ChronoUnit.DAYS));
        tokens.save(session);
        return new IssuedSession(access, refresh, accessMinutes * 60, refreshDays * 86400, user);
    }

    private BusinessException unauthorized(String message) {
        return new BusinessException("UNAUTHORIZED", message, HttpStatus.UNAUTHORIZED);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record IssuedSession(String accessToken, String refreshToken, long accessMaxAge,
                                long refreshMaxAge, User user) {}
}
