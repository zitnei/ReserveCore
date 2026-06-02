package com.reservecore.domain.reservation;

/**
 * 予約ステータス
 * PENDING   : 仮予約
 * CONFIRMED : 確定
 * CANCELLED : キャンセル
 * COMPLETED : 完了
 * （ARRIVED / NO_SHOW は Phase 2）
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
