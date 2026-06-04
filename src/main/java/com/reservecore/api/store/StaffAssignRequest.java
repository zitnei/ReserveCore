package com.reservecore.api.store;

import jakarta.validation.constraints.NotNull;

/**
 * スタッフ割当リクエスト。
 * 既存ユーザーを店舗のスタッフとして割り当てる（CUSTOMER は STAFF に昇格）。
 */
public record StaffAssignRequest(

        @NotNull(message = "ユーザーIDは必須です")
        Long userId
) {}
