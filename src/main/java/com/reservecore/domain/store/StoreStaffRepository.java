package com.reservecore.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {

    /** STAFF が指定店舗に所属しているか（自店のみ操作の権限判定に使用） */
    boolean existsByStore_IdAndUser_Id(Long storeId, Long userId);
}
