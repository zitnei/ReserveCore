package com.reservecore.domain.reservation;

import com.reservecore.domain.service.Service;
import com.reservecore.domain.store.Store;
import com.reservecore.domain.user.Role;
import com.reservecore.domain.user.User;
import com.reservecore.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 二重予約防止のEXCLUDE制約（V3）がDBレベルで効くことを検証する。
 */
class ReservationConstraintTest extends IntegrationTestSupport {

    private Store store;
    private Service service;
    private User customer;
    private User staff;

    @BeforeEach
    void setUpEntities() {
        store = storeRepository.save(Store.builder()
                .name("テスト店")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .build());

        service = serviceRepository.save(Service.builder()
                .store(store)
                .name("カット")
                .durationMinutes(60)
                .price(4000)
                .build());

        customer = userRepository.save(User.builder()
                .email("customer@example.com").password("x").name("客").role(Role.CUSTOMER).build());

        staff = userRepository.save(User.builder()
                .email("staff@example.com").password("x").name("スタッフ").role(Role.STAFF).build());
    }

    private Reservation reservation(LocalDateTime start, int minutes, ReservationStatus status) {
        return Reservation.builder()
                .store(store).service(service).customer(customer).staff(staff)
                .startTime(start).endTime(start.plusMinutes(minutes))
                .status(status)
                .build();
    }

    @Test
    @DisplayName("同一スタッフの時間帯が重なる予約はDB制約で弾かれる")
    void overlapping_isRejected() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 10, 10, 0);
        reservationRepository.saveAndFlush(reservation(base, 60, ReservationStatus.CONFIRMED)); // 10:00-11:00

        // 10:30-11:30 は重複 → 例外
        assertThatThrownBy(() ->
                reservationRepository.saveAndFlush(
                        reservation(base.plusMinutes(30), 60, ReservationStatus.CONFIRMED)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("連続する予約（11:00境界）は許可される")
    void backToBack_isAllowed() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 10, 10, 0);
        reservationRepository.saveAndFlush(reservation(base, 60, ReservationStatus.CONFIRMED));            // 10:00-11:00
        reservationRepository.saveAndFlush(reservation(base.plusMinutes(60), 60, ReservationStatus.CONFIRMED)); // 11:00-12:00

        assertThat(reservationRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("キャンセル済みの予約は枠を塞がない")
    void cancelled_doesNotBlock() {
        LocalDateTime base = LocalDateTime.of(2026, 6, 10, 10, 0);
        reservationRepository.saveAndFlush(reservation(base, 60, ReservationStatus.CANCELLED)); // 10:00-11:00 CANCELLED
        // 同じ時間帯でも CONFIRMED を入れられる
        reservationRepository.saveAndFlush(reservation(base, 60, ReservationStatus.CONFIRMED));

        assertThat(reservationRepository.count()).isEqualTo(2);
    }
}
