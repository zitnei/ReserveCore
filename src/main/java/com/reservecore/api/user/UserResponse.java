package com.reservecore.api.user;

import com.reservecore.domain.user.User;

/**
 * ユーザー情報レスポンス
 * パスワードなどの機密情報は含めない
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }
}
