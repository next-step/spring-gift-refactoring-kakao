package gift.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class MemberAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입에 성공하면 토큰을 반환한다")
    void register() throws Exception {
        var request = new MemberRequest("newuser@example.com", "password123");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("이미 등록된 이메일로 가입하면 409를 반환한다")
    void register_DuplicateEmail() throws Exception {
        var request = new MemberRequest("admin@example.com", "password123");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("로그인에 성공하면 토큰을 반환한다")
    void login() throws Exception {
        var request = new MemberRequest("admin@example.com", "admin1234");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인하면 400을 반환한다")
    void login_WrongPassword() throws Exception {
        var request = new MemberRequest("admin@example.com", "wrongpassword");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 400을 반환한다")
    void login_NotFound() throws Exception {
        var request = new MemberRequest("unknown@example.com", "password123");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("이메일이 비어있으면 회원가입 시 400을 반환한다")
    void register_InvalidRequest() throws Exception {
        var request = new MemberRequest("", "password123");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
