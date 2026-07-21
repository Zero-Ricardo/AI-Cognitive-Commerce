package com.aishop.commerce.user;

import com.aishop.commerce.common.ApiResponse;
import com.aishop.commerce.common.BusinessException;
import com.aishop.commerce.repository.UserRepository;
import com.aishop.commerce.security.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository users;
    public UserController(UserRepository users) { this.users = users; }

    @PatchMapping("/me")
    @Transactional
    public ApiResponse<ProfileView> update(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @Valid @RequestBody ProfileRequest request) {
        var user = users.findById(principal.id()).orElseThrow(() -> BusinessException.notFound("用户不存在"));
        user.setNickname(request.nickname().trim());
        return ApiResponse.ok(new ProfileView(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(), user.getEmail()));
    }

    public record ProfileRequest(@NotBlank @Size(max = 64) String nickname) {}
    public record ProfileView(Long id, String username, String nickname, String phone, String email) {}
}
