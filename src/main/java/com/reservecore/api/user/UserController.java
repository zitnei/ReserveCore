package com.reservecore.api.user;

import com.reservecore.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ユーザーAPI
 * GET /api/users/me : 認証済みユーザー自身の情報（全ロール可）
 * GET /api/users    : 全ユーザー一覧（ADMINのみ）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 自分の情報を取得（JWT認証が効いているかの確認用） */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(Authentication authentication) {
        // authentication.getName() は JWT の subject = email
        return ResponseEntity.ok(userService.getMyInfo(authentication.getName()));
    }

    /** 全ユーザー一覧（ADMIN専用：権限制御のデモ） */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
