package com.reservecore.api.store;

import com.reservecore.domain.store.StoreStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 店舗スタッフ管理API（店舗にぶら下がる）
 * POST   /api/stores/{storeId}/staff          : スタッフ割当（ADMIN・CUSTOMERはSTAFFに昇格）
 * GET    /api/stores/{storeId}/staff          : スタッフ一覧（認証済みなら誰でも）
 * DELETE /api/stores/{storeId}/staff/{userId} : 割当解除（ADMIN）
 */
@RestController
@RequestMapping("/api/stores/{storeId}/staff")
@RequiredArgsConstructor
public class StoreStaffController {

    private final StoreStaffService storeStaffService;

    /** スタッフ割当（ADMIN専用） */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponse> assign(
            @PathVariable Long storeId,
            @Valid @RequestBody StaffAssignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeStaffService.assign(storeId, request));
    }

    /** スタッフ一覧 */
    @GetMapping
    public ResponseEntity<List<StaffResponse>> list(@PathVariable Long storeId) {
        return ResponseEntity.ok(storeStaffService.list(storeId));
    }

    /** 割当解除（ADMIN専用） */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> remove(@PathVariable Long storeId, @PathVariable Long userId) {
        storeStaffService.remove(storeId, userId);
        return ResponseEntity.noContent().build();
    }
}
