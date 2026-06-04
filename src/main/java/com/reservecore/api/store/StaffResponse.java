package com.reservecore.api.store;

import com.reservecore.domain.store.StoreStaff;

/**
 * 店舗スタッフ情報レスポンス
 */
public record StaffResponse(
        Long storeId,
        Long userId,
        String name,
        String email,
        String role
) {
    public static StaffResponse from(StoreStaff storeStaff) {
        return new StaffResponse(
                storeStaff.getStore().getId(),
                storeStaff.getUser().getId(),
                storeStaff.getUser().getName(),
                storeStaff.getUser().getEmail(),
                storeStaff.getUser().getRole().name()
        );
    }
}
