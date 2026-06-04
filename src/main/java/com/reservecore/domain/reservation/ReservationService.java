package com.reservecore.domain.reservation;

import com.reservecore.api.reservation.ReservationCreateRequest;
import com.reservecore.api.reservation.ReservationResponse;
import com.reservecore.common.exception.ConflictException;
import com.reservecore.common.exception.NotFoundException;
import com.reservecore.domain.service.Service;
import com.reservecore.domain.service.ServiceRepository;
import com.reservecore.domain.store.Store;
import com.reservecore.domain.store.StoreRepository;
import com.reservecore.domain.store.StoreStaffRepository;
import com.reservecore.domain.user.Role;
import com.reservecore.domain.user.User;
import com.reservecore.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 予約のビジネスロジック。
 *
 * 二重予約は「アプリ層の事前チェック（→ 409）」と「DBのEXCLUDE制約（同時実行の最終防壁）」の
 * 二段構えで防ぐ。
 *
 * 注意: このファイルはエンティティ {@link Service} を import しているため、
 * Spring の {@code @Service} アノテーションは完全修飾名で記述している（名前衝突の回避）。
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ReservationService {

    /** 枠を占有する（重複チェック対象の）ステータス */
    private static final Set<ReservationStatus> ACTIVE_STATUSES =
            Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final ServiceRepository serviceRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final UserRepository userRepository;

    /**
     * 予約を登録する。予約者は認証ユーザー自身。
     */
    @Transactional
    public ReservationResponse create(ReservationCreateRequest request, String requesterEmail) {
        User customer = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("ユーザーが見つかりません"));

        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("店舗が見つかりません: id=" + request.storeId()));

        Service service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("サービスが見つかりません: id=" + request.serviceId()));
        if (!service.getStore().getId().equals(store.getId())) {
            throw new IllegalArgumentException("指定のサービスはこの店舗のものではありません");
        }

        User staff = userRepository.findById(request.staffId())
                .orElseThrow(() -> new NotFoundException("スタッフが見つかりません: id=" + request.staffId()));
        if (!storeStaffRepository.existsByStore_IdAndUser_Id(store.getId(), staff.getId())) {
            throw new IllegalArgumentException("指定のスタッフはこの店舗に所属していません");
        }

        LocalDateTime startTime = request.startTime();
        LocalDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        validateWithinBusinessHours(store, startTime, endTime);

        // アプリ層の事前チェック（分かりやすい 409 を返す）
        if (reservationRepository.existsOverlap(staff.getId(), startTime, endTime, ACTIVE_STATUSES)) {
            throw new ConflictException("指定の時間帯はすでに予約が入っています");
        }

        Reservation reservation = Reservation.builder()
                .store(store)
                .service(service)
                .customer(customer)
                .staff(staff)
                .startTime(startTime)
                .endTime(endTime)
                .status(ReservationStatus.CONFIRMED)
                .note(request.note())
                .build();

        try {
            // EXCLUDE制約を即時に評価させるため flush する
            reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            // 事前チェックをすり抜けた同時実行の競合をここで吸収
            throw new ConflictException("指定の時間帯はすでに予約が入っています");
        }

        return ReservationResponse.from(reservation);
    }

    /**
     * 予約一覧を取得する。
     * CUSTOMER は自分の予約のみ、ADMIN/STAFF は全予約を参照できる。
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> list(String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("ユーザーが見つかりません"));

        List<Reservation> reservations = (user.getRole() == Role.CUSTOMER)
                ? reservationRepository.findByCustomer_IdOrderByStartTimeAsc(user.getId())
                : reservationRepository.findAllByOrderByStartTimeAsc();

        return reservations.stream().map(ReservationResponse::from).toList();
    }

    /**
     * 予約をキャンセルする。
     * キャンセルできるのは「予約本人」または「ADMIN/STAFF」のみ。
     */
    @Transactional
    public ReservationResponse cancel(Long reservationId, String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("ユーザーが見つかりません"));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("予約が見つかりません: id=" + reservationId));

        boolean isOwner = reservation.getCustomer().getId().equals(user.getId());
        boolean isStaffOrAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        if (!isOwner && !isStaffOrAdmin) {
            throw new AccessDeniedException("この予約をキャンセルする権限がありません");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new IllegalArgumentException("この予約はキャンセルできません");
        }

        reservation.changeStatus(ReservationStatus.CANCELLED);
        return ReservationResponse.from(reservation);
    }

    /** 予約時間が店舗の営業時間内（同日・開店〜閉店）に収まるか検証する。 */
    private void validateWithinBusinessHours(Store store, LocalDateTime start, LocalDateTime end) {
        boolean sameDay = start.toLocalDate().equals(end.toLocalDate());
        if (!sameDay
                || start.toLocalTime().isBefore(store.getOpeningTime())
                || end.toLocalTime().isAfter(store.getClosingTime())) {
            throw new IllegalArgumentException("予約時間が店舗の営業時間外です");
        }
    }
}
