package com.reservecore.api.store;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.reservecore.domain.store.Store;

import java.time.LocalTime;

/**
 * 店舗情報レスポンス
 * 時刻は "HH:mm"（例: "09:00"）の形式で返す。
 */
public record StoreResponse(
        Long id,
        String name,
        String address,
        String phone,
        @JsonFormat(pattern = "HH:mm") LocalTime openingTime,
        @JsonFormat(pattern = "HH:mm") LocalTime closingTime
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getAddress(),
                store.getPhone(),
                store.getOpeningTime(),
                store.getClosingTime()
        );
    }
}
