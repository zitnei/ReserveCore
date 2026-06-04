package com.reservecore.api.reservation;

import com.reservecore.domain.reservation.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 予約API
 * POST  /api/reservations            : 予約登録（認証済み・本人名義）
 * GET   /api/reservations            : 予約一覧（CUSTOMER=自分 / ADMIN・STAFF=全件）
 * PATCH /api/reservations/{id}/cancel: 予約キャンセル（本人 or ADMIN・STAFF）
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /** 予約登録 */
    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationCreateRequest request,
            Authentication authentication) {
        ReservationResponse response = reservationService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 予約一覧 */
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(reservationService.list(authentication.getName()));
    }

    /** 予約キャンセル */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(reservationService.cancel(id, authentication.getName()));
    }
}
