package com.reservecore.domain.store;

import com.reservecore.api.store.StoreCreateRequest;
import com.reservecore.api.store.StoreResponse;
import com.reservecore.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 店舗のビジネスロジック。
 * 登録は ADMIN のみ（呼び出し側のコントローラーで @PreAuthorize により制御）。
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    /**
     * 店舗を登録する。
     * 営業時間の整合性（開店 < 閉店）を最低限チェックする。
     * ※ 日跨ぎ営業・定休日などは Phase 2 で対応予定。
     */
    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        if (!request.openingTime().isBefore(request.closingTime())) {
            throw new IllegalArgumentException("開店時刻は閉店時刻より前である必要があります");
        }

        Store store = Store.builder()
                .name(request.name())
                .address(request.address())
                .phone(request.phone())
                .openingTime(request.openingTime())
                .closingTime(request.closingTime())
                .build();

        return StoreResponse.from(storeRepository.save(store));
    }

    /** 店舗一覧を取得する。 */
    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores() {
        return storeRepository.findAll().stream()
                .map(StoreResponse::from)
                .toList();
    }

    /** 店舗詳細を取得する。存在しなければ 404。 */
    @Transactional(readOnly = true)
    public StoreResponse getStore(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("店舗が見つかりません: id=" + id));
        return StoreResponse.from(store);
    }
}
