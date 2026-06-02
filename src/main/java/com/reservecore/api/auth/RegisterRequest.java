package com.reservecore.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ユーザー登録リクエスト
 */
public record RegisterRequest(

        @NotBlank(message = "メールアドレスは必須です")
        @Email(message = "メールアドレスの形式が正しくありません")
        String email,

        @NotBlank(message = "パスワードは必須です")
        @Size(min = 8, message = "パスワードは8文字以上で入力してください")
        String password,

        @NotBlank(message = "名前は必須です")
        @Size(max = 50, message = "名前は50文字以内で入力してください")
        String name
) {}
