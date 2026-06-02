package com.reservecore.api.auth;

/**
 * 認証レスポンス（JWT トークンを返す）
 */
public record AuthResponse(
        String token,
        String email,
        String name,
        String role
) {}
