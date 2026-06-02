package com.reservecore.api.service;

import com.reservecore.domain.service.Service;

/**
 * サービス（メニュー）情報レスポンス
 */
public record ServiceResponse(
        Long id,
        Long storeId,
        String name,
        Integer durationMinutes,
        Integer price
) {
    public static ServiceResponse from(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getStore().getId(),
                service.getName(),
                service.getDurationMinutes(),
                service.getPrice()
        );
    }
}
