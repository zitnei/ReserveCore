package com.reservecore.config;

import com.reservecore.domain.service.Service;
import com.reservecore.domain.service.ServiceRepository;
import com.reservecore.domain.store.Store;
import com.reservecore.domain.store.StoreRepository;
import com.reservecore.domain.store.StoreStaff;
import com.reservecore.domain.store.StoreStaffRepository;
import com.reservecore.domain.user.Role;
import com.reservecore.domain.user.User;
import com.reservecore.domain.user.UserRepository;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * アプリ起動時に初期データを投入する。
 *
 * <ol>
 *   <li>初期 ADMIN ユーザー（登録 API からは作れないためここで seed）</li>
 *   <li>デモ用の店舗・サービス・スタッフ・サンプル顧客
 *       （ログイン直後に予約フローを体験できるようにするため）</li>
 * </ol>
 *
 * いずれも「既に存在すれば作らない」冪等な設計。Free DB が 90 日で失効しても、
 * 新しい DB で再起動すれば自動的にデモデータが復元される。
 * 認証情報・seed の ON/OFF は application.yml / 環境変数から制御する。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ServiceRepository serviceRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    /** デモデータ投入の ON/OFF（本番運用に切り替える際は false にできる）。 */
    @Value("${app.demo-data.enabled:true}")
    private boolean demoDataEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        if (demoDataEnabled) {
            seedDemoData();
        }
    }

    /** 初期 ADMIN を作成する（存在しなければ）。 */
    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .name(adminName)
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("初期ADMINユーザーを作成しました: {}", adminEmail);
    }

    /**
     * デモ用の店舗・サービス・スタッフ・サンプル顧客を投入する。
     * 既にデモ店舗が存在する場合は何もしない（冪等）。
     */
    private void seedDemoData() {
        final String demoStoreName = "ReserveCore デモ店（渋谷）";

        // 既にデモ店舗があれば二重投入しない
        boolean alreadySeeded = storeRepository.findAll().stream()
                .anyMatch(s -> demoStoreName.equals(s.getName()));
        if (alreadySeeded) {
            return;
        }

        // 1) 店舗
        Store store = storeRepository.save(Store.builder()
                .name(demoStoreName)
                .address("東京都渋谷区道玄坂1-2-3 ReserveCoreビル 4F")
                .phone("03-1234-5678")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(20, 0))
                .build());

        // 2) サービス（メニュー）
        serviceRepository.saveAll(List.of(
                Service.builder().store(store).name("カット").durationMinutes(60).price(4500).build(),
                Service.builder().store(store).name("カラー").durationMinutes(90).price(7000).build(),
                Service.builder().store(store).name("パーマ").durationMinutes(120).price(9000).build(),
                Service.builder().store(store).name("ヘッドスパ").durationMinutes(45).price(3500).build()
        ));

        // 3) スタッフ（STAFF ロールのユーザー + 店舗への割当）
        User staff1 = createUserIfAbsent("staff.sato@reservecore.com", "佐藤 花子", Role.STAFF);
        User staff2 = createUserIfAbsent("staff.suzuki@reservecore.com", "鈴木 太郎", Role.STAFF);
        assignStaff(store, staff1);
        assignStaff(store, staff2);

        // 4) サンプル顧客（デモ用の「既存顧客」としてログインできるよう用意）
        createUserIfAbsent("customer.tanaka@reservecore.com", "田中 美咲", Role.CUSTOMER);

        log.info("デモデータを投入しました（店舗1・サービス4・スタッフ2・顧客1）");
    }

    /** email が未登録ならユーザーを作成して返す。既存ならそれを返す。 */
    private User createUserIfAbsent(String email, String name, Role role) {
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        // デモアカウントの共通パスワード（README に明記）
                        .password(passwordEncoder.encode("password123"))
                        .name(name)
                        .role(role)
                        .build()));
    }

    /** 店舗にスタッフを割り当てる（重複時はスキップ）。 */
    private void assignStaff(Store store, User user) {
        if (storeStaffRepository.existsByStore_IdAndUser_Id(store.getId(), user.getId())) {
            return;
        }
        storeStaffRepository.save(StoreStaff.builder()
                .store(store)
                .user(user)
                .build());
    }
}
