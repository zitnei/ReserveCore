package com.reservecore.api.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservecore.api.auth.AuthResponse;
import com.reservecore.api.auth.LoginRequest;
import com.reservecore.domain.store.Store;
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
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 店舗スタッフ管理API（割当/一覧/解除）の結合テスト。
 * CUSTOMER→STAFF 昇格と、昇格後に自店サービスを作れることまで検証する。
 */
class StoreStaffControllerTest extends IntegrationTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    private Store store;

    @BeforeEach
    void setUpStore() {
        store = storeRepository.save(Store.builder()
                .name("渋谷店")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .build());
    }

    @Test
    @DisplayName("ADMINはユーザーをスタッフに割当でき、CUSTOMERはSTAFFに昇格する（201）")
    void assign_asAdmin_returns201_andPromotesToStaff() throws Exception {
        String adminToken = createAdminAndGetToken("admin@example.com");
        User user = createUser("user@example.com", "password123", Role.CUSTOMER);

        mockMvc.perform(post("/api/stores/{storeId}/staff", store.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffAssignRequest(user.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.storeId").value(store.getId()))
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    @DisplayName("CUSTOMERはスタッフ割当できない（403）")
    void assign_asCustomer_returns403() throws Exception {
        createUser("customer@example.com", "password123", Role.CUSTOMER);
        String customerToken = loginAndGetToken("customer@example.com", "password123");
        User target = createUser("target@example.com", "password123", Role.CUSTOMER);

        mockMvc.perform(post("/api/stores/{storeId}/staff", store.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffAssignRequest(target.getId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("存在しないユーザーの割当は404")
    void assign_userNotFound_returns404() throws Exception {
        String adminToken = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(post("/api/stores/{storeId}/staff", store.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffAssignRequest(999999L))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("存在しない店舗への割当は404")
    void assign_storeNotFound_returns404() throws Exception {
        String adminToken = createAdminAndGetToken("admin@example.com");
        User user = createUser("user@example.com", "password123", Role.CUSTOMER);

        mockMvc.perform(post("/api/stores/{storeId}/staff", 999999)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffAssignRequest(user.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("同じユーザーの二重割当は409")
    void assign_duplicate_returns409() throws Exception {
        String adminToken = createAdminAndGetToken("admin@example.com");
        User user = createUser("user@example.com", "password123", Role.CUSTOMER);
        String body = objectMapper.writeValueAsString(new StaffAssignRequest(user.getId()));

        mockMvc.perform(post("/api/stores/{storeId}/staff", store.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/stores/{storeId}/staff", store.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("認証済みユーザーはスタッフ一覧を取得できる（200・配列）")
    void list_authenticated_returns200() throws Exception {
        createUser("customer@example.com", "password123", Role.CUSTOMER);
        String customerToken = loginAndGetToken("customer@example.com", "password123");

        mockMvc.perform(get("/api/stores/{storeId}/staff", store.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("ADMINは割当を解除できる（204）")
    void remove_asAdmin_returns204() throws Exception {
        String adminToken = createAdminAndGetToken("admin@example.com");
        User user = createUser("user@example.com", "password123", Role.CUSTOMER);
        mockMvc.perform(post("/api/stores/{storeId}/staff", store.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffAssignRequest(user.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/stores/{storeId}/staff/{userId}", store.getId(), user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("割当が無いユーザーの解除は404")
    void remove_notAssigned_returns404() throws Exception {
        String adminToken = createAdminAndGetToken("admin@example.com");
        User user = createUser("user@example.com", "password123", Role.CUSTOMER);

        mockMvc.perform(delete("/api/stores/{storeId}/staff/{userId}", store.getId(), user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("昇格したSTAFFは自店のサービスを登録できる（割当の効果を結合検証）")
    void assignedStaff_canCreateServiceForOwnStore() throws Exception {
        String adminToken = createAdminAndGetToken("admin@example.com");
        User user = createUser("newstaff@example.com", "staff1234", Role.CUSTOMER);

        // 割当 → STAFFに昇格
        mockMvc.perform(post("/api/stores/{storeId}/staff", store.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffAssignRequest(user.getId()))))
                .andExpect(status().isCreated());

        // 昇格後に再ログイン → STAFFトークンを取得
        String staffToken = loginAndGetToken("newstaff@example.com", "staff1234");

        // 自店のサービスを作成できる（ADMIN or 自店STAFF のみ可能なエンドポイント）
        mockMvc.perform(post("/api/stores/{storeId}/services", store.getId())
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "カット",
                                  "durationMinutes": 60,
                                  "price": 4000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeId").value(store.getId()));
    }

    // --- ヘルパー ---

    private User createUser(String email, String rawPassword, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name("テストユーザー")
                .role(role)
                .build());
    }

    private String createAdminAndGetToken(String email) throws Exception {
        createUser(email, "admin1234", Role.ADMIN);
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
