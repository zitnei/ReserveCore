package com.reservecore.domain.user;

/**
 * ユーザーの権限ロール
 * ADMIN  : 全操作が可能（システム管理者）
 * STAFF  : 自店舗の予約・売上操作が可能
 * CUSTOMER: 自分の予約のみ操作可能
 */
public enum Role {
    ADMIN,
    STAFF,
    CUSTOMER
}
