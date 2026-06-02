package com.reservecore.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservecore.api.auth.AuthResponse;
import com.reservecore.api.auth.LoginRequest;
import com.reservecore.api.auth.RegisterRequest;
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
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * サービスAPI（登録/一覧）の権限制御の結合テスト。
 * 登録は「ADMIN」または「その店舗に所属する STAFF」のみ許可される。
 */
class ServiceControllerTest extends IntegrationTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    private Store store;

    private static final String BODY = """
            {
              "name": "カット",
              "durationMinutes": 60,
              "price": 4000
            }
            """;

    @BeforeEach
    void setUpStore() {
        store = storeRepository.save(Store.builder()
                .name("渋谷店")
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(18, 0))
                .build());
    }

    @Test
    @DisplayName("ADMINはサービスを登録できる（201）")
    void createService_asAdmin_returns201() throws Exception {
        String token = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(post("/api/stores/{storeId}/services", store.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.storeId").value(store.getId()))
                .andExpect(jsonPath("$.name").value("カット"));
    }

    @Test
    @DisplayName("自店所属のSTAFFはサービスを登録できる（201）")
    void createService_asOwnStoreStaff_returns201() throws Exception {
        User staff = createStaff("staff@example.com");
        storeStaffRepository.save(StoreStaff.builder().store(store).user(staff).build());
        String token = loginAndGetToken("staff@example.com", "staff1234");

        mockMvc.perform(post("/api/stores/{storeId}/services", store.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeId").value(store.getId()));
    }

    @Test
    @DisplayName("他店のSTAFFは登録できない（403）")
    void createService_asOtherStoreStaff_returns403() throws Exception {
        // store には紐付けない STAFF
        createStaff("otherstaff@example.com");
        String token = loginAndGetToken("otherstaff@example.com", "staff1234");

        mockMvc.perform(post("/api/stores/{storeId}/services", store.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CUSTOMERは登録できない（403）")
    void createService_asCustomer_returns403() throws Exception {
        String token = registerCustomerAndGetToken("customer@example.com");

        mockMvc.perform(post("/api/stores/{storeId}/services", store.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("存在しない店舗へのサービス登録は404")
    void createService_storeNotFound_returns404() throws Exception {
        String token = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(post("/api/stores/{storeId}/services", 999999)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("所要時間が0以下だとバリデーションで400")
    void createService_invalidDuration_returns400() throws Exception {
        String token = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(post("/api/stores/{storeId}/services", store.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "カット",
                                  "durationMinutes": 0,
                                  "price": 4000
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("認証済みユーザーはサービス一覧を取得できる（200・配列）")
    void getServices_authenticated_returns200() throws Exception {
        String token = registerCustomerAndGetToken("customer2@example.com");

        mockMvc.perform(get("/api/stores/{storeId}/services", store.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("存在しない店舗のサービス一覧は404")
    void getServices_storeNotFound_returns404() throws Exception {
        String token = registerCustomerAndGetToken("customer3@example.com");

        mockMvc.perform(get("/api/stores/{storeId}/services", 999999)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // --- ヘルパー ---

    private User createStaff(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("staff1234"))
                .name("スタッフ")
                .role(Role.STAFF)
                .build());
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
