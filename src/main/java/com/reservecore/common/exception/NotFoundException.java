package com.reservecore.common.exception;

/**
 * リソースが見つからない場合の例外（→ 404 Not Found）
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
