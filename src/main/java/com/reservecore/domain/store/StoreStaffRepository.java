package com.reservecore.domain.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {

    /** STAFF が指定店舗に所属しているか（自店のみ操作の権限判定に使用） */
    boolean existsByStore_IdAndUser_Id(Long storeId, Long userId);

    /** 指定店舗に所属するスタッフ割当の一覧 */
    List<StoreStaff> findByStore_Id(Long storeId);

    /** 指定店舗からスタッフ割当を解除し、削除件数を返す */
    long deleteByStore_IdAndUser_Id(Long storeId, Long userId);
}
