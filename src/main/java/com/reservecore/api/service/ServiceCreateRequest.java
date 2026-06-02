package com.reservecore.api.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * サービス（メニュー）登録リクエスト
 */
public record ServiceCreateRequest(

        @NotBlank(message = "サービス名は必須です")
        @Size(max = 100, message = "サービス名は100文字以内で入力してください")
        String name,

        @NotNull(message = "所要時間は必須です")
        @Positive(message = "所要時間は1分以上で入力してください")
        Integer durationMinutes,

        @NotNull(message = "料金は必須です")
        @PositiveOrZero(message = "料金は0以上で入力してください")
        Integer price
) {}
