package com.reservecore.api.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * 店舗登録リクエスト
 * 時刻は "HH:mm"（例: "09:00"）または "HH:mm:ss" のISO形式で受け取る。
 */
public record StoreCreateRequest(

        @NotBlank(message = "店舗名は必須です")
        @Size(max = 100, message = "店舗名は100文字以内で入力してください")
        String name,

        @Size(max = 255, message = "住所は255文字以内で入力してください")
        String address,

        @Size(max = 20, message = "電話番号は20文字以内で入力してください")
        String phone,

        @NotNull(message = "開店時刻は必須です")
        LocalTime openingTime,

        @NotNull(message = "閉店時刻は必須です")
        LocalTime closingTime
) {}
