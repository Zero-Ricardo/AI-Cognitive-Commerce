package com.aishop.commerce.auth;

import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.domain.Enums;
import com.aishop.commerce.domain.User;
import com.aishop.commerce.security.AppUserPrincipal;
import com.aishop.commerce.security.SessionAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final boolean secureCookie;

    public AuthController(AuthService authService, @Value("${app.auth.secure-cookie}") boolean secureCookie) {
        this.authService = authService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.username(), request.phone(), request.email(), request.password());
        return ApiResponse.ok(UserView.from(user));
    }

    @PostMapping("/login")
    public ApiResponse<UserView> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var session = authService.login(request.account().trim(), request.password());
        setCookies(response, session);
        return ApiResponse.ok(UserView.from(session.user()));
    }

    @PostMapping("/refresh")
    public ApiResponse<UserView> refresh(HttpServletRequest request, HttpServletResponse response) {
        var session = authService.refresh(SessionAuthenticationFilter.cookie(request, "refresh_token"));
        setCookies(response, session);
        return ApiResponse.ok(UserView.from(session.user()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(SessionAuthenticationFilter.cookie(request, "access_token"));
        clearCookies(response);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ApiResponse.ok(new UserView(principal.id(), principal.username(), principal.nickname(), principal.roles()));
    }

    private void setCookies(HttpServletResponse response, AuthService.IssuedSession session) {
        response.addHeader("Set-Cookie", cookie("access_token", session.accessToken(), session.accessMaxAge()).toString());
        response.addHeader("Set-Cookie", cookie("refresh_token", session.refreshToken(), session.refreshMaxAge()).toString());
    }

    private void clearCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookie("access_token", "", 0).toString());
        response.addHeader("Set-Cookie", cookie("refresh_token", "", 0).toString());
    }

    private ResponseCookie cookie(String name, String value, long age) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secureCookie).sameSite("Lax")
                .path("/").maxAge(age).build();
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(@NotBlank String account, @NotBlank String password) {}
    public record UserView(Long id, String username, String nickname, Set<Enums.Role> roles) {
        static UserView from(User user) { return new UserView(user.getId(), user.getUsername(), user.getNickname(), user.getRoles()); }
    }
}
