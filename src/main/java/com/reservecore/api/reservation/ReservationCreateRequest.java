package com.reservecore.api.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 予約登録リクエスト。
 * 終了時刻はサービスの所要時間から算出するため受け取らない。
 * 予約者（customer）は認証情報から特定する。
 */
public record ReservationCreateRequest(

        @NotNull(message = "店舗IDは必須です")
        Long storeId,

        @NotNull(message = "サービスIDは必須です")
        Long serviceId,

        @NotNull(message = "担当スタッフIDは必須です")
        Long staffId,

        @NotNull(message = "予約開始日時は必須です")
        @Future(message = "予約開始日時は未来の日時を指定してください")
        LocalDateTime startTime,

        @Size(max = 500, message = "備考は500文字以内で入力してください")
        String note
) {}
