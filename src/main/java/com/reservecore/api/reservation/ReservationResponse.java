package com.reservecore.api.reservation;

import com.reservecore.domain.reservation.Reservation;

import java.time.LocalDateTime;

/**
 * 予約情報レスポンス
 */
public record ReservationResponse(
        Long id,
        Long storeId,
        Long serviceId,
        String serviceName,
        Long staffId,
        String staffName,
        Long customerId,
        String customerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String note
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getStore().getId(),
                r.getService().getId(),
                r.getService().getName(),
                r.getStaff().getId(),
                r.getStaff().getName(),
                r.getCustomer().getId(),
                r.getCustomer().getName(),
                r.getStartTime(),
                r.getEndTime(),
                r.getStatus().name(),
                r.getNote()
        );
    }
}
