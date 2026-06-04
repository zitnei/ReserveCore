package com.reservecore.api.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservecore.api.auth.AuthResponse;
import com.reservecore.api.auth.LoginRequest;
import com.reservecore.api.auth.RegisterRequest;
import com.reservecore.domain.service.Service;
import com.reservecore.domain.store.Store;
import com.reservecore.domain.store.StoreStaff;
import com.reservecore.domain.user.Role;
import com.reservecore.domain.user.User;
import com.reservecore.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 予約API（登録/一覧/キャンセル）の結合テスト。
 * 二重予約防止(409)・営業時間チェック(400)・ロール別の見え方・キャンセル権限を検証する。
 */
class ReservationControllerTest extends IntegrationTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    private Store store;
    private Service service;
    private User staff;

    @BeforeEach
    void setUpFixtures() {
        store = storeRepository.save(Store.builder()
                .name("渋谷店")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .build());

        service = serviceRepository.save(Service.builder()
                .store(store)
                .name("カット")
                .durationMinutes(60)
                .price(4000)
                .build());

        staff = userRepository.save(User.builder()
                .email("staff@example.com")
                .password(passwordEncoder.encode("staff1234"))
                .name("スタッフ")
                .role(Role.STAFF)
                .build());

        // スタッフを店舗に所属させる（予約時の所属チェックを通すため）
        storeStaffRepository.save(StoreStaff.builder().store(store).user(staff).build());
    }

    @Test
    @DisplayName("CUSTOMERは予約を登録できる（201・CONFIRMED）")
    void create_asCustomer_returns201() throws Exception {
        String token = registerCustomerAndGetToken("customer@example.com");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(staff.getId(), futureAt(10, 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.staffId").value(staff.getId()));
    }

    @Test
    @DisplayName("トークンなしの予約登録は401")
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(staff.getId(), futureAt(10, 0))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("営業時間外（開店前）の予約は400")
    void create_outsideBusinessHours_returns400() throws Exception {
        String token = registerCustomerAndGetToken("customer@example.com");

        // 08:00開始（開店09:00より前）
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(staff.getId(), futureAt(8, 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("店舗に所属しないスタッフ指定は400")
    void create_staffNotInStore_returns400() throws Exception {
        String token = registerCustomerAndGetToken("customer@example.com");
        User otherStaff = userRepository.save(User.builder()
                .email("otherstaff@example.com")
                .password(passwordEncoder.encode("staff1234"))
                .name("他店スタッフ")
                .role(Role.STAFF)
                .build());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(otherStaff.getId(), futureAt(10, 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("同一スタッフの時間帯が重なる予約は409")
    void create_overlapping_returns409() throws Exception {
        String tokenA = registerCustomerAndGetToken("a@example.com");
        String tokenB = registerCustomerAndGetToken("b@example.com");

        // 10:00-11:00 を確保
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(staff.getId(), futureAt(10, 0))))
                .andExpect(status().isCreated());

        // 10:30-11:30 は重複 → 409
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(staff.getId(), futureAt(10, 30))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CUSTOMERは自分の予約だけが一覧に出る")
    void list_asCustomer_returnsOwnOnly() throws Exception {
        String tokenA = registerCustomerAndGetToken("a@example.com");
        String tokenB = registerCustomerAndGetToken("b@example.com");
        createReservation(tokenA, futureAt(10, 0)); // Aの予約
        createReservation(tokenB, futureAt(12, 0)); // Bの予約（重複しない）

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("ADMINは全予約を一覧できる")
    void list_asAdmin_returnsAll() throws Exception {
        String tokenA = registerCustomerAndGetToken("a@example.com");
        String tokenB = registerCustomerAndGetToken("b@example.com");
        createReservation(tokenA, futureAt(10, 0));
        createReservation(tokenB, futureAt(12, 0));
        String adminToken = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("予約本人はキャンセルできる（200・CANCELLED）")
    void cancel_byOwner_returns200() throws Exception {
        String token = registerCustomerAndGetToken("customer@example.com");
        long id = createReservation(token, futureAt(10, 0));

        mockMvc.perform(patch("/api/reservations/{id}/cancel", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("他人の予約はキャンセルできない（403）")
    void cancel_byOtherCustomer_returns403() throws Exception {
        String tokenA = registerCustomerAndGetToken("a@example.com");
        String tokenB = registerCustomerAndGetToken("b@example.com");
        long id = createReservation(tokenA, futureAt(10, 0));

        mockMvc.perform(patch("/api/reservations/{id}/cancel", id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("存在しない予約のキャンセルは404")
    void cancel_notFound_returns404() throws Exception {
        String token = registerCustomerAndGetToken("customer@example.com");

        mockMvc.perform(patch("/api/reservations/{id}/cancel", 999999)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // --- ヘルパー ---

    /** 7日後の指定時刻（必ず未来かつ同日内）を返す。 */
    private LocalDateTime futureAt(int hour, int minute) {
        return LocalDateTime.now().plusDays(7)
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    }

    private String reservationJson(Long staffId, LocalDateTime startTime) throws Exception {
        return objectMapper.writeValueAsString(
                new ReservationCreateRequest(store.getId(), service.getId(), staffId, startTime, null));
    }

    /** 予約を作成し、生成された予約IDを返す。 */
    private long createReservation(String token, LocalDateTime startTime) throws Exception {
        String body = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson(staff.getId(), startTime)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String registerCustomerAndGetToken(String email) throws Exception {
        RegisterRequest register = new RegisterRequest(email, "password123", "顧客");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());
        return loginAndGetToken(email, "password123");
    }

    private String createAdminAndGetToken(String email) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("admin1234"))
                .name("管理者")
                .role(Role.ADMIN)
                .build());
        return loginAndGetToken(email, "admin1234");
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest(email, password);
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(body, AuthResponse.class).token();
    }
}
