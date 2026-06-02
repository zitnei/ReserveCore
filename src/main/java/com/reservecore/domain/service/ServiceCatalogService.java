package com.reservecore.domain.service;

import com.reservecore.api.service.ServiceCreateRequest;
import com.reservecore.api.service.ServiceResponse;
import com.reservecore.common.exception.NotFoundException;
import com.reservecore.domain.store.Store;
import com.reservecore.domain.store.StoreRepository;
import com.reservecore.domain.store.StoreStaffRepository;
import com.reservecore.domain.user.Role;
import com.reservecore.domain.user.User;
import com.reservecore.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * サービス（メニュー）カタログのビジネスロジック。
 * 登録は ADMIN または「その店舗に所属する STAFF」のみ許可する。
 *
 * 注意:
 * このパッケージにはエンティティ {@code Service} があるため、Spring の
 * {@code @Service} アノテーションは完全修飾名で記述している（名前衝突の回避）。
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final UserRepository userRepository;

    /**
     * サービスを登録する。
     * - 店舗が存在しなければ 404
     * - ADMIN もしくは自店所属の STAFF 以外は 403
     */
    @Transactional
    public ServiceResponse createService(Long storeId, ServiceCreateRequest request, String requesterEmail) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("店舗が見つかりません: id=" + storeId));

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new NotFoundException("ユーザーが見つかりません"));

        authorizeStoreModification(store.getId(), requester);

        Service service = Service.builder()
                .store(store)
                .name(request.name())
                .durationMinutes(request.durationMinutes())
                .price(request.price())
                .build();

        return ServiceResponse.from(serviceRepository.save(service));
    }

    /** 指定店舗のサービス一覧を取得する。店舗が存在しなければ 404。 */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByStore(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new NotFoundException("店舗が見つかりません: id=" + storeId);
        }
        return serviceRepository.findByStore_Id(storeId).stream()
                .map(ServiceResponse::from)
                .toList();
    }

    /**
     * 店舗への変更権限を判定する。
     * - ADMIN          : 全店舗OK
     * - 自店所属のSTAFF : OK
     * - それ以外        : AccessDeniedException（→ 403）
     */
    private void authorizeStoreModification(Long storeId, User requester) {
        if (requester.getRole() == Role.ADMIN) {
            return;
        }
        if (requester.getRole() == Role.STAFF
                && storeStaffRepository.existsByStore_IdAndUser_Id(storeId, requester.getId())) {
            return;
        }
        throw new AccessDeniedException("この店舗を操作する権限がありません");
    }
}
