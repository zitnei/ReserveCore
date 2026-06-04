package com.reservecore.domain.store;

import com.reservecore.api.store.StaffAssignRequest;
import com.reservecore.api.store.StaffResponse;
import com.reservecore.common.exception.ConflictException;
import com.reservecore.common.exception.NotFoundException;
import com.reservecore.domain.user.Role;
import com.reservecore.domain.user.User;
import com.reservecore.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 店舗スタッフ（store_staff）の管理ロジック。
 * 割当・解除は ADMIN 限定（コントローラーの @PreAuthorize で制御）。
 */
@Service
@RequiredArgsConstructor
public class StoreStaffService {

    private final StoreStaffRepository storeStaffRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    /**
     * ユーザーを店舗のスタッフとして割り当てる。
     * CUSTOMER は STAFF に昇格する（ADMIN/STAFF はロール据え置き）。
     */
    @Transactional
    public StaffResponse assign(Long storeId, StaffAssignRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("店舗が見つかりません: id=" + storeId));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("ユーザーが見つかりません: id=" + request.userId()));

        if (storeStaffRepository.existsByStore_IdAndUser_Id(store.getId(), user.getId())) {
            throw new ConflictException("このユーザーはすでにこの店舗のスタッフです");
        }

        if (user.getRole() == Role.CUSTOMER) {
            user.changeRole(Role.STAFF);
        }

        StoreStaff saved = storeStaffRepository.save(
                StoreStaff.builder().store(store).user(user).build());

        return StaffResponse.from(saved);
    }

    /** 指定店舗のスタッフ一覧を取得する。店舗が存在しなければ 404。 */
    @Transactional(readOnly = true)
    public List<StaffResponse> list(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new NotFoundException("店舗が見つかりません: id=" + storeId);
        }
        return storeStaffRepository.findByStore_Id(storeId).stream()
                .map(StaffResponse::from)
                .toList();
    }

    /**
     * スタッフ割当を解除する。割当が無ければ 404。
     * （ロールは変更しない。他店のスタッフを兼任している場合があるため）
     */
    @Transactional
    public void remove(Long storeId, Long userId) {
        long deleted = storeStaffRepository.deleteByStore_IdAndUser_Id(storeId, userId);
        if (deleted == 0) {
            throw new NotFoundException(
                    "スタッフ割当が見つかりません: storeId=" + storeId + ", userId=" + userId);
        }
    }
}
