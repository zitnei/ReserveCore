package com.reservecore.support;

import com.reservecore.domain.reservation.ReservationRepository;
import com.reservecore.domain.service.ServiceRepository;
import com.reservecore.domain.store.StoreRepository;
import com.reservecore.domain.store.StoreStaffRepository;
import com.reservecore.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 結合テスト共通の基底クラス。
 *
 * 全テストが同一の実DB（PostgreSQL）を共有するため、各テスト実行前に
 * 外部キー整合性を保つ順序で全テーブルを初期化する。
 * （reservations → services / store_staff → stores → users）
 *
 * これにより、テストクラスの実行順に依存した FK 制約違反を防ぐ。
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    @Autowired protected ReservationRepository reservationRepository;
    @Autowired protected ServiceRepository serviceRepository;
    @Autowired protected StoreStaffRepository storeStaffRepository;
    @Autowired protected StoreRepository storeRepository;
    @Autowired protected UserRepository userRepository;

    @BeforeEach
    protected void cleanDatabase() {
        reservationRepository.deleteAll();
        serviceRepository.deleteAll();
        storeStaffRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();
    }
}
