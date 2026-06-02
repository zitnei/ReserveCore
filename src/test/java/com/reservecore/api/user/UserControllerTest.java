package com.reservecore.api.user;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 権限制御（JWT認証 + ロール別アクセス）の結合テスト
 */
class UserControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("トークンなしで /api/users/me にアクセスすると 401")
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CUSTOMERトークンで /api/users/me は 200（自分の情報を取得）")
    void getMe_withCustomerToken_returns200() throws Exception {
        String token = registerCustomerAndGetToken("customer@example.com");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("CUSTOMERトークンでADMIN専用 /api/users にアクセスすると 403")
    void getAllUsers_withCustomerToken_returns403() throws Exception {
        String token = registerCustomerAndGetToken("customer2@example.com");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMINトークンで /api/users は 200（一覧取得）")
    void getAllUsers_withAdminToken_returns200() throws Exception {
        registerCustomerAndGetToken("customer3@example.com");
        String adminToken = createAdminAndGetToken("admin@example.com");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
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
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(body, AuthResponse.class).token();
    }
}
