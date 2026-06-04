package com.reservecore.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 同一スタッフで時間帯が重なる「有効な」予約が存在するか（アプリ層の事前チェック）。
     * 区間 [start, end) 同士は (start1 < end2) かつ (start2 < end1) のとき重なる。
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.staff.id = :staffId
              AND r.status IN :statuses
              AND r.startTime < :endTime
              AND r.endTime > :startTime
            """)
    boolean existsOverlap(@Param("staffId") Long staffId,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime,
                          @Param("statuses") Collection<ReservationStatus> statuses);

    /** 顧客自身の予約一覧（開始時刻の昇順） */
    List<Reservation> findByCustomer_IdOrderByStartTimeAsc(Long customerId);

    /** 全予約一覧（ADMIN/STAFF用・開始時刻の昇順） */
    List<Reservation> findAllByOrderByStartTimeAsc();
}
