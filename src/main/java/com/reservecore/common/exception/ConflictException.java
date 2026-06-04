package com.reservecore.common.exception;

/**
 * リソースの競合（→ 409 Conflict）。
 * 例: 同一スタッフの時間帯が重複する二重予約。
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
