package com.reservecore.domain.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    /** 指定店舗に属するサービス一覧を取得する。 */
    List<Service> findByStore_Id(Long storeId);
}
