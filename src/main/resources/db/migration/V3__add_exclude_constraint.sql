-- ============================================================
-- V3: 二重予約防止（同一スタッフの時間帯重複をDBレベルで禁止）
--   - btree_gist: bigint(staff_id)とrangeをGiSTで併用するため必要
--   - tsrange(..., '[)'): 半開区間。11:00終了と11:00開始は重複扱いしない
--   - WHERE status IN (...): 枠を実際に塞ぐ状態だけ対象（CANCELLED等は除外）
-- ============================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
    ADD CONSTRAINT no_overlap_per_staff
    EXCLUDE USING gist (
        staff_id WITH =,
        tsrange(start_time, end_time, '[)') WITH &&
    )
    WHERE (status IN ('PENDING', 'CONFIRMED'));
