package com.reservecore.api.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservecore.api.auth.AuthResponse;
import com.reservecore.api.auth.LoginRequest;
import com.reservecore.api.auth.RegisterRequest;
import com.reservecore.domain.user.Role;
import com.reservecore.domain.user.User;
import com.reservecore.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 店舗API（登録/一覧/詳細）の権限・バリデーションの結合テスト。
 */
class StoreControllerTest extends IntegrationTestSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("ADMINは店舗を登録できる（201）")
    void createStore_asAdmin_returns201() throws Exception {
        String token = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(post("/api/stores")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "渋谷店",
                                  "address": "東京都渋谷区1-1-1",
                                  "phone": "03-1234-5678",
                                  "openingTime": "09:00",
                                  "closingTime": "18:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("渋谷店"))
                .andExpect(jsonPath("$.openingTime").value("09:00"));
    }

    @Test
    @DisplayName("CUSTOMERは店舗を登録できない（403）")
    void createStore_asCustomer_returns403() throws Exception {
        String token = registerCustomerAndGetToken("customer@example.com");

        mockMvc.perform(post("/api/stores")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "渋谷店",
                                  "openingTime": "09:00",
                                  "closingTime": "18:00"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("トークンなしの店舗登録は401")
    void createStore_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "渋谷店",
                                  "openingTime": "09:00",
                                  "closingTime": "18:00"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("開店時刻が閉店時刻以降だと400")
    void createStore_invalidBusinessHours_returns400() throws Exception {
        String token = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(post("/api/stores")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "深夜店",
                                  "openingTime": "18:00",
                                  "closingTime": "09:00"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("店舗名が空だとバリデーションで400")
    void createStore_blankName_returns400() throws Exception {
        String token = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(post("/api/stores")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "openingTime": "09:00",
                                  "closingTime": "18:00"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("認証済みユーザーは店舗一覧を取得できる（200・配列）")
    void getStores_authenticated_returns200() throws Exception {
        String token = registerCustomerAndGetToken("customer2@example.com");

        mockMvc.perform(get("/api/stores")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("存在しない店舗IDの詳細取得は404")
    void getStore_notFound_returns404() throws Exception {
        String token = registerCustomerAndGetToken("customer3@example.com");

        mockMvc.perform(get("/api/stores/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // --- ヘルパー ---

    private String registerCustomerAndGetToken(String email) throws Exception {
        RegisterRequest register = new RegisterRequest(email, "password123", "顧客");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());
        return loginAndGetToken(email, "password123");
    }

    private String createAdminAndGetToken(String email) throws Exception {
        User admin = User.builder()
                .email(email)
                .password(passwordEncoder.encode("admin1234"))
                .name("管理者")
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
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
